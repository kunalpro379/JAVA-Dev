package scheduling;

import java.util.concurrent.*;
import java.util.*;
import java.time.*;
import java.util.function.*;

public class TaskScheduler {
    private final ScheduledExecutorService scheduler;
    private final PriorityBlockingQueue<ScheduledTask<?>> taskQueue;
    private final Map<String, ScheduledTask<?>> taskRegistry;
    private final Map<String, Set<String>> taskDependencies;
    private final SchedulerMetrics metrics;
    private volatile boolean running;

    public TaskScheduler(int poolSize) {
        this.scheduler = Executors.newScheduledThreadPool(poolSize);
        this.taskQueue = new PriorityBlockingQueue<>();
        this.taskRegistry = new ConcurrentHashMap<>();
        this.taskDependencies = new ConcurrentHashMap<>();
        this.metrics = new SchedulerMetrics();
        this.running = true;

        // Start task processing
        startTaskProcessor();
    }

    public <T> Future<T> schedule(Task<T> task, TaskConfig config) {
        ScheduledTask<T> scheduledTask = new ScheduledTask<>(
            task, config, generateTaskId());
        
        taskRegistry.put(scheduledTask.getId(), scheduledTask);
        taskQueue.offer(scheduledTask);
        metrics.recordScheduled();
        
        return scheduledTask.getFuture();
    }

    public <T> Future<T> scheduleWithDependencies(
            Task<T> task, 
            TaskConfig config,
            Set<String> dependencies) {
        
        String taskId = generateTaskId();
        taskDependencies.put(taskId, dependencies);
        
        ScheduledTask<T> scheduledTask = new ScheduledTask<>(
            task, config, taskId);
        
        taskRegistry.put(taskId, scheduledTask);
        
        if (areDependenciesMet(taskId)) {
            taskQueue.offer(scheduledTask);
        }
        
        metrics.recordScheduled();
        return scheduledTask.getFuture();
    }

    public void cancel(String taskId) {
        ScheduledTask<?> task = taskRegistry.remove(taskId);
        if (task != null) {
            task.cancel();
            taskQueue.remove(task);
            metrics.recordCancelled();
        }
    }

    public boolean isDependencyMet(String taskId) {
        return !taskRegistry.containsKey(taskId) ||
               taskRegistry.get(taskId).isCompleted();
    }

    private boolean areDependenciesMet(String taskId) {
        Set<String> deps = taskDependencies.getOrDefault(
            taskId, Collections.emptySet());
        return deps.stream().allMatch(this::isDependencyMet);
    }

    private void startTaskProcessor() {
        Thread processor = new Thread(() -> {
            while (running) {
                try {
                    processNextTask();
                } catch (Exception e) {
                    metrics.recordError();
                }
            }
        });
        processor.setDaemon(true);
        processor.start();
    }

    private void processNextTask() throws InterruptedException {
        ScheduledTask<?> task = taskQueue.take();
        
        if (!areDependenciesMet(task.getId())) {
            taskQueue.offer(task);
            Thread.sleep(100);
            return;
        }

        if (task.shouldExecuteNow()) {
            executeTask(task);
        } else {
            taskQueue.offer(task);
            Thread.sleep(100);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void executeTask(ScheduledTask<T> task) {
        if (task.isCancelled()) {
            return;
        }

        try {
            TaskConfig config = task.getConfig();
            Callable<T> callable = () -> {
                try {
                    metrics.recordStarted();
                    T result = task.getTask().execute();
                    metrics.recordCompleted();
                    
                    if (config.isRecurring()) {
                        rescheduleTask(task);
                    }
                    
                    return result;
                } catch (Exception e) {
                    metrics.recordError();
                    throw e;
                }
            };

            ScheduledFuture<T> future;
            if (config.getDelay() > 0) {
                future = scheduler.schedule(
                    callable, 
                    config.getDelay(), 
                    config.getTimeUnit()
                );
            } else {
                future = (ScheduledFuture<T>) 
                    scheduler.submit(callable);
            }
            
            task.setScheduledFuture(future);

        } catch (Exception e) {
            task.completeExceptionally(e);
            metrics.recordError();
        }
    }

    private <T> void rescheduleTask(ScheduledTask<T> task) {
        TaskConfig config = task.getConfig();
        if (config.getMaxExecutions() == 0 || 
            task.getExecutionCount() < config.getMaxExecutions()) {
            
            ScheduledTask<T> newTask = new ScheduledTask<>(
                task.getTask(),
                config,
                generateTaskId()
            );
            
            taskRegistry.put(newTask.getId(), newTask);
            taskQueue.offer(newTask);
        }
    }

    private String generateTaskId() {
        return UUID.randomUUID().toString();
    }

    public void shutdown() {
        running = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public SchedulerMetrics.Snapshot getMetrics() {
        return metrics.snapshot();
    }

    // Task interface
    @FunctionalInterface
    public interface Task<T> {
        T execute() throws Exception;
    }

    // Task configuration
    public static class TaskConfig {
        private final long delay;
        private final TimeUnit timeUnit;
        private final TaskPriority priority;
        private final boolean recurring;
        private final long maxExecutions;
        private final Duration timeout;

        private TaskConfig(Builder builder) {
            this.delay = builder.delay;
            this.timeUnit = builder.timeUnit;
            this.priority = builder.priority;
            this.recurring = builder.recurring;
            this.maxExecutions = builder.maxExecutions;
            this.timeout = builder.timeout;
        }

        public long getDelay() { return delay; }
        public TimeUnit getTimeUnit() { return timeUnit; }
        public TaskPriority getPriority() { return priority; }
        public boolean isRecurring() { return recurring; }
        public long getMaxExecutions() { return maxExecutions; }
        public Duration getTimeout() { return timeout; }

        public static class Builder {
            private long delay = 0;
            private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
            private TaskPriority priority = TaskPriority.NORMAL;
            private boolean recurring = false;
            private long maxExecutions = 0;
            private Duration timeout = Duration.ofMinutes(1);

            public Builder delay(long delay, TimeUnit unit) {
                this.delay = delay;
                this.timeUnit = unit;
                return this;
            }

            public Builder priority(TaskPriority priority) {
                this.priority = priority;
                return this;
            }

            public Builder recurring(boolean recurring) {
                this.recurring = recurring;
                return this;
            }

            public Builder maxExecutions(long maxExecutions) {
                this.maxExecutions = maxExecutions;
                return this;
            }

            public Builder timeout(Duration timeout) {
                this.timeout = timeout;
                return this;
            }

            public TaskConfig build() {
                return new TaskConfig(this);
            }
        }
    }

    // Scheduled task wrapper
    private static class ScheduledTask<T> 
            implements Comparable<ScheduledTask<?>> {
        
        private final Task<T> task;
        private final TaskConfig config;
        private final String id;
        private final CompletableFuture<T> future;
        private volatile ScheduledFuture<T> scheduledFuture;
        private final AtomicLong executionCount;
        private volatile boolean completed;

        public ScheduledTask(Task<T> task, TaskConfig config, String id) {
            this.task = task;
            this.config = config;
            this.id = id;
            this.future = new CompletableFuture<>();
            this.executionCount = new AtomicLong();
            this.completed = false;
        }

        public Task<T> getTask() { return task; }
        public TaskConfig getConfig() { return config; }
        public String getId() { return id; }
        public CompletableFuture<T> getFuture() { return future; }
        public long getExecutionCount() { 
            return executionCount.get(); 
        }
        public boolean isCompleted() { return completed; }

        public void setScheduledFuture(ScheduledFuture<T> future) {
            this.scheduledFuture = future;
            executionCount.incrementAndGet();
        }

        public void cancel() {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(true);
            }
            future.cancel(true);
        }

        public boolean isCancelled() {
            return future.isCancelled();
        }

        public void completeExceptionally(Throwable ex) {
            completed = true;
            future.completeExceptionally(ex);
        }

        public boolean shouldExecuteNow() {
            return System.currentTimeMillis() >= 
                   config.getTimeUnit().toMillis(config.getDelay());
        }

        @Override
        public int compareTo(ScheduledTask<?> other) {
            return this.config.getPriority().compareTo(
                other.config.getPriority());
        }
    }

    // Task priority enum
    public enum TaskPriority {
        HIGH(0), NORMAL(1), LOW(2);

        private final int value;

        TaskPriority(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    // Scheduler metrics
    private static class SchedulerMetrics {
        private final AtomicLong scheduled = new AtomicLong();
        private final AtomicLong started = new AtomicLong();
        private final AtomicLong completed = new AtomicLong();
        private final AtomicLong cancelled = new AtomicLong();
        private final AtomicLong errors = new AtomicLong();

        public void recordScheduled() {
            scheduled.incrementAndGet();
        }

        public void recordStarted() {
            started.incrementAndGet();
        }

        public void recordCompleted() {
            completed.incrementAndGet();
        }

        public void recordCancelled() {
            cancelled.incrementAndGet();
        }

        public void recordError() {
            errors.incrementAndGet();
        }

        public Snapshot snapshot() {
            return new Snapshot(
                scheduled.get(),
                started.get(),
                completed.get(),
                cancelled.get(),
                errors.get()
            );
        }

        public static class Snapshot {
            private final long scheduled;
            private final long started;
            private final long completed;
            private final long cancelled;
            private final long errors;

            public Snapshot(
                    long scheduled,
                    long started,
                    long completed,
                    long cancelled,
                    long errors) {
                this.scheduled = scheduled;
                this.started = started;
                this.completed = completed;
                this.cancelled = cancelled;
                this.errors = errors;
            }

            @Override
            public String toString() {
                return String.format(
                    "Scheduler Metrics - Scheduled: %d, Started: %d, " +
                    "Completed: %d, Cancelled: %d, Errors: %d",
                    scheduled, started, completed, cancelled, errors
                );
            }
        }
    }

    // Example usage
    public static void main(String[] args) {
        TaskScheduler scheduler = new TaskScheduler(4);

        try {
            // Schedule a simple task
            Future<String> future1 = scheduler.schedule(
                () -> {
                    Thread.sleep(1000);
                    return "Task 1 completed";
                },
                new TaskConfig.Builder()
                    .priority(TaskPriority.HIGH)
                    .build()
            );

            // Schedule a recurring task
            Future<Void> future2 = scheduler.schedule(
                () -> {
                    System.out.println("Recurring task executed at: " + 
                        Instant.now());
                    return null;
                },
                new TaskConfig.Builder()
                    .delay(2, TimeUnit.SECONDS)
                    .recurring(true)
                    .maxExecutions(5)
                    .build()
            );

            // Schedule tasks with dependencies
            String taskId1 = UUID.randomUUID().toString();
            Future<String> future3 = scheduler.schedule(
                () -> "Task 3 completed",
                new TaskConfig.Builder()
                    .priority(TaskPriority.NORMAL)
                    .build()
            );

            Future<String> future4 = scheduler.scheduleWithDependencies(
                () -> "Task 4 completed",
                new TaskConfig.Builder()
                    .priority(TaskPriority.LOW)
                    .build(),
                Set.of(taskId1)
            );

            // Wait for results
            System.out.println(future1.get());
            Thread.sleep(10000);

            // Print metrics
            System.out.println(scheduler.getMetrics());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scheduler.shutdown();
        }
    }
}