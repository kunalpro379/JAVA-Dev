package designpatterns;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

// Generic interface for cache operations
interface Cache<K, V> {
    Optional<V> get(K key);
    void put(K key, V value);
    void remove(K key);
    void clear();
    int size();
}

// Abstract factory for creating different types of caches
interface CacheFactory {
    <K, V> Cache<K, V> createCache(CacheType type, int capacity);
    enum CacheType { LRU, FIFO, TIMED }
}

// Singleton pattern for cache factory
class CacheManager implements CacheFactory {
    private static final CacheManager INSTANCE = new CacheManager();
    
    private CacheManager() {}
    
    public static CacheManager getInstance() {
        return INSTANCE;
    }

    @Override
    public <K, V> Cache<K, V> createCache(CacheType type, int capacity) {
        switch (type) {
            case LRU:
                return new LRUCache<>(capacity);
            case FIFO:
                return new FIFOCache<>(capacity);
            case TIMED:
                return new TimedCache<>(capacity);
            default:
                throw new IllegalArgumentException("Unknown cache type: " + type);
        }
    }
}

// Decorator pattern base class
abstract class CacheDecorator<K, V> implements Cache<K, V> {
    protected final Cache<K, V> cache;
    
    protected CacheDecorator(Cache<K, V> cache) {
        this.cache = cache;
    }
    
    @Override
    public Optional<V> get(K key) {
        return cache.get(key);
    }
    
    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }
    
    @Override
    public void remove(K key) {
        cache.remove(key);
    }
    
    @Override
    public void clear() {
        cache.clear();
    }
    
    @Override
    public int size() {
        return cache.size();
    }
}

// Observer pattern interfaces
interface CacheListener<K, V> {
    void onPut(K key, V value);
    void onRemove(K key);
    void onClear();
}

// Strategy pattern for eviction policies
interface EvictionPolicy<K> {
    K selectEvictionCandidate();
    void recordAccess(K key);
    void remove(K key);
}

// LRU implementation using Strategy pattern
class LRUEvictionPolicy<K> implements EvictionPolicy<K> {
    private final LinkedHashMap<K, Long> accessOrder = new LinkedHashMap<>();
    
    @Override
    public K selectEvictionCandidate() {
        return accessOrder.keySet().iterator().next();
    }
    
    @Override
    public void recordAccess(K key) {
        accessOrder.remove(key);
        accessOrder.put(key, System.nanoTime());
    }
    
    @Override
    public void remove(K key) {
        accessOrder.remove(key);
    }
}

// Base cache implementation with template method pattern
abstract class BaseCache<K, V> implements Cache<K, V> {
    protected final Map<K, V> storage;
    protected final int capacity;
    protected final List<CacheListener<K, V>> listeners;
    
    protected BaseCache(int capacity) {
        this.capacity = capacity;
        this.storage = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
    }
    
    public void addListener(CacheListener<K, V> listener) {
        listeners.add(listener);
    }
    
    public void removeListener(CacheListener<K, V> listener) {
        listeners.remove(listener);
    }
    
    protected void notifyPut(K key, V value) {
        listeners.forEach(listener -> listener.onPut(key, value));
    }
    
    protected void notifyRemove(K key) {
        listeners.forEach(listener -> listener.onRemove(key));
    }
    
    protected void notifyClear() {
        listeners.forEach(CacheListener::onClear);
    }
}

// LRU Cache implementation
class LRUCache<K, V> extends BaseCache<K, V> {
    private final EvictionPolicy<K> evictionPolicy;
    
    public LRUCache(int capacity) {
        super(capacity);
        this.evictionPolicy = new LRUEvictionPolicy<>();
    }
    
    @Override
    public Optional<V> get(K key) {
        V value = storage.get(key);
        if (value != null) {
            evictionPolicy.recordAccess(key);
        }
        return Optional.ofNullable(value);
    }
    
    @Override
    public void put(K key, V value) {
        if (storage.size() >= capacity && !storage.containsKey(key)) {
            K evicted = evictionPolicy.selectEvictionCandidate();
            remove(evicted);
        }
        
        storage.put(key, value);
        evictionPolicy.recordAccess(key);
        notifyPut(key, value);
    }
    
    @Override
    public void remove(K key) {
        storage.remove(key);
        evictionPolicy.remove(key);
        notifyRemove(key);
    }
    
    @Override
    public void clear() {
        storage.clear();
        notifyClear();
    }
    
    @Override
    public int size() {
        return storage.size();
    }
}

// FIFO Cache implementation
class FIFOCache<K, V> extends BaseCache<K, V> {
    private final Queue<K> insertionOrder = new LinkedList<>();
    
    public FIFOCache(int capacity) {
        super(capacity);
    }
    
    @Override
    public Optional<V> get(K key) {
        return Optional.ofNullable(storage.get(key));
    }
    
    @Override
    public void put(K key, V value) {
        if (storage.size() >= capacity && !storage.containsKey(key)) {
            K evicted = insertionOrder.poll();
            if (evicted != null) {
                remove(evicted);
            }
        }
        
        storage.put(key, value);
        insertionOrder.offer(key);
        notifyPut(key, value);
    }
    
    @Override
    public void remove(K key) {
        storage.remove(key);
        insertionOrder.remove(key);
        notifyRemove(key);
    }
    
    @Override
    public void clear() {
        storage.clear();
        insertionOrder.clear();
        notifyClear();
    }
    
    @Override
    public int size() {
        return storage.size();
    }
}

// Timed Cache implementation with entry expiration
class TimedCache<K, V> extends BaseCache<K, V> {
    private final Map<K, Long> expirationTimes;
    private final long defaultTTLMillis = 5000; // 5 seconds
    
    public TimedCache(int capacity) {
        super(capacity);
        this.expirationTimes = new ConcurrentHashMap<>();
        startCleanupTask();
    }
    
    private void startCleanupTask() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::removeExpiredEntries, 1, 1, TimeUnit.SECONDS);
    }
    
    private void removeExpiredEntries() {
        long now = System.currentTimeMillis();
        expirationTimes.entrySet().removeIf(entry -> {
            if (entry.getValue() < now) {
                storage.remove(entry.getKey());
                notifyRemove(entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    @Override
    public Optional<V> get(K key) {
        Long expirationTime = expirationTimes.get(key);
        if (expirationTime == null || expirationTime < System.currentTimeMillis()) {
            remove(key);
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(key));
    }
    
    @Override
    public void put(K key, V value) {
        if (storage.size() >= capacity && !storage.containsKey(key)) {
            K oldest = expirationTimes.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
            if (oldest != null) {
                remove(oldest);
            }
        }
        
        storage.put(key, value);
        expirationTimes.put(key, System.currentTimeMillis() + defaultTTLMillis);
        notifyPut(key, value);
    }
    
    @Override
    public void remove(K key) {
        storage.remove(key);
        expirationTimes.remove(key);
        notifyRemove(key);
    }
    
    @Override
    public void clear() {
        storage.clear();
        expirationTimes.clear();
        notifyClear();
    }
    
    @Override
    public int size() {
        return storage.size();
    }
}

// Example cache listener implementation
class LoggingCacheListener<K, V> implements CacheListener<K, V> {
    @Override
    public void onPut(K key, V value) {
        System.out.println("Cache put: " + key + " = " + value);
    }
    
    @Override
    public void onRemove(K key) {
        System.out.println("Cache remove: " + key);
    }
    
    @Override
    public void onClear() {
        System.out.println("Cache cleared");
    }
}

// Demonstration of usage
public class CacheSystem {
    public static void main(String[] args) throws InterruptedException {
        // Get cache factory instance
        CacheFactory factory = CacheManager.getInstance();
        
        // Create different types of caches
        Cache<String, Integer> lruCache = factory.createCache(CacheFactory.CacheType.LRU, 3);
        Cache<String, Integer> fifoCache = factory.createCache(CacheFactory.CacheType.FIFO, 3);
        Cache<String, Integer> timedCache = factory.createCache(CacheFactory.CacheType.TIMED, 3);
        
        // Add logging listener to LRU cache
        if (lruCache instanceof BaseCache) {
            ((BaseCache<String, Integer>) lruCache).addListener(new LoggingCacheListener<>());
        }
        
        System.out.println("Testing LRU Cache:");
        lruCache.put("A", 1);
        lruCache.put("B", 2);
        lruCache.put("C", 3);
        lruCache.get("A"); // Access A to make it most recently used
        lruCache.put("D", 4); // Should evict B
        System.out.println("LRU Cache size: " + lruCache.size());
        
        System.out.println("\nTesting FIFO Cache:");
        fifoCache.put("A", 1);
        fifoCache.put("B", 2);
        fifoCache.put("C", 3);
        fifoCache.get("A"); // Access won't affect order
        fifoCache.put("D", 4); // Should evict A
        System.out.println("FIFO Cache size: " + fifoCache.size());
        
        System.out.println("\nTesting Timed Cache:");
        timedCache.put("A", 1);
        timedCache.put("B", 2);
        System.out.println("Initial value of A: " + timedCache.get("A").orElse(null));
        
        // Wait for entries to expire
        Thread.sleep(6000);
        System.out.println("Value of A after expiration: " + timedCache.get("A").orElse(null));
    }
}