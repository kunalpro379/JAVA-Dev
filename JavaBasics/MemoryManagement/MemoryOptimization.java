package MemoryManagement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates various memory optimization techniques in Java.
 * This class covers strategies to make Java applications more memory-efficient.
 */
public class MemoryOptimization {

    public static void demonstrate() {
        System.out.println("Memory Optimization Techniques in Java");
        
        // Demonstrate various memory optimization techniques
        demonstrateObjectReuse();
        demonstrateCollectionSizing();
        demonstratePrimitiveVsWrapper();
        demonstrateStringPooling();
        demonstrateObjectCachingInternals();
        demonstrateJVMTuning();
    }
    
    private static void demonstrateObjectReuse() {
        System.out.println("\n--- 1. Object Reuse ---");
        System.out.println("Creating new objects repeatedly can lead to increased GC pressure");
        
        // Bad practice: creating new objects in a loop
        System.out.println("\nBad practice:");
        System.out.println("for (int i = 0; i < 1000; i++) {");
        System.out.println("    StringBuilder sb = new StringBuilder();");
        System.out.println("    sb.append(\"Processing item \").append(i);");
        System.out.println("    // process using sb");
        System.out.println("}");
        
        // Good practice: reusing objects
        System.out.println("\nGood practice (object reuse):");
        System.out.println("StringBuilder sb = new StringBuilder(100);  // Pre-allocate capacity");
        System.out.println("for (int i = 0; i < 1000; i++) {");
        System.out.println("    sb.setLength(0);  // Clear the buffer");
        System.out.println("    sb.append(\"Processing item \").append(i);");
        System.out.println("    // process using sb");
        System.out.println("}");
    }
    
    private static void demonstrateCollectionSizing() {
        System.out.println("\n--- 2. Collection Initial Capacity ---");
        System.out.println("Proper sizing of collections can reduce memory reallocation");
        
        // Demonstrate ArrayList capacity
        System.out.println("\nArrayList example:");
        System.out.println("// Bad practice (default size will cause multiple resizing)");
        System.out.println("List<Integer> list = new ArrayList<>();  // Default capacity of 10");
        System.out.println("// If you know you need to store 1000 elements:");
        System.out.println("// Good practice");
        System.out.println("List<Integer> sizedList = new ArrayList<>(1000);  // Pre-sized capacity");
        
        // Demonstrate HashMap capacity
        System.out.println("\nHashMap example:");
        System.out.println("// If you know you'll have around 1000 entries with 0.75 load factor:");
        System.out.println("Map<String, Data> map = new HashMap<>(1334);  // capacity = expected size / load factor");
        System.out.println("// Or explicitly set both:");
        System.out.println("Map<String, Data> customMap = new HashMap<>(1000, 0.8f);");
    }
    
    private static void demonstratePrimitiveVsWrapper() {
        System.out.println("\n--- 3. Primitive vs Wrapper Types ---");
        System.out.println("Primitive types require less memory than their wrapper equivalents");
        
        // Create arrays to demonstrate
        int size = 1000000;
        
        System.out.println("Creating array of " + size + " primitive ints");
        // Actual measurement
        long startMemory = getUsedMemory();
        int[] primitiveArray = new int[size];
        for (int i = 0; i < size; i++) {
            primitiveArray[i] = i;
        }
        long primitiveMemory = getUsedMemory() - startMemory;
        
        System.out.println("Creating array of " + size + " Integer objects");
        startMemory = getUsedMemory();
        Integer[] wrapperArray = new Integer[size];
        for (int i = 0; i < size; i++) {
            wrapperArray[i] = Integer.valueOf(i);
        }
        long wrapperMemory = getUsedMemory() - startMemory;
        
        System.out.println("Memory used by int array: ~" + primitiveMemory / (1024 * 1024) + " MB");
        System.out.println("Memory used by Integer array: ~" + wrapperMemory / (1024 * 1024) + " MB");
        System.out.println("Using primitive types when possible saves significant memory");
    }
    
    private static void demonstrateStringPooling() {
        System.out.println("\n--- 4. String Pooling ---");
        System.out.println("String literals are automatically pooled, but String objects are not");
        
        // String literals
        String literal1 = "Hello World";
        String literal2 = "Hello World";
        
        // String objects
        String object1 = new String("Hello World");
        String object2 = new String("Hello World");
        
        System.out.println("literal1 == literal2: " + (literal1 == literal2));
        System.out.println("object1 == object2: " + (object1 == object2));
        System.out.println("literal1 == object1: " + (literal1 == object1));
        
        // Interning
        String internedObject = object1.intern();
        System.out.println("literal1 == internedObject: " + (literal1 == internedObject));
        
        System.out.println("\nBest practice: Use string literals when possible");
        System.out.println("For dynamic strings, consider using .intern() if the same string value");
        System.out.println("is used many times (but be cautious not to fill the string pool)");
    }
    
    private static void demonstrateObjectCachingInternals() {
        System.out.println("\n--- 5. Object Caching Internals ---");
        System.out.println("Java caches certain objects internally, like Integer and String");
        
        // Integer caching
        Integer a = 127;
        Integer b = 127;
        System.out.println("Integer 127 == 127: " + (a == b) + " (cached, -128 to 127)");
        
        Integer c = 1000;
        Integer d = 1000;
        System.out.println("Integer 1000 == 1000: " + (c == d) + " (not cached)");
        
        // Boolean caching
        Boolean bool1 = Boolean.valueOf(true);
        Boolean bool2 = Boolean.valueOf(true);
        System.out.println("Boolean true == true: " + (bool1 == bool2) + " (always cached)");
        
        System.out.println("\nUnderstanding these internal caching mechanisms helps");
        System.out.println("when designing your own caching strategies.");
    }
    
    private static void demonstrateJVMTuning() {
        System.out.println("\n--- 6. JVM Tuning Parameters ---");
        System.out.println("The JVM can be tuned for different memory utilization goals");
        
        System.out.println("\nCommon JVM memory tuning flags:");
        System.out.println("-Xms<size>        Set initial Java heap size");
        System.out.println("-Xmx<size>        Set maximum Java heap size");
        System.out.println("-XX:MetaspaceSize=<size>  Set initial metaspace size");
        System.out.println("-XX:MaxMetaspaceSize=<size>  Set maximum metaspace size");
        
        System.out.println("\nGarbage Collector selection:");
        System.out.println("-XX:+UseSerialGC         Serial Garbage Collector (simple, small applications)");
        System.out.println("-XX:+UseParallelGC       Parallel Garbage Collector (throughput)");
        System.out.println("-XX:+UseConcMarkSweepGC  CMS Garbage Collector (low latency)");
        System.out.println("-XX:+UseG1GC             G1 Garbage Collector (balanced)");
        System.out.println("-XX:+UseZGC              Z Garbage Collector (very low pause times, large heaps)");
        System.out.println("-XX:+UseShenandoahGC     Shenandoah Garbage Collector (low pause)");
        
        System.out.println("\nExample for a server application:");
        System.out.println("java -Xms4g -Xmx4g -XX:+UseG1GC -jar myapp.jar");
    }
    
    private static long getUsedMemory() {
        System.gc(); // Request GC to get more accurate readings
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}