package reactive;

import java.util.concurrent.*;
import java.util.function.*;
import java.util.*;
import java.time.Duration;
import java.util.concurrent.atomic.*;

public class EventProcessor<T> {
    private final BlockingQueue<Event<T>> eventQueue;
    private final List<EventSubscriber<T>> subscribers;
    private final ExecutorService executorService;
    private final BackpressureStrategy backpressureStrategy;
    private final EventMetrics metrics;
    private volatile boolean running;
    private final int bufferSize;

    public EventProcessor(int bufferSize, BackpressureStrategy strategy) {
        this.bufferSize = bufferSize;
        this.eventQueue = new LinkedBlockingQueue<>(bufferSize);
        this.subscribers = new CopyOnWriteArrayList<>();
        this.executorService = Executors.newWorkStealingPool();
        this.backpressureStrategy = strategy;
        this.metrics = new EventMetrics();
        this.running = true;

        startEventProcessor();
    }

    public void publish(T data) {
        Event<T> event = new Event<>(data);
        
        if (!tryPublishWithBackpressure(event)) {
            handleBackpressure(event);
        }
    }

    private boolean tryPublishWithBackpressure(Event<T> event) {
        try {
            switch (backpressureStrategy) {
                case DROP:
                    return eventQueue.offer(event);
                    
                case BUFFER:
                    eventQueue.put(event);
                    return true;
                    
                case LATEST:
                    if (!eventQueue.offer(event)) {
                        eventQueue.poll();
                        return eventQueue.offer(event);
                    }
                    return true;
                    
                default:
                    throw new IllegalStateException(
                        "Unknown backpressure strategy");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void handleBackpressure(Event<T> event) {
        metrics.recordDropped();
        if (backpressureStrategy == BackpressureStrategy.ERROR) {
            throw new BackpressureException(
                "Event queue full: " + bufferSize);
        }
    }

    public Subscription subscribe(
            EventSubscriber<T> subscriber,
            Function<T, Boolean> filter,
            Function<T, T> transformer) {
        
        EventSubscriber<T> processedSubscriber = 
            new ProcessedEventSubscriber<>(
                subscriber, filter, transformer);
        subscribers.add(processedSubscriber);
        
        return () -> subscribers.remove(processedSubscriber);
    }

    private void startEventProcessor() {
        Thread processor = new Thread(() -> {
            while (running) {
                try {
                    Event<T> event = eventQueue.take();
                    processEvent(event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        processor.setDaemon(true);
        processor.start();
    }

    private void processEvent(Event<T> event) {
        metrics.recordProcessed();
        
        List<CompletableFuture<Void>> futures = 
            subscribers.stream()
                .map(subscriber -> CompletableFuture
                    .runAsync(() -> {
                        try {
                            subscriber.onEvent(event);
                            metrics.recordDelivered();
                        } catch (Exception e) {
                            metrics.recordError();
                            subscriber.onError(e);
                        }
                    }, executorService))
                .toList();

        CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]))
            .exceptionally(throwable -> {
                metrics.recordError();
                return null;
            });
    }

    public void shutdown() {
        running = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public EventMetrics.Snapshot getMetrics() {
        return metrics.snapshot();
    }

    // Event class
    public static class Event<T> {
        private final T data;
        private final long timestamp;

        public Event(T data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public T getData() { return data; }
        public long getTimestamp() { return timestamp; }
    }

    // Subscriber interface
    @FunctionalInterface
    public interface EventSubscriber<T> {
        void onEvent(Event<T> event);
        default void onError(Throwable error) {
            error.printStackTrace();
        }
    }

    // Processed subscriber wrapper
    private static class ProcessedEventSubscriber<T> 
            implements EventSubscriber<T> {
        
        private final EventSubscriber<T> delegate;
        private final Function<T, Boolean> filter;
        private final Function<T, T> transformer;

        public ProcessedEventSubscriber(
                EventSubscriber<T> delegate,
                Function<T, Boolean> filter,
                Function<T, T> transformer) {
            this.delegate = delegate;
            this.filter = filter;
            this.transformer = transformer;
        }

        @Override
        public void onEvent(Event<T> event) {
            T data = event.getData();
            if (filter.apply(data)) {
                T transformed = transformer.apply(data);
                delegate.onEvent(new Event<>(transformed));
            }
        }

        @Override
        public void onError(Throwable error) {
            delegate.onError(error);
        }
    }

    // Subscription interface
    @FunctionalInterface
    public interface Subscription {
        void unsubscribe();
    }

    // Backpressure strategies
    public enum BackpressureStrategy {
        DROP,    // Drop new events when buffer is full
        BUFFER,  // Wait until space is available
        LATEST,  // Keep only the latest events
        ERROR    // Throw exception when buffer is full
    }

    // Custom exception
    public static class BackpressureException extends RuntimeException {
        public BackpressureException(String message) {
            super(message);
        }
    }

    // Metrics tracking
    private static class EventMetrics {
        private final AtomicLong processed = new AtomicLong();
        private final AtomicLong delivered = new AtomicLong();
        private final AtomicLong dropped = new AtomicLong();
        private final AtomicLong errors = new AtomicLong();

        public void recordProcessed() {
            processed.incrementAndGet();
        }

        public void recordDelivered() {
            delivered.incrementAndGet();
        }

        public void recordDropped() {
            dropped.incrementAndGet();
        }

        public void recordError() {
            errors.incrementAndGet();
        }

        public Snapshot snapshot() {
            return new Snapshot(
                processed.get(),
                delivered.get(),
                dropped.get(),
                errors.get()
            );
        }

        public static class Snapshot {
            private final long processed;
            private final long delivered;
            private final long dropped;
            private final long errors;

            public Snapshot(
                    long processed,
                    long delivered,
                    long dropped,
                    long errors) {
                this.processed = processed;
                this.delivered = delivered;
                this.dropped = dropped;
                this.errors = errors;
            }

            @Override
            public String toString() {
                return String.format(
                    "Event Metrics - Processed: %d, Delivered: %d, " +
                    "Dropped: %d, Errors: %d",
                    processed, delivered, dropped, errors
                );
            }
        }
    }

    // Example usage
    public static void main(String[] args) {
        EventProcessor<String> processor = 
            new EventProcessor<>(1000, BackpressureStrategy.BUFFER);

        try {
            // Add subscribers
            processor.subscribe(
                event -> System.out.println(
                    "Subscriber 1: " + event.getData()),
                data -> data.length() > 5,  // filter
                String::toUpperCase        // transformer
            );

            processor.subscribe(
                event -> System.out.println(
                    "Subscriber 2: " + event.getData()),
                data -> true,              // no filter
                data -> data + "!"         // transformer
            );

            // Publish events
            processor.publish("Hello");
            processor.publish("World");
            processor.publish("Events");

            // Wait for processing
            Thread.sleep(1000);

            // Print metrics
            System.out.println(processor.getMetrics());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            processor.shutdown();
        }
    }
}