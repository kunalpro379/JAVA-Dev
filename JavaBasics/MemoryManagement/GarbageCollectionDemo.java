package MemoryManagement;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.HashMap;
import java.lang.ref.WeakReference;
import java.lang.ref.SoftReference;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

/**
 * Demonstrates different aspects of garbage collection in Java including:
 * 1. Garbage Collection basics
 * 2. Different reference types (Strong, Soft, Weak, Phantom)
 * 3. GC algorithms and generations
 */
public class GarbageCollectionDemo {

    public static void demonstrate() {
        System.out.println("Garbage Collection in Java controls the lifecycle of objects");
        
        // Basic garbage collection demo
        demonstrateBasicGC();
        
        // Reference types demo
        demonstrateReferenceTypes();
        
        // GC algorithms and generations
        demonstrateGCAlgorithms();
    }
    
    private static void demonstrateBasicGC() {
        System.out.println("\n--- Basic Garbage Collection ---");
        System.out.println("Objects become eligible for garbage collection when no longer reachable");
        
        // Create objects
        System.out.println("Creating objects...");
        Object obj1 = new Object();
        Object obj2 = new Object();
        
        System.out.println("obj1 and obj2 are strongly referenced and not eligible for GC");
        
        // Make obj1 eligible for garbage collection
        System.out.println("Setting obj1 to null, making it eligible for GC");
        obj1 = null;
        
        // Request garbage collection
        System.out.println("Requesting garbage collection...");
        System.gc();
        
        System.out.println("After GC request: obj1 may be collected, obj2 is still strongly referenced");
    }
    
    private static void demonstrateReferenceTypes() {
        System.out.println("\n--- Reference Types ---");
        
        // 1. Strong References
        System.out.println("\n1. Strong References:");
        System.out.println("   - Default reference type");
        System.out.println("   - Objects with strong references are not eligible for GC");
        
        Object strongRef = new Object();
        System.out.println("   Created a strong reference: Object won't be collected while reference exists");
        
        // 2. Soft References
        System.out.println("\n2. Soft References:");
        System.out.println("   - Objects with only soft references are collected before OutOfMemoryError");
        System.out.println("   - Used for memory-sensitive caches");
        
        Object softObject = new Object();
        SoftReference<Object> softRef = new SoftReference<>(softObject);
        
        // Remove strong reference
        softObject = null;
        
        System.out.println("   Object accessible through softRef.get(): " + (softRef.get() != null));
        System.out.println("   JVM may clear soft references when memory is tight");
        
        // 3. Weak References
        System.out.println("\n3. Weak References:");
        System.out.println("   - Objects with only weak references are collected in next GC cycle");
        System.out.println("   - Used when you want GC to determine object lifecycle");
        
        Object weakObject = new Object();
        WeakReference<Object> weakRef = new WeakReference<>(weakObject);
        
        // Remove strong reference
        weakObject = null;
        
        System.out.println("   Object accessible through weakRef.get() before GC: " + (weakRef.get() != null));
        System.gc(); // Request GC
        System.out.println("   Object accessible through weakRef.get() after GC: " + (weakRef.get() != null));
        
        // WeakHashMap example
        System.out.println("\n   WeakHashMap Example:");
        WeakHashMap<Key, String> weakHashMap = new WeakHashMap<>();
        HashMap<Key, String> regularMap = new HashMap<>();
        
        Key key1 = new Key("key1");
        Key key2 = new Key("key2");
        
        weakHashMap.put(key1, "Weak mapping");
        regularMap.put(key2, "Regular mapping");
        
        System.out.println("   Maps before nullifying keys - WeakHashMap size: " + weakHashMap.size() 
                        + ", Regular HashMap size: " + regularMap.size());
        
        key1 = null; // Remove strong reference to key1
        key2 = null; // Remove strong reference to key2
        
        System.gc(); // Request GC
        
        // Sleep briefly to allow GC to work
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println("   Maps after nullifying keys - WeakHashMap size: " + weakHashMap.size() 
                        + ", Regular HashMap size: " + regularMap.size());

        // 4. Phantom References
        System.out.println("\n4. Phantom References:");
        System.out.println("   - Cannot be used to retrieve the object");
        System.out.println("   - Used for scheduling post-mortem cleanup actions");
        
        ReferenceQueue<Object> refQueue = new ReferenceQueue<>();
        Object phantomObject = new Object();
        PhantomReference<Object> phantomRef = new PhantomReference<>(phantomObject, refQueue);
        
        System.out.println("   Phantom reference created. Can't access object through phantomRef.get(): " 
                        + (phantomRef.get() == null));
        
        // Remove strong reference to make object eligible for GC
        phantomObject = null;
        
        System.gc(); // Request GC
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println("   After GC, check if reference is enqueued: " + phantomRef.isEnqueued());
        System.out.println("   Phantom references are used with a ReferenceQueue for cleanup operations");
    }
    
    private static void demonstrateGCAlgorithms() {
        System.out.println("\n--- Garbage Collection Algorithms and Generations ---");
        System.out.println("JVM uses different GC algorithms and memory generations:");
        
        System.out.println("\n1. Memory Generations:");
        System.out.println("   - Young Generation (Eden, Survivor spaces)");
        System.out.println("   - Old Generation");
        System.out.println("   - Metaspace (Class metadata)");
        
        System.out.println("\n2. Common GC Algorithms:");
        System.out.println("   - Serial GC: Single-threaded, stop-the-world");
        System.out.println("   - Parallel GC: Multi-threaded, stop-the-world");
        System.out.println("   - Concurrent Mark Sweep (CMS): Reduces pause times");
        System.out.println("   - G1 GC: Garbage First, balance between pause times and throughput");
        System.out.println("   - ZGC: Low latency GC for large heaps");
        System.out.println("   - Shenandoah: Low pause times even with large heaps");
        
        System.out.println("\nCurrent JVM uses GC algorithm set by JVM flags like:");
        System.out.println("   -XX:+UseSerialGC, -XX:+UseParallelGC, -XX:+UseConcMarkSweepGC, -XX:+UseG1GC, etc.");
        
        // Create some short-lived objects to demonstrate young generation collection
        System.out.println("\nCreating many short-lived objects to trigger young generation collection...");
        for (int i = 0; i < 1000000; i++) {
            Object obj = new Object(); // These objects quickly become garbage
        }
        
        System.out.println("Young generation objects have been created and abandoned");
    }
    
    // Simple class to use as keys in map examples
    static class Key {
        private String id;
        
        public Key(String id) {
            this.id = id;
        }
        
        @Override
        public String toString() {
            return "Key[" + id + "]";
        }
    }
}