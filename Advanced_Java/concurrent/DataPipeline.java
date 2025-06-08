package concurrent;

import java.util.concurrent.*;
import java.util.*;
import java.util.function.*;
import java.time.*;
import java.util.stream.*;
import java.nio.ByteBuffer;

public class DataPipeline<T> {
    private final BlockingQueue<DataItem<T>> sourceQueue;
    private final List<ProcessingStage<T>> stages;
    private final ExecutorService executorService;
    private final MetricsCollector metrics;
    private final ErrorHandler<T> errorHandler;
    private final int backpressureThreshold;
    private volatile boolean isRunning;

    public DataPipeline(PipelineConfig<T> config) {
        this.sourceQueue = new LinkedBlockingQueue<>(config.queueCapacity);
        this.stages = new ArrayList<>();
        this.executorService = Executors.newFixedThreadPool(config.threadCount);
        this.metrics = new MetricsCollector();
        this.errorHandler = config.errorHandler;
        this.backpressureThreshold = config.backpressureThreshold;
        this.isRunning = true;
    }

    public <R> DataPipeline<R> addStage(String stageName, 
                                       Function<T, CompletableFuture<R>> processor) {
        ProcessingStage<T> stage = new ProcessingStage<>(
            stageName, processor, metrics, errorHandler
        );
        stages.add(stage);
        return (DataPipeline<R>) this;
    }

    public CompletableFuture<Void> submit(T data) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (sourceQueue.size() >= backpressureThreshold) {
                    handleBackpressure();
                }
                
                DataItem<T> item = new DataItem<>(data, Instant.now());
                sourceQueue.put(item);
                metrics.recordSubmission();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
        }, executorService);
    }

    private void handleBackpressure() {
        metrics.recordBackpressure();
        while (sourceQueue.size() >= backpressureThreshold) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
        }
    }

    public void start() {
        CompletableFuture.runAsync(this::processItems, executorService);
    }

    private void processItems() {
        while (isRunning || !sourceQueue.isEmpty()) {
            try {
                DataItem<T> item = sourceQueue.poll(100, TimeUnit.MILLISECONDS);
                if (item != null) {
                    processItem(item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processItem(DataItem<T> item) {
        CompletableFuture<?> future = CompletableFuture.completedFuture(item);
        
        for (ProcessingStage<T> stage : stages) {
            future = future.thenCompose(data -> {
                Instant start = Instant.now();
                return stage.process((DataItem<T>) data)
                    .whenComplete((result, error) -> {
                        Duration duration = Duration.between(start, Instant.now());
                        if (error != null) {
                            metrics.recordStageError(stage.getName());
                        } else {
                            metrics.recordStageSuccess(stage.getName(), duration);
                        }
                    });
            });
        }

        future.exceptionally(error -> {
            errorHandler.handleError(item.getData(), error);
            return null;
        });
    }

    public PipelineMetrics getMetrics() {
        return metrics.getMetrics();
    }

    public void shutdown() {
        isRunning = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Pipeline configuration
    public static class PipelineConfig<T> {
        private final int queueCapacity;
        private final int threadCount;
        private final int backpressureThreshold;
        private final ErrorHandler<T> errorHandler;

        private PipelineConfig(Builder<T> builder) {
            this.queueCapacity = builder.queueCapacity;
            this.threadCount = builder.threadCount;
            this.backpressureThreshold = builder.backpressureThreshold;
            this.errorHandler = builder.errorHandler;
        }

        public static class Builder<T> {
            private int queueCapacity = 1000;
            private int threadCount = Runtime.getRuntime().availableProcessors();
            private int backpressureThreshold = 800;
            private ErrorHandler<T> errorHandler = (data, error) -> 
                System.err.println("Error processing: " + error.getMessage());

            public Builder<T> queueCapacity(int capacity) {
                this.queueCapacity = capacity;
                return this;
            }

            public Builder<T> threadCount(int threads) {
                this.threadCount = threads;
                return this;
            }

            public Builder<T> backpressureThreshold(int threshold) {
                this.backpressureThreshold = threshold;
                return this;
            }

            public Builder<T> errorHandler(ErrorHandler<T> handler) {
                this.errorHandler = handler;
                return this;
            }

            public PipelineConfig<T> build() {
                return new PipelineConfig<>(this);
            }
        }
    }

    // Processing stage
    private static class ProcessingStage<T> {
        private final String name;
        private final Function<T, CompletableFuture<?>> processor;
        private final MetricsCollector metrics;
        private final ErrorHandler<T> errorHandler;

        public ProcessingStage(String name, 
                             Function<T, CompletableFuture<?>> processor,
                             MetricsCollector metrics,
                             ErrorHandler<T> errorHandler) {
            this.name = name;
            this.processor = processor;
            this.metrics = metrics;
            this.errorHandler = errorHandler;
        }

        public String getName() {
            return name;
        }

        public CompletableFuture<?> process(DataItem<T> item) {
            return processor.apply(item.getData())
                .thenApply(result -> new DataItem<>(result, item.getTimestamp()))
                .exceptionally(error -> {
                    errorHandler.handleError(item.getData(), error);
                    throw new CompletionException(error);
                });
        }
    }

    // Data item with metadata
    private static class DataItem<T> {
        private final T data;
        private final Instant timestamp;

        public DataItem(T data, Instant timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }

        public T getData() {
            return data;
        }

        public Instant getTimestamp() {
            return timestamp;
        }
    }

    // Metrics collection
    private static class MetricsCollector {
        private final Map<String, StageMetrics> stageMetrics;
        private final AtomicLong submissionCount;
        private final AtomicLong backpressureCount;

        public MetricsCollector() {
            this.stageMetrics = new ConcurrentHashMap<>();
            this.submissionCount = new AtomicLong();
            this.backpressureCount = new AtomicLong();
        }

        public void recordSubmission() {
            submissionCount.incrementAndGet();
        }

        public void recordBackpressure() {
            backpressureCount.incrementAndGet();
        }

        public void recordStageSuccess(String stageName, Duration duration) {
            stageMetrics.computeIfAbsent(stageName, k -> new StageMetrics())
                       .recordSuccess(duration);
        }

        public void recordStageError(String stageName) {
            stageMetrics.computeIfAbsent(stageName, k -> new StageMetrics())
                       .recordError();
        }

        public PipelineMetrics getMetrics() {
            return new PipelineMetrics(
                submissionCount.get(),
                backpressureCount.get(),
                new HashMap<>(stageMetrics)
            );
        }
    }

    // Stage metrics
    private static class StageMetrics {
        private final AtomicLong successCount;
        private final AtomicLong errorCount;
        private final AtomicLong totalProcessingTime;

        public StageMetrics() {
            this.successCount = new AtomicLong();
            this.errorCount = new AtomicLong();
            this.totalProcessingTime = new AtomicLong();
        }

        public void recordSuccess(Duration duration) {
            successCount.incrementAndGet();
            totalProcessingTime.addAndGet(duration.toMillis());
        }

        public void recordError() {
            errorCount.incrementAndGet();
        }

        public long getSuccessCount() {
            return successCount.get();
        }

        public long getErrorCount() {
            return errorCount.get();
        }

        public Duration getAverageProcessingTime() {
            long count = successCount.get();
            return count > 0 
                ? Duration.ofMillis(totalProcessingTime.get() / count)
                : Duration.ZERO;
        }
    }

    // Pipeline metrics
    public static class PipelineMetrics {
        private final long submissionCount;
        private final long backpressureCount;
        private final Map<String, StageMetrics> stageMetrics;

        public PipelineMetrics(long submissionCount,
                             long backpressureCount,
                             Map<String, StageMetrics> stageMetrics) {
            this.submissionCount = submissionCount;
            this.backpressureCount = backpressureCount;
            this.stageMetrics = stageMetrics;
        }

        public long getSubmissionCount() {
            return submissionCount;
        }

        public long getBackpressureCount() {
            return backpressureCount;
        }

        public Map<String, StageMetrics> getStageMetrics() {
            return Collections.unmodifiableMap(stageMetrics);
        }
    }

    // Error handler interface
    @FunctionalInterface
    public interface ErrorHandler<T> {
        void handleError(T data, Throwable error);
    }

    // Example usage
    public static void main(String[] args) {
        // Create pipeline configuration
        PipelineConfig<String> config = new PipelineConfig.Builder<String>()
            .queueCapacity(1000)
            .threadCount(4)
            .backpressureThreshold(800)
            .errorHandler((data, error) -> 
                System.err.println("Error processing: " + data + 
                    " - " + error.getMessage())
            )
            .build();

        // Create and configure pipeline
        DataPipeline<String> pipeline = new DataPipeline<>(config);

        // Add processing stages
        pipeline
            .addStage("validation", data -> 
                CompletableFuture.supplyAsync(() -> {
                    if (data.isEmpty()) {
                        throw new IllegalArgumentException("Empty data");
                    }
                    return data.toUpperCase();
                }))
            .addStage("processing", data ->
                CompletableFuture.supplyAsync(() -> {
                    // Simulate processing
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return data + "_PROCESSED";
                }))
            .addStage("enrichment", data ->
                CompletableFuture.supplyAsync(() ->
                    data + "_" + Instant.now().toEpochMilli()
                ));

        // Start the pipeline
        pipeline.start();

        // Submit data
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String data = "Data_" + i;
            futures.add(pipeline.submit(data));
        }

        // Submit some invalid data
        futures.add(pipeline.submit(""));

        // Wait for processing to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                // Print metrics
                PipelineMetrics metrics = pipeline.getMetrics();
                System.out.println("\nPipeline Metrics:");
                System.out.println("Submissions: " + metrics.getSubmissionCount());
                System.out.println("Backpressure events: " + 
                    metrics.getBackpressureCount());
                
                System.out.println("\nStage Metrics:");
                metrics.getStageMetrics().forEach((stage, stageMetrics) -> {
                    System.out.println("\n" + stage + ":");
                    System.out.println("Successes: " + 
                        stageMetrics.getSuccessCount());
                    System.out.println("Errors: " + stageMetrics.getErrorCount());
                    System.out.println("Avg. Processing Time: " + 
                        stageMetrics.getAverageProcessingTime().toMillis() + "ms");
                });

                // Shutdown the pipeline
                pipeline.shutdown();
            })
            .join();
    }
}