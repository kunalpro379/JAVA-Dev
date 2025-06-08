package concurrentpatterns;

import java.util.concurrent.*;
import java.util.*;
import java.util.function.Function;

// Thread-safe Singleton using Double-Checked Locking
class ThreadSafeSingleton {
    private static volatile ThreadSafeSingleton instance;
    private int value;

    private ThreadSafeSingleton() {
        value = 0;
    }

    public static ThreadSafeSingleton getInstance() {
        if (instance == null) {
            synchronized (ThreadSafeSingleton.class) {
                if (instance == null) {
                    instance = new ThreadSafeSingleton();
                }
            }
        }
        return instance;
    }

    public synchronized void increment() {
        value++;
    }

    public synchronized int getValue() {
        return value;
    }
}

// Active Object Pattern
interface Command {
    void execute();
}

class ActiveObject {
    private final BlockingQueue<Command> queue;
    private final Thread thread;
    private volatile boolean isRunning;

    public ActiveObject() {
        this.queue = new LinkedBlockingQueue<>();
        this.isRunning = true;
        this.thread = new Thread(this::processCommands);
        this.thread.start();
    }

    public void enqueue(Command command) {
        queue.offer(command);
    }

    private void processCommands() {
        while (isRunning) {
            try {
                Command command = queue.poll(1, TimeUnit.SECONDS);
                if (command != null) {
                    command.execute();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void shutdown() {
        isRunning = false;
        thread.interrupt();
    }
}

// Producer-Consumer Pattern with Bounded Buffer
class BoundedBuffer<T> {
    private final Queue<T> queue;
    private final int capacity;
    private final Object lock = new Object();

    public BoundedBuffer(int capacity) {
        this.capacity = capacity;
        this.queue = new LinkedList<>();
    }

    public void put(T item) throws InterruptedException {
        synchronized (lock) {
            while (queue.size() == capacity) {
                lock.wait();
            }
            queue.offer(item);
            lock.notifyAll();
        }
    }

    public T take() throws InterruptedException {
        synchronized (lock) {
            while (queue.isEmpty()) {
                lock.wait();
            }
            T item = queue.poll();
            lock.notifyAll();
            return item;
        }
    }
}

// Read-Write Lock Pattern
class ReadWriteLock {
    private int readers = 0;
    private int writers = 0;
    private int writeRequests = 0;

    public synchronized void lockRead() throws InterruptedException {
        while (writers > 0 || writeRequests > 0) {
            wait();
        }
        readers++;
    }

    public synchronized void unlockRead() {
        readers--;
        notifyAll();
    }

    public synchronized void lockWrite() throws InterruptedException {
        writeRequests++;
        while (readers > 0 || writers > 0) {
            wait();
        }
        writeRequests--;
        writers++;
    }

    public synchronized void unlockWrite() {
        writers--;
        notifyAll();
    }
}

// Thread Pool Pattern
class CustomThreadPool {
    private final BlockingQueue<Runnable> taskQueue;
    private final List<WorkerThread> threads;
    private volatile boolean isShutdown;

    public CustomThreadPool(int numThreads) {
        taskQueue = new LinkedBlockingQueue<>();
        threads = new ArrayList<>(numThreads);
        isShutdown = false;

        for (int i = 0; i < numThreads; i++) {
            WorkerThread thread = new WorkerThread();
            thread.start();
            threads.add(thread);
        }
    }

    public void execute(Runnable task) {
        if (!isShutdown) {
            taskQueue.offer(task);
        }
    }

    public void shutdown() {
        isShutdown = true;
        for (WorkerThread thread : threads) {
            thread.interrupt();
        }
    }

    private class WorkerThread extends Thread {
        @Override
        public void run() {
            while (!isShutdown) {
                try {
                    Runnable task = taskQueue.poll(1, TimeUnit.SECONDS);
                    if (task != null) {
                        task.execute();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}

// Future Pattern
class Future<T> {
    private T result;
    private boolean isDone;
    private final Object lock = new Object();

    public void setResult(T result) {
        synchronized (lock) {
            this.result = result;
            isDone = true;
            lock.notifyAll();
        }
    }

    public T get() throws InterruptedException {
        synchronized (lock) {
            while (!isDone) {
                lock.wait();
            }
            return result;
        }
    }

    public boolean isDone() {
        return isDone;
    }
}

// Object Pool Pattern with Thread Safety
class ObjectPool<T> {
    private final Queue<T> pool;
    private final Semaphore semaphore;
    private final Function<Void, T> factory;

    public ObjectPool(int size, Function<Void, T> factory) {
        this.pool = new ConcurrentLinkedQueue<>();
        this.semaphore = new Semaphore(size);
        this.factory = factory;

        for (int i = 0; i < size; i++) {
            pool.offer(factory.apply(null));
        }
    }

    public T acquire() throws InterruptedException {
        semaphore.acquire();
        T item = pool.poll();
        if (item == null) {
            item = factory.apply(null);
        }
        return item;
    }

    public void release(T item) {
        pool.offer(item);
        semaphore.release();
    }
}

public class ConcurrentPatternsDemo {
    public static void main(String[] args) throws InterruptedException {
        // Demonstrate Thread-safe Singleton
        System.out.println("Testing Thread-safe Singleton:");
        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                ThreadSafeSingleton.getInstance().increment();
            });
        }

        // Demonstrate Active Object
        System.out.println("\nTesting Active Object Pattern:");
        ActiveObject activeObject = new ActiveObject();
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            activeObject.enqueue(() -> 
                System.out.println("Executing task " + taskId)
            );
        }

        // Demonstrate Bounded Buffer
        System.out.println("\nTesting Bounded Buffer:");
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(5);
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    buffer.put(i);
                    System.out.println("Produced: " + i);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    int value = buffer.take();
                    System.out.println("Consumed: " + value);
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();

        // Demonstrate Object Pool
        System.out.println("\nTesting Object Pool:");
        ObjectPool<StringBuilder> pool = new ObjectPool<>(2, 
            unused -> new StringBuilder());

        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    StringBuilder sb = pool.acquire();
                    System.out.println("Using StringBuilder: " + 
                        System.identityHashCode(sb));
                    Thread.sleep(100);
                    pool.release(sb);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Clean up
        Thread.sleep(2000);
        activeObject.shutdown();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
        
        System.out.println("\nFinal singleton value: " + 
            ThreadSafeSingleton.getInstance().getValue());
    }
}