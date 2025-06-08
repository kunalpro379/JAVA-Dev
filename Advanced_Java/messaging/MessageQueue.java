package messaging;

import java.util.concurrent.*;
import java.util.*;
import java.time.*;
import java.util.function.*;
import java.nio.ByteBuffer;
import java.util.stream.*;

public class MessageQueue<T> {
    private final Map<String, PriorityBlockingQueue<Message<T>>> queues;
    private final Map<String, MessageHandler<T>> handlers;
    private final Map<String, DeadLetterQueue<T>> deadLetterQueues;
    private final MessagePersistence<T> persistence;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;
    private final MessageMetrics metrics;
    private final RetryPolicy retryPolicy;
    private volatile boolean isRunning;

    public MessageQueue(QueueConfig<T> config) {
        this.queues = new ConcurrentHashMap<>();
        this.handlers = new ConcurrentHashMap<>();
        this.deadLetterQueues = new ConcurrentHashMap<>();
        this.persistence = config.persistence;
        this.executor = Executors.newFixedThreadPool(config.workerThreads);
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.metrics = new MessageMetrics();
        this.retryPolicy = config.retryPolicy;
        this.isRunning = true;

        // Initialize default queue
        createQueue("default", 100);

        // Start message processing
        startMessageProcessing();
        startDeadLetterProcessing();
    }

    public void createQueue(String queueName, int capacity) {
        queues.computeIfAbsent(queueName, k -> 
            new PriorityBlockingQueue<>(capacity, 
                Comparator.comparingInt(Message::getPriority).reversed())
        );
        deadLetterQueues.computeIfAbsent(queueName, 
            k -> new DeadLetterQueue<>(capacity));
    }

    public void registerHandler(String queueName, MessageHandler<T> handler) {
        handlers.put(queueName, handler);
    }

    public CompletableFuture<Void> send(String queueName, T payload) {
        return send(queueName, payload, 0);
    }

    public CompletableFuture<Void> send(String queueName, T payload, int priority) {
        PriorityBlockingQueue<Message<T>> queue = queues.get(queueName);
        if (queue == null) {
            throw new IllegalArgumentException("Queue not found: " + queueName);
        }

        Message<T> message = new Message<>(
            UUID.randomUUID().toString(),
            payload,
            priority,
            Instant.now()
        );

        return CompletableFuture.runAsync(() -> {
            try {
                // Persist message before queuing
                persistence.persist(queueName, message);
                
                queue.offer(message);
                metrics.recordMessageSent(queueName);
                
            } catch (Exception e) {
                metrics.recordMessageFailed(queueName);
                throw new CompletionException(e);
            }
        }, executor);
    }

    private void startMessageProcessing() {
        queues.forEach((queueName, queue) -> {
            CompletableFuture.runAsync(() -> {
                while (isRunning) {
                    try {
                        Message<T> message = queue.poll(100, TimeUnit.MILLISECONDS);
                        if (message != null) {
                            processMessage(queueName, message);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, executor);
        });
    }

    private void processMessage(String queueName, Message<T> message) {
        MessageHandler<T> handler = handlers.get(queueName);
        if (handler == null) {
            moveToDeadLetter(queueName, message, 
                new IllegalStateException("No handler registered"));
            return;
        }

        try {
            handler.handle(message.getPayload());
            metrics.recordMessageProcessed(queueName);
            persistence.delete(queueName, message.getId());
            
        } catch (Exception e) {
            metrics.recordMessageFailed(queueName);
            
            if (message.getRetryCount() < retryPolicy.getMaxRetries()) {
                scheduleRetry(queueName, message);
            } else {
                moveToDeadLetter(queueName, message, e);
            }
        }
    }

    private void scheduleRetry(String queueName, Message<T> message) {
        message.incrementRetryCount();
        long delay = retryPolicy.getDelayMillis(message.getRetryCount());
        
        scheduler.schedule(() -> {
            PriorityBlockingQueue<Message<T>> queue = queues.get(queueName);
            if (queue != null) {
                queue.offer(message);
                metrics.recordMessageRetried(queueName);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void moveToDeadLetter(String queueName, 
                                Message<T> message, 
                                Exception error) {
        DeadLetterQueue<T> dlq = deadLetterQueues.get(queueName);
        if (dlq != null) {
            dlq.add(new DeadLetter<>(message, error, Instant.now()));
            metrics.recordMessageDeadLettered(queueName);
        }
    }

    private void startDeadLetterProcessing() {
        scheduler.scheduleAtFixedRate(() -> {
            deadLetterQueues.forEach((queueName, dlq) -> {
                List<DeadLetter<T>> expired = dlq.removeExpired(
                    Duration.ofHours(24)
                );
                expired.forEach(deadLetter -> 
                    persistence.delete(queueName, deadLetter.getMessage().getId())
                );
            });
        }, 1, 1, TimeUnit.HOURS);
    }

    public List<DeadLetter<T>> getDeadLetters(String queueName) {
        DeadLetterQueue<T> dlq = deadLetterQueues.get(queueName);
        return dlq != null ? dlq.getAll() : Collections.emptyList();
    }

    public CompletableFuture<Void> retryDeadLetter(String queueName, 
                                                  String messageId) {
        DeadLetterQueue<T> dlq = deadLetterQueues.get(queueName);
        if (dlq == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            Optional<DeadLetter<T>> deadLetter = dlq.remove(messageId);
            deadLetter.ifPresent(dl -> {
                Message<T> message = dl.getMessage();
                message.resetRetryCount();
                PriorityBlockingQueue<Message<T>> queue = queues.get(queueName);
                if (queue != null) {
                    queue.offer(message);
                    metrics.recordDeadLetterRetried(queueName);
                }
            });
        }, executor);
    }

    public QueueMetrics getMetrics() {
        return metrics.snapshot();
    }

    public void shutdown() {
        isRunning = false;
        executor.shutdown();
        scheduler.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Message definition
    private static class Message<T> {
        private final String id;
        private final T payload;
        private final int priority;
        private final Instant timestamp;
        private int retryCount;

        public Message(String id, T payload, int priority, Instant timestamp) {
            this.id = id;
            this.payload = payload;
            this.priority = priority;
            this.timestamp = timestamp;
            this.retryCount = 0;
        }

        public String getId() { return id; }
        public T getPayload() { return payload; }
        public int getPriority() { return priority; }
        public Instant getTimestamp() { return timestamp; }
        public int getRetryCount() { return retryCount; }
        
        public void incrementRetryCount() { retryCount++; }
        public void resetRetryCount() { retryCount = 0; }
    }

    // Dead letter definition
    private static class DeadLetter<T> {
        private final Message<T> message;
        private final Exception error;
        private final Instant deadLetterTimestamp;

        public DeadLetter(Message<T> message, 
                         Exception error, 
                         Instant deadLetterTimestamp) {
            this.message = message;
            this.error = error;
            this.deadLetterTimestamp = deadLetterTimestamp;
        }

        public Message<T> getMessage() { return message; }
        public Exception getError() { return error; }
        public Instant getDeadLetterTimestamp() { return deadLetterTimestamp; }
    }

    // Dead letter queue
    private static class DeadLetterQueue<T> {
        private final Map<String, DeadLetter<T>> deadLetters;
        private final int capacity;

        public DeadLetterQueue(int capacity) {
            this.deadLetters = new ConcurrentHashMap<>();
            this.capacity = capacity;
        }

        public void add(DeadLetter<T> deadLetter) {
            if (deadLetters.size() >= capacity) {
                // Remove oldest entry if capacity is reached
                String oldestId = deadLetters.entrySet().stream()
                    .min(Comparator.comparing(
                        e -> e.getValue().getDeadLetterTimestamp()))
                    .map(Map.Entry::getKey)
                    .orElse(null);
                if (oldestId != null) {
                    deadLetters.remove(oldestId);
                }
            }
            deadLetters.put(deadLetter.getMessage().getId(), deadLetter);
        }

        public Optional<DeadLetter<T>> remove(String messageId) {
            return Optional.ofNullable(deadLetters.remove(messageId));
        }

        public List<DeadLetter<T>> removeExpired(Duration ttl) {
            Instant threshold = Instant.now().minus(ttl);
            return deadLetters.values().stream()
                .filter(dl -> dl.getDeadLetterTimestamp().isBefore(threshold))
                .peek(dl -> deadLetters.remove(dl.getMessage().getId()))
                .collect(Collectors.toList());
        }

        public List<DeadLetter<T>> getAll() {
            return new ArrayList<>(deadLetters.values());
        }
    }

    // Message handler interface
    @FunctionalInterface
    public interface MessageHandler<T> {
        void handle(T message) throws Exception;
    }

    // Message persistence interface
    public interface MessagePersistence<T> {
        void persist(String queueName, Message<T> message) throws Exception;
        void delete(String queueName, String messageId) throws Exception;
    }

    // Retry policy
    public static class RetryPolicy {
        private final int maxRetries;
        private final long initialDelayMillis;
        private final double backoffMultiplier;

        public RetryPolicy(int maxRetries, 
                         long initialDelayMillis, 
                         double backoffMultiplier) {
            this.maxRetries = maxRetries;
            this.initialDelayMillis = initialDelayMillis;
            this.backoffMultiplier = backoffMultiplier;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public long getDelayMillis(int attempt) {
            return (long) (initialDelayMillis * 
                Math.pow(backoffMultiplier, attempt - 1));
        }
    }

    // Queue configuration
    public static class QueueConfig<T> {
        private final int workerThreads;
        private final MessagePersistence<T> persistence;
        private final RetryPolicy retryPolicy;

        private QueueConfig(Builder<T> builder) {
            this.workerThreads = builder.workerThreads;
            this.persistence = builder.persistence;
            this.retryPolicy = builder.retryPolicy;
        }

        public static class Builder<T> {
            private int workerThreads = Runtime.getRuntime().availableProcessors();
            private MessagePersistence<T> persistence = 
                new NoOpMessagePersistence<>();
            private RetryPolicy retryPolicy = 
                new RetryPolicy(3, 1000, 2.0);

            public Builder<T> workerThreads(int threads) {
                this.workerThreads = threads;
                return this;
            }

            public Builder<T> persistence(MessagePersistence<T> persistence) {
                this.persistence = persistence;
                return this;
            }

            public Builder<T> retryPolicy(RetryPolicy policy) {
                this.retryPolicy = policy;
                return this;
            }

            public QueueConfig<T> build() {
                return new QueueConfig<>(this);
            }
        }
    }

    // No-op persistence implementation
    private static class NoOpMessagePersistence<T> 
            implements MessagePersistence<T> {
        @Override
        public void persist(String queueName, Message<T> message) {}

        @Override
        public void delete(String queueName, String messageId) {}
    }

    // Metrics collection
    private static class MessageMetrics {
        private final Map<String, AtomicLong> sentMessages;
        private final Map<String, AtomicLong> processedMessages;
        private final Map<String, AtomicLong> failedMessages;
        private final Map<String, AtomicLong> retriedMessages;
        private final Map<String, AtomicLong> deadLetteredMessages;
        private final Map<String, AtomicLong> deadLetterRetries;

        public MessageMetrics() {
            this.sentMessages = new ConcurrentHashMap<>();
            this.processedMessages = new ConcurrentHashMap<>();
            this.failedMessages = new ConcurrentHashMap<>();
            this.retriedMessages = new ConcurrentHashMap<>();
            this.deadLetteredMessages = new ConcurrentHashMap<>();
            this.deadLetterRetries = new ConcurrentHashMap<>();
        }

        public void recordMessageSent(String queueName) {
            sentMessages.computeIfAbsent(queueName, k -> new AtomicLong())
                      .incrementAndGet();
        }

        public void recordMessageProcessed(String queueName) {
            processedMessages.computeIfAbsent(queueName, k -> new AtomicLong())
                          .incrementAndGet();
        }

        public void recordMessageFailed(String queueName) {
            failedMessages.computeIfAbsent(queueName, k -> new AtomicLong())
                        .incrementAndGet();
        }

        public void recordMessageRetried(String queueName) {
            retriedMessages.computeIfAbsent(queueName, k -> new AtomicLong())
                         .incrementAndGet();
        }

        public void recordMessageDeadLettered(String queueName) {
            deadLetteredMessages.computeIfAbsent(queueName, k -> new AtomicLong())
                              .incrementAndGet();
        }

        public void recordDeadLetterRetried(String queueName) {
            deadLetterRetries.computeIfAbsent(queueName, k -> new AtomicLong())
                           .incrementAndGet();
        }

        public QueueMetrics snapshot() {
            return new QueueMetrics(
                new HashMap<>(mapToLong(sentMessages)),
                new HashMap<>(mapToLong(processedMessages)),
                new HashMap<>(mapToLong(failedMessages)),
                new HashMap<>(mapToLong(retriedMessages)),
                new HashMap<>(mapToLong(deadLetteredMessages)),
                new HashMap<>(mapToLong(deadLetterRetries))
            );
        }

        private Map<String, Long> mapToLong(Map<String, AtomicLong> map) {
            return map.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().get()
                ));
        }
    }

    // Metrics snapshot
    public static class QueueMetrics {
        private final Map<String, Long> sentMessages;
        private final Map<String, Long> processedMessages;
        private final Map<String, Long> failedMessages;
        private final Map<String, Long> retriedMessages;
        private final Map<String, Long> deadLetteredMessages;
        private final Map<String, Long> deadLetterRetries;

        public QueueMetrics(
            Map<String, Long> sentMessages,
            Map<String, Long> processedMessages,
            Map<String, Long> failedMessages,
            Map<String, Long> retriedMessages,
            Map<String, Long> deadLetteredMessages,
            Map<String, Long> deadLetterRetries
        ) {
            this.sentMessages = sentMessages;
            this.processedMessages = processedMessages;
            this.failedMessages = failedMessages;
            this.retriedMessages = retriedMessages;
            this.deadLetteredMessages = deadLetteredMessages;
            this.deadLetterRetries = deadLetterRetries;
        }

        public Map<String, Long> getSentMessages() {
            return Collections.unmodifiableMap(sentMessages);
        }

        public Map<String, Long> getProcessedMessages() {
            return Collections.unmodifiableMap(processedMessages);
        }

        public Map<String, Long> getFailedMessages() {
            return Collections.unmodifiableMap(failedMessages);
        }

        public Map<String, Long> getRetriedMessages() {
            return Collections.unmodifiableMap(retriedMessages);
        }

        public Map<String, Long> getDeadLetteredMessages() {
            return Collections.unmodifiableMap(deadLetteredMessages);
        }

        public Map<String, Long> getDeadLetterRetries() {
            return Collections.unmodifiableMap(deadLetterRetries);
        }
    }

    // Example usage
    public static void main(String[] args) {
        // Create file-based persistence
        MessagePersistence<String> persistence = new MessagePersistence<>() {
            private final Map<String, Message<String>> storage = 
                new ConcurrentHashMap<>();

            @Override
            public void persist(String queueName, Message<String> message) {
                storage.put(message.getId(), message);
                System.out.println(
                    "Persisted message " + message.getId() + 
                    " to queue " + queueName
                );
            }

            @Override
            public void delete(String queueName, String messageId) {
                storage.remove(messageId);
                System.out.println(
                    "Deleted message " + messageId + 
                    " from queue " + queueName
                );
            }
        };

        // Configure and create message queue
        MessageQueue<String> queue = new MessageQueue<>(
            new QueueConfig.Builder<String>()
                .workerThreads(4)
                .persistence(persistence)
                .retryPolicy(new RetryPolicy(3, 1000, 2.0))
                .build()
        );

        // Create additional queues
        queue.createQueue("high-priority", 50);
        queue.createQueue("low-priority", 200);

        // Register handlers
        queue.registerHandler("default", message -> 
            System.out.println("Processing default: " + message)
        );
        queue.registerHandler("high-priority", message -> {
            System.out.println("Processing high-priority: " + message);
            if (message.contains("error")) {
                throw new RuntimeException("Simulated error");
            }
        });
        queue.registerHandler("low-priority", message ->
            System.out.println("Processing low-priority: " + message)
        );

        // Send messages
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Normal messages
        futures.add(queue.send("default", "Message 1"));
        futures.add(queue.send("high-priority", "Message 2", 2));
        futures.add(queue.send("low-priority", "Message 3", 0));

        // Message that will fail and retry
        futures.add(queue.send("high-priority", "error_message", 1));

        // Wait for processing to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                try {
                    // Allow time for retries and dead-lettering
                    Thread.sleep(5000);

                    // Print metrics
                    QueueMetrics metrics = queue.getMetrics();
                    System.out.println("\nQueue Metrics:");
                    System.out.println("Sent messages: " + 
                        metrics.getSentMessages());
                    System.out.println("Processed messages: " + 
                        metrics.getProcessedMessages());
                    System.out.println("Failed messages: " + 
                        metrics.getFailedMessages());
                    System.out.println("Retried messages: " + 
                        metrics.getRetriedMessages());
                    System.out.println("Dead-lettered messages: " + 
                        metrics.getDeadLetteredMessages());
                    System.out.println("Dead letter retries: " + 
                        metrics.getDeadLetterRetries());

                    // Print dead letters
                    System.out.println("\nDead Letters:");
                    queue.getDeadLetters("high-priority").forEach(dl -> 
                        System.out.println(
                            dl.getMessage().getId() + ": " + 
                            dl.getError().getMessage()
                        )
                    );

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // Cleanup
                    queue.shutdown();
                }
            })
            .join();
    }
}