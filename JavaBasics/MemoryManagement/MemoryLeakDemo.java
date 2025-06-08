package MemoryManagement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.WeakHashMap;

/**
 * Demonstrates common memory leak scenarios in Java and how to prevent them.
 * Memory leaks in Java occur when objects are still referenced but no longer needed,
 * preventing garbage collection from reclaiming the memory.
 */
public class MemoryLeakDemo {

    // Static collection that can cause memory leaks if not managed properly
    private static final List<Object> staticCollection = new ArrayList<>();
    
    public static void demonstrate() {
        System.out.println("Memory Leaks in Java and how to prevent them");
        
        // Common memory leak demonstrations
        demonstrateStaticFieldLeak();
        demonstrateUnclosedResources();
        demonstrateInnerClassLeak();
        demonstrateWeakHashMapSolution();
        demonstrateCachingIssues();
    }
    
    private static void demonstrateStaticFieldLeak() {
        System.out.println("\n--- 1. Static Collections Memory Leak ---");
        System.out.println("Problem: Static collections hold references to objects forever");
        
        // This approach can cause a memory leak
        System.out.println("Adding 10 objects to static collection");
        for (int i = 0; i < 10; i++) {
            LargeObject obj = new LargeObject(i);
            staticCollection.add(obj);
        }
        
        System.out.println("Static collection size: " + staticCollection.size());
        System.out.println("These objects will never be garbage collected as long as the class is loaded");
        
        System.out.println("\nSolution: Avoid static collections or ensure you remove objects when done");
        // To fix: staticCollection.clear();
    }
    
    private static void demonstrateUnclosedResources() {
        System.out.println("\n--- 2. Unclosed Resources Memory Leak ---");
        System.out.println("Problem: Not closing resources like streams, connections, etc.");
        
        // Proper way using try-with-resources (Java 7+)
        System.out.println("\nSolution: Use try-with-resources for automatic resource management");
        System.out.println("Example: try (FileInputStream fis = new FileInputStream(file)) { ... }");
        
        // NOTE: Not actually creating resources to avoid dependencies
        System.out.println("\nAlways close resources in a finally block if not using try-with-resources:");
        System.out.println("FileInputStream fis = null;");
        System.out.println("try {");
        System.out.println("    fis = new FileInputStream(file);");
        System.out.println("    // use the stream");
        System.out.println("} finally {");
        System.out.println("    if (fis != null) {");
        System.out.println("        try { fis.close(); } catch (IOException e) { }");
        System.out.println("    }");
        System.out.println("}");
    }
    
    private static void demonstrateInnerClassLeak() {
        System.out.println("\n--- 3. Non-Static Inner Class Memory Leak ---");
        System.out.println("Problem: Non-static inner classes hold reference to outer class");
        
        // Create an object with a non-static inner class
        OuterClass outer = new OuterClass();
        Object listener = outer.createListener();
        
        System.out.println("Non-static inner class created");
        System.out.println("Inner class retains reference to outer even if only inner class is needed");
        
        System.out.println("\nSolution: Use static inner classes when outer class reference isn't needed");
        Object staticListener = OuterClass.createStaticListener();
        System.out.println("Static inner class created - doesn't reference the outer class");
    }
    
    private static void demonstrateWeakHashMapSolution() {
        System.out.println("\n--- 4. HashMap vs WeakHashMap ---");
        System.out.println("Problem: HashMaps retain strong references to keys and values");
        
        // Create objects and maps
        Map<Key, String> regularMap = new HashMap<>();
        Map<Key, String> weakMap = new WeakHashMap<>();
        
        // Create keys
        Key key1 = new Key("Regular Map Key");
        Key key2 = new Key("Weak Map Key");
        
        // Add to maps
        regularMap.put(key1, "Regular Map Value");
        weakMap.put(key2, "Weak Map Value");
        
        System.out.println("Before nullifying keys - Regular map size: " + regularMap.size() + 
                         ", Weak map size: " + weakMap.size());
        
        // Remove strong references to keys
        key1 = null;
        key2 = null;
        
        // Force garbage collection
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println("After nullifying keys and GC - Regular map size: " + regularMap.size() + 
                         ", Weak map size: " + weakMap.size());
                         
        System.out.println("\nSolution: Use WeakHashMap when you want map entries removed when keys are not");
        System.out.println("referenced elsewhere, such as for caches");
    }
    
    private static void demonstrateCachingIssues() {
        System.out.println("\n--- 5. Caching-Related Memory Leaks ---");
        System.out.println("Problem: Unbounded caches that grow indefinitely");
        
        System.out.println("\nSolution approaches:");
        System.out.println("1. Use time-based expiration (e.g., Guava Cache or Caffeine)");
        System.out.println("   Cache<Key, Graph> cache = CacheBuilder.newBuilder()");
        System.out.println("       .expireAfterWrite(10, TimeUnit.MINUTES)");
        System.out.println("       .build();");
        
        System.out.println("\n2. Use size-bounded caches with eviction policies");
        System.out.println("   Cache<Key, Graph> cache = CacheBuilder.newBuilder()");
        System.out.println("       .maximumSize(1000)");
        System.out.println("       .build();");
        
        System.out.println("\n3. Use WeakHashMap or SoftReference for memory-sensitive caches");
        System.out.println("   Map<Key, SoftReference<Value>> cache = new HashMap<>();");
    }
    
    // A class to demonstrate memory consumption
    static class LargeObject {
        private final int id;
        private final byte[] memory = new byte[1024 * 100]; // 100KB per object
        
        public LargeObject(int id) {
            this.id = id;
        }
        
        @Override
        public String toString() {
            return "LargeObject [id=" + id + "]";
        }
    }
    
    // Class to demonstrate inner class memory leak
    static class OuterClass {
        private final byte[] data = new byte[1024 * 100]; // 100KB
        
        // Non-static inner class holds implicit reference to outer class
        public class InnerListener {
            public void onEvent() {
                System.out.println("Inner listener called, has access to OuterClass");
            }
        }
        
        // Static inner class doesn't hold reference to outer class
        public static class StaticInnerListener {
            public void onEvent() {
                System.out.println("Static inner listener called, no access to OuterClass");
            }
        }
        
        public Object createListener() {
            return new InnerListener();
        }
        
        public static Object createStaticListener() {
            return new StaticInnerListener();
        }
    }
    
    // Key class for map examples
    static class Key {
        private final String name;
        
        public Key(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return "Key [name=" + name + "]";
        }
    }
}