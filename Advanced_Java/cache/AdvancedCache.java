package cache;

import java.util.concurrent.*;
import java.util.*;
import java.time.*;
import java.util.function.*;
import java.nio.file.*;
import java.io.*;

public class AdvancedCache<K, V> {
    private final ConcurrentMap<K, CacheEntry<V>> cache;
    private final EvictionPolicy evictionPolicy;
    private final CachePersistence<K, V> persistence;
    private final CacheMetrics metrics;
    private final LoadingCache<K, V> loadingCache;
    private final ScheduledExecutorService maintenance;
    private final Duration ttl;
    private final long maxSize;

    public AdvancedCache(CacheConfig<K, V> config) {
        this.cache = new ConcurrentHashMap<>();
        this.evictionPolicy = config.evictionPolicy;
        this.persistence = config.persistence;
        this.metrics = new CacheMetrics();
        this.loadingCache = new LoadingCache<>(config.loader);
        this.maintenance = Executors.newSingleThreadScheduledExecutor();
        this.ttl = config.ttl;
        this.maxSize = config.maxSize;

        // Start maintenance tasks
        startMaintenanceTasks();
    }

    public V get(K key) {
        try {
            CacheEntry<V> entry = cache.get(key);
            if (entry != null) {
                if (isExpired(entry)) {
                    remove(key);
                    metrics.recordEviction("expired");
                    return loadValue(key);
                }
                metrics.recordHit();
                evictionPolicy.recordAccess(key);
                return entry.getValue();
            }

            metrics.recordMiss();
            return loadValue(key);

        } catch (Exception e) {
            metrics.recordError();
            throw new CacheException("Error getting value for key: " + key, e);
        }
    }

    public void put(K key, V value) {
        try {
            evict();
            CacheEntry<V> entry = new CacheEntry<>(value, Instant.now());
            cache.put(key, entry);
            evictionPolicy.recordInsertion(key);
            metrics.recordPut();
            persistence.persist(key, value);

        } catch (Exception e) {
            metrics.recordError();
            throw new CacheException("Error putting value for key: " + key, e);
        }
    }

    public void remove(K key) {
        try {
            cache.remove(key);
            evictionPolicy.recordRemoval(key);
            metrics.recordRemoval();
            persistence.remove(key);

        } catch (Exception e) {
            metrics.recordError();
            throw new CacheException("Error removing key: " + key, e);
        }
    }

    public void clear() {
        try {
            cache.clear();
            evictionPolicy.clear();
            metrics.recordClear();
            persistence.clear();

        } catch (Exception e) {
            metrics.recordError();
            throw new CacheException("Error clearing cache", e);
        }
    }

    private V loadValue(K key) {
        try {
            // Try loading from persistence first
            Optional<V> persisted = persistence.load(key);
            if (persisted.isPresent()) {
                V value = persisted.get();
                put(key, value);
                return value;
            }

            // Load using cache loader
            return loadingCache.load(key).thenApply(value -> {
                put(key, value);
                return value;
            }).join();

        } catch (Exception e) {
            metrics.recordError();
            throw new CacheException("Error loading value for key: " + key, e);
        }
    }

    private void evict() {
        if (cache.size() >= maxSize) {
            K keyToEvict = evictionPolicy.selectEvictionCandidate();
            if (keyToEvict != null) {
                remove(keyToEvict);
                metrics.recordEviction("size");
            }
        }
    }

    private boolean isExpired(CacheEntry<V> entry) {
        return ttl != null && 
               Duration.between(entry.getTimestamp(), Instant.now())
                      .compareTo(ttl) > 0;
    }

    private void startMaintenanceTasks() {
        // Schedule periodic eviction of expired entries
        maintenance.scheduleAtFixedRate(() -> {
            try {
                cache.entrySet().removeIf(entry -> 
                    isExpired(entry.getValue()));
            } catch (Exception e) {
                metrics.recordError();
            }
        }, 1, 1, TimeUnit.MINUTES);

        // Schedule metrics reporting
        maintenance.scheduleAtFixedRate(() -> {
            CacheMetrics.Snapshot snapshot = metrics.snapshot();
            System.out.println("\nCache Metrics:");
            System.out.println("Size: " + cache.size());
            System.out.println("Hits: " + snapshot.getHits());
            System.out.println("Misses: " + snapshot.getMisses());
            System.out.println("Hit ratio: " + snapshot.getHitRatio());
            System.out.println("Evictions: " + snapshot.getEvictions());
            System.out.println("Errors: " + snapshot.getErrors());
        }, 1, 1, TimeUnit.MINUTES);

        // Schedule periodic persistence
        maintenance.scheduleAtFixedRate(() -> {
            try {
                persistence.persistAll(cache.entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getValue()
                    )));
            } catch (Exception e) {
                metrics.recordError();
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    public void shutdown() {
        maintenance.shutdown();
        try {
            if (!maintenance.awaitTermination(30, TimeUnit.SECONDS)) {
                maintenance.shutdownNow();
            }
        } catch (InterruptedException e) {
            maintenance.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Cache entry
    private static class CacheEntry<V> {
        private final V value;
        private final Instant timestamp;

        public CacheEntry(V value, Instant timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        public V getValue() { return value; }
        public Instant getTimestamp() { return timestamp; }
    }

    // Eviction policy interface
    public interface EvictionPolicy {
        void recordAccess(Object key);
        void recordInsertion(Object key);
        void recordRemoval(Object key);
        Object selectEvictionCandidate();
        void clear();
    }

    // LRU eviction policy
    public static class LRUEvictionPolicy implements EvictionPolicy {
        private final Map<Object, Instant> accessTimes = 
            new ConcurrentHashMap<>();

        @Override
        public void recordAccess(Object key) {
            accessTimes.put(key, Instant.now());
        }

        @Override
        public void recordInsertion(Object key) {
            accessTimes.put(key, Instant.now());
        }

        @Override
        public void recordRemoval(Object key) {
            accessTimes.remove(key);
        }

        @Override
        public Object selectEvictionCandidate() {
            return accessTimes.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        }

        @Override
        public void clear() {
            accessTimes.clear();
        }
    }

    // LFU eviction policy
    public static class LFUEvictionPolicy implements EvictionPolicy {
        private final Map<Object, Long> accessCounts = 
            new ConcurrentHashMap<>();

        @Override
        public void recordAccess(Object key) {
            accessCounts.merge(key, 1L, Long::sum);
        }

        @Override
        public void recordInsertion(Object key) {
            accessCounts.put(key, 0L);
        }

        @Override
        public void recordRemoval(Object key) {
            accessCounts.remove(key);
        }

        @Override
        public Object selectEvictionCandidate() {
            return accessCounts.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        }

        @Override
        public void clear() {
            accessCounts.clear();
        }
    }

    // Cache persistence interface
    public interface CachePersistence<K, V> {
        void persist(K key, V value) throws Exception;
        void persistAll(Map<K, V> entries) throws Exception;
        Optional<V> load(K key) throws Exception;
        void remove(K key) throws Exception;
        void clear() throws Exception;
    }

    // File-based persistence implementation
    public static class FilePersistence<K, V> implements CachePersistence<K, V> {
        private final Path directory;
        private final ObjectMapper mapper;

        public FilePersistence(Path directory) {
            this.directory = directory;
            this.mapper = new ObjectMapper();
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                throw new CacheException("Error creating persistence directory", e);
            }
        }

        @Override
        public void persist(K key, V value) throws Exception {
            Path file = directory.resolve(key.toString());
            mapper.writeValue(file.toFile(), value);
        }

        @Override
        public void persistAll(Map<K, V> entries) throws Exception {
            for (Map.Entry<K, V> entry : entries.entrySet()) {
                persist(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public Optional<V> load(K key) throws Exception {
            Path file = directory.resolve(key.toString());
            if (Files.exists(file)) {
                return Optional.of(mapper.readValue(file.toFile(), 
                    new TypeReference<V>() {}));
            }
            return Optional.empty();
        }

        @Override
        public void remove(K key) throws Exception {
            Path file = directory.resolve(key.toString());
            Files.deleteIfExists(file);
        }

        @Override
        public void clear() throws Exception {
            Files.walk(directory)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        }
    }

    // Loading cache for async value loading
    private static class LoadingCache<K, V> {
        private final CacheLoader<K, V> loader;
        private final ExecutorService executor;

        public LoadingCache(CacheLoader<K, V> loader) {
            this.loader = loader;
            this.executor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
            );
        }

        public CompletableFuture<V> load(K key) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return loader.load(key);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }, executor);
        }
    }

    // Cache loader interface
    @FunctionalInterface
    public interface CacheLoader<K, V> {
        V load(K key) throws Exception;
    }

    // Cache metrics
    private static class CacheMetrics {
        private final AtomicLong hits = new AtomicLong();
        private final AtomicLong misses = new AtomicLong();
        private final AtomicLong puts = new AtomicLong();
        private final AtomicLong removals = new AtomicLong();
        private final AtomicLong evictions = new AtomicLong();
        private final AtomicLong errors = new AtomicLong();
        private final Map<String, AtomicLong> evictionsByReason = 
            new ConcurrentHashMap<>();

        public void recordHit() {
            hits.incrementAndGet();
        }

        public void recordMiss() {
            misses.incrementAndGet();
        }

        public void recordPut() {
            puts.incrementAndGet();
        }

        public void recordRemoval() {
            removals.incrementAndGet();
        }

        public void recordEviction(String reason) {
            evictions.incrementAndGet();
            evictionsByReason.computeIfAbsent(reason, k -> new AtomicLong())
                          .incrementAndGet();
        }

        public void recordError() {
            errors.incrementAndGet();
        }

        public void recordClear() {
            removals.addAndGet(puts.get());
            puts.set(0);
        }

        public Snapshot snapshot() {
            return new Snapshot(
                hits.get(),
                misses.get(),
                puts.get(),
                removals.get(),
                evictions.get(),
                errors.get(),
                new HashMap<>(evictionsByReason.entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get()
                    )))
            );
        }

        public static class Snapshot {
            private final long hits;
            private final long misses;
            private final long puts;
            private final long removals;
            private final long evictions;
            private final long errors;
            private final Map<String, Long> evictionsByReason;

            public Snapshot(
                long hits,
                long misses,
                long puts,
                long removals,
                long evictions,
                long errors,
                Map<String, Long> evictionsByReason
            ) {
                this.hits = hits;
                this.misses = misses;
                this.puts = puts;
                this.removals = removals;
                this.evictions = evictions;
                this.errors = errors;
                this.evictionsByReason = evictionsByReason;
            }

            public long getHits() { return hits; }
            public long getMisses() { return misses; }
            public long getPuts() { return puts; }
            public long getRemovals() { return removals; }
            public long getEvictions() { return evictions; }
            public long getErrors() { return errors; }
            
            public double getHitRatio() {
                long total = hits + misses;
                return total == 0 ? 0.0 : (double) hits / total;
            }
            
            public Map<String, Long> getEvictionsByReason() {
                return Collections.unmodifiableMap(evictionsByReason);
            }
        }
    }

    // Cache configuration
    public static class CacheConfig<K, V> {
        private final EvictionPolicy evictionPolicy;
        private final CachePersistence<K, V> persistence;
        private final CacheLoader<K, V> loader;
        private final Duration ttl;
        private final long maxSize;

        private CacheConfig(Builder<K, V> builder) {
            this.evictionPolicy = builder.evictionPolicy;
            this.persistence = builder.persistence;
            this.loader = builder.loader;
            this.ttl = builder.ttl;
            this.maxSize = builder.maxSize;
        }

        public static class Builder<K, V> {
            private EvictionPolicy evictionPolicy = new LRUEvictionPolicy();
            private CachePersistence<K, V> persistence = 
                new NoOpPersistence<>();
            private CacheLoader<K, V> loader = key -> null;
            private Duration ttl;
            private long maxSize = 1000;

            public Builder<K, V> evictionPolicy(EvictionPolicy policy) {
                this.evictionPolicy = policy;
                return this;
            }

            public Builder<K, V> persistence(CachePersistence<K, V> persistence) {
                this.persistence = persistence;
                return this;
            }

            public Builder<K, V> loader(CacheLoader<K, V> loader) {
                this.loader = loader;
                return this;
            }

            public Builder<K, V> ttl(Duration ttl) {
                this.ttl = ttl;
                return this;
            }

            public Builder<K, V> maxSize(long maxSize) {
                this.maxSize = maxSize;
                return this;
            }

            public CacheConfig<K, V> build() {
                return new CacheConfig<>(this);
            }
        }
    }

    // No-op persistence implementation
    private static class NoOpPersistence<K, V> implements CachePersistence<K, V> {
        @Override
        public void persist(K key, V value) {}

        @Override
        public void persistAll(Map<K, V> entries) {}

        @Override
        public Optional<V> load(K key) {
            return Optional.empty();
        }

        @Override
        public void remove(K key) {}

        @Override
        public void clear() {}
    }

    // Cache exception
    public static class CacheException extends RuntimeException {
        public CacheException(String message) {
            super(message);
        }

        public CacheException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Object mapper for persistence (simplified)
    private static class ObjectMapper {
        public void writeValue(File file, Object value) throws IOException {
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(file))) {
                oos.writeObject(value);
            }
        }

        public <T> T readValue(File file, TypeReference<T> type) 
                throws IOException {
            try (ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(file))) {
                return type.getType().cast(ois.readObject());
            } catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
        }
    }

    // Type reference for generic type information
    public static class TypeReference<T> {
        @SuppressWarnings("unchecked")
        public Class<T> getType() {
            return (Class<T>) Object.class;
        }
    }

    // Example usage
    public static void main(String[] args) {
        // Configure and create cache
        AdvancedCache<String, String> cache = new AdvancedCache<>(
            new CacheConfig.Builder<String, String>()
                .evictionPolicy(new LRUEvictionPolicy())
                .persistence(new FilePersistence<>(
                    Paths.get("cache-data")))
                .loader(key -> {
                    // Simulate loading from external source
                    Thread.sleep(100);
                    return "Value for " + key;
                })
                .ttl(Duration.ofMinutes(30))
                .maxSize(1000)
                .build()
        );

        try {
            // Put some values
            cache.put("key1", "value1");
            cache.put("key2", "value2");
            cache.put("key3", "value3");

            // Get values
            System.out.println(cache.get("key1")); // From cache
            System.out.println(cache.get("key4")); // Will load
            
            // Wait to see metrics
            Thread.sleep(70000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cache.shutdown();
        }
    }
}