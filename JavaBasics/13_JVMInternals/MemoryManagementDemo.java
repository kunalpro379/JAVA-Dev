package jvminternals;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServer;
import java.lang.ref.*;

class LargeObject {
    private byte[] data;
    private String id;

    public LargeObject(int sizeInMB, String id) {
        this.data = new byte[sizeInMB * 1024 * 1024]; // Size in MB
        this.id = id;
    }

    @Override
    protected void finalize() {
        System.out.println("Finalizing LargeObject " + id);
    }
}

class MemoryLeakExample {
    private static final List<byte[]> leakyList = new ArrayList<>();

    public static void simulateMemoryLeak() {
        try {
            while (true) {
                byte[] data = new byte[1024 * 1024]; // 1MB
                leakyList.add(data);
                Thread.sleep(100);
            }
        } catch (OutOfMemoryError | InterruptedException e) {
            System.out.println("Memory leak simulation stopped: " + e.getMessage());
        }
    }
}

public class MemoryManagementDemo {
    // Demonstrate different types of references
    private static void demonstrateReferences() {
        // Strong reference
        LargeObject strongRef = new LargeObject(1, "Strong");

        // Soft reference (memory-sensitive)
        SoftReference<LargeObject> softRef = new SoftReference<>(
            new LargeObject(1, "Soft")
        );

        // Weak reference (GC-sensitive)
        WeakReference<LargeObject> weakRef = new WeakReference<>(
            new LargeObject(1, "Weak")
        );

        // Phantom reference (pre-GC notification)
        ReferenceQueue<LargeObject> refQueue = new ReferenceQueue<>();
        PhantomReference<LargeObject> phantomRef = new PhantomReference<>(
            new LargeObject(1, "Phantom"),
            refQueue
        );

        // Force garbage collection
        System.gc();
        
        // Check references
        System.out.println("Strong Reference: " + (strongRef != null));
        System.out.println("Soft Reference: " + (softRef.get() != null));
        System.out.println("Weak Reference: " + (weakRef.get() != null));
        System.out.println("Phantom Reference in queue: " + (refQueue.poll() != null));
    }

    // Demonstrate memory monitoring
    private static void monitorMemory() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        
        System.out.println("\nMemory Usage:");
        
        // Heap memory usage
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        System.out.println("\nHEAP MEMORY:");
        System.out.println("Initial: " + formatSize(heapUsage.getInit()));
        System.out.println("Used: " + formatSize(heapUsage.getUsed()));
        System.out.println("Committed: " + formatSize(heapUsage.getCommitted()));
        System.out.println("Max: " + formatSize(heapUsage.getMax()));

        // Non-heap memory usage
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        System.out.println("\nNON-HEAP MEMORY:");
        System.out.println("Initial: " + formatSize(nonHeapUsage.getInit()));
        System.out.println("Used: " + formatSize(nonHeapUsage.getUsed()));
        System.out.println("Committed: " + formatSize(nonHeapUsage.getCommitted()));
        System.out.println("Max: " + formatSize(nonHeapUsage.getMax()));

        // Memory pools
        System.out.println("\nMEMORY POOLS:");
        for (MemoryPoolMXBean pool : pools) {
            System.out.println("\nPool: " + pool.getName());
            MemoryUsage poolUsage = pool.getUsage();
            System.out.println("Used: " + formatSize(poolUsage.getUsed()));
            System.out.println("Max: " + formatSize(poolUsage.getMax()));
            System.out.println("Type: " + pool.getType());
        }
    }

    // Demonstrate generation sizes and GC
    private static void demonstrateGarbageCollection() {
        System.out.println("\nGarbage Collection Stats:");
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.println("\nGC Name: " + gc.getName());
            System.out.println("Collection count: " + gc.getCollectionCount());
            System.out.println("Collection time: " + 
                TimeUnit.MILLISECONDS.toSeconds(gc.getCollectionTime()) + " seconds");
        }
    }

    // Demonstrate object allocation and promotion
    private static void demonstrateObjectLifecycle() {
        System.out.println("\nDemonstrating Object Lifecycle:");
        
        // Young Generation allocation
        List<LargeObject> objects = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            objects.add(new LargeObject(1, "Gen" + i));
            System.out.println("Allocated object " + i);
        }

        // Force Minor GC
        System.out.println("\nForcing Minor GC...");
        System.gc();
        
        // Clear some objects to demonstrate garbage collection
        objects.subList(0, 3).clear();
        
        // Force Major GC
        System.out.println("\nForcing Major GC...");
        System.gc();
        System.runFinalization();
    }

    // Demonstrate memory leak detection
    private static void demonstrateMemoryLeak() {
        System.out.println("\nDemonstrating Memory Leak Detection:");
        
        Thread leakThread = new Thread(() -> {
            try {
                MemoryLeakExample.simulateMemoryLeak();
            } catch (OutOfMemoryError e) {
                System.out.println("Memory leak detected: " + e.getMessage());
            }
        });

        leakThread.start();
        
        // Monitor memory usage while leak is occurring
        try {
            for (int i = 0; i < 5; i++) {
                monitorMemory();
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        leakThread.interrupt();
    }

    private static String formatSize(long bytes) {
        if (bytes < 0) return "undefined";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public static void main(String[] args) {
        // Set initial JVM arguments for demonstration
        // -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xmx512m -Xms512m
        System.out.println("JVM Memory Management Demonstration");
        System.out.println("==================================");

        // Demonstrate different types of references
        demonstrateReferences();

        // Monitor current memory usage
        monitorMemory();

        // Demonstrate garbage collection
        demonstrateGarbageCollection();

        // Demonstrate object lifecycle
        demonstrateObjectLifecycle();

        // Demonstrate memory leak detection
        demonstrateMemoryLeak();

        // Final memory status
        System.out.println("\nFinal Memory Status:");
        monitorMemory();
    }
}