Redis?
Redis stores data in RAM,perfect for high availability systems.
in memory database used for: 
Caching (super-fast reads)
Distributed locking
Pub/Sub messaging
Session storage

Why caching? (Spring Boot + Redis)
Caching improves performance by:
Reducing database load
Improving response time
Increasing availability

In Spring Boot-->
Client → Cache (Redis) → DB
If the data exists in Redis → return it immediately
If not → fetch from DB → store in Redis → return result


HOw to IMplement?
```java
@SpringBootApplication
@EnableCaching
public class DemoApplication {

}
```
Use cache in service
```java
@Service
public class ProductService {

    @Cacheable(value = "products", key = "#productId")
    public Product getProduct(int productId){
        System.out.println("Fetching from DB...");
        return productRepository.findById(productId).orElseThrow();
    }
}
```
Problem: Cache Stampede
Imagine 1 million users request same product at same time.
If cache expires, all requests hit DB → DB crashes.

Solution = Distributed Locking
Only one request should rebuild cache; others should wait.

###### Distributed Locking using Redis
Redis provides distributed locks using SETNX (set-if-not-exist).
Why use locks?
Prevent race conditions
Avoid duplicate processing
Manage high traffic safely

Two servers (A and B) try to update the same product price at same time
Without lock   → last write wins → inconsistent data
With lock      → only one server updates → safe

###### Implementing Distributed Lock using Spring + Redis

1. Add library (Redisson recommended)
2. Use lock in service
```java
   @Service
public class InventoryService {

    @Autowired
    private RedissonClient redissonClient;
    public void updateStock(String productId) {
        RLock lock = redissonClient.getLock("lock:product:" + productId);
        try {
            lock.lock(); // Acquire distributed lock

            // critical section
            System.out.println("Updating stock safely...");
            // update db logic

        } finally {
            lock.unlock();
        }
    }
}
```
No double update
No inconsistent data
Safe in microservices
Works even if multiple instances of app running

##### Caching + Locking together
```java
Try reading from Redis cache  
    If exists → return  
    If not → acquire lock  
        Check again if cache filled  
        If still empty → query DB + write to cache  
    Release lock  
Return result
```
Ex:-
```java
public Product getProduct(int productId){
    String key = "product:" + productId;

    //check cache
    Product cached = redisTemplate.opsForValue().get(key);
    if(cached != null) return cached;
    RLock lock = redissonClient.getLock("lock:" + key);
    try {
        lock.lock();

        // Check again after acquiring lock
        cached = redisTemplate.opsForValue().get(key);
        if (cached != null) return cached;
        // Fetch from DB
        Product product = repository.findById(productId).orElseThrow();
        // Save to cache with TTL
        redisTemplate.opsForValue().set(key, product, 10, TimeUnit.MINUTES);

        return product;

    } finally {
        lock.unlock();
    }
}
```

####### High Availability Achieved
Redis Caching
Fast responses
Less DB load
No downtimes

Redis Distributed Locks
Safe concurrency
Prevent cache stampede
Ensures correctness in distributed systems

