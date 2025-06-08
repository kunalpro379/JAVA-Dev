package MemoryManagement;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates the different memory areas in the JVM:
 * 1. Stack - stores method frames, primitive local variables, and object references
 * 2. Heap - stores all objects and arrays
 * 3. Metaspace - stores class metadata (replaced PermGen in Java 8+)
 */
public class JVMMemoryStructure {
    
    public static void demonstrate() {
        // Display current memory stats
        printMemoryStats();
        
        // Demonstrate Stack Memory
        demonstrateStackMemory();
        
        // Demonstrate Heap Memory
        demonstrateHeapMemory();
        
        // Demonstrate Metaspace
        demonstrateMetaspace();
    }
    
    private static void demonstrateStackMemory() {
        System.out.println("\n--- Stack Memory Demonstration ---");
        System.out.println("Stack stores: method calls, local primitive variables, and object references");
        
        // Primitive variables are stored directly on the stack
        int x = 10;
        double y = 20.5;
        
        // References to objects are stored on the stack
        // The actual objects are stored on the heap
        String message = "Hello, this reference is stored on stack, but the String object is on heap";
        
        // Method calls create new frames on the stack
        recursiveMethod(3);
        
        System.out.println("Stack memory is automatically managed as methods execute and return");
    }
    
    private static void recursiveMethod(int count) {
        // Each call creates a new stack frame with its own 'count' variable
        System.out.println("Stack frame " + count + " created");
        
        // Local variables are stored on the current stack frame
        int localVar = count * 10;
        
        if (count > 0) {
            // Recursive call adds another frame to the stack
            recursiveMethod(count - 1);
        }
        
        System.out.println("Stack frame " + count + " will be removed (count=" + localVar + ")");
        // When this method returns, its stack frame is automatically removed
    }
    
    private static void demonstrateHeapMemory() {
        System.out.println("\n--- Heap Memory Demonstration ---");
        System.out.println("Heap stores: all objects and arrays");
        
        System.out.println("Creating objects to allocate on the heap...");
        List<byte[]> memoryConsumers = new ArrayList<>();
        
        // Create several objects to fill heap memory
        for (int i = 0; i < 5; i++) {
            // Each byte array object is allocated on the heap
            byte[] bytes = new byte[1024 * 1024]; // 1MB
            memoryConsumers.add(bytes);
            System.out.println("Allocated 1MB on the heap, total: " + (i + 1) + "MB");
        }
        
        printMemoryStats();
        
        // Release references to allow garbage collection
        System.out.println("Releasing references to allow garbage collection");
        memoryConsumers.clear();
        System.gc(); // Request garbage collection
        
        printMemoryStats();
    }
    
    private static void demonstrateMetaspace() {
        System.out.println("\n--- Metaspace Demonstration ---");
        System.out.println("Metaspace stores: class metadata, method bytecode, constant pools");
        
        // Metaspace stores class definitions and metadata
        System.out.println("When classes are loaded, their definitions are stored in Metaspace");
        System.out.println("For example, this class (JVMMemoryStructure) and all its methods");
        System.out.println("have their definitions stored in Metaspace");
        
        // Note: Dynamically loading many classes would demonstrate Metaspace usage,
        // but that's complex for this demo
    }
    
    public static void printMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        
        System.out.println("\n----- Memory Statistics -----");
        System.out.println("Max Memory (heap): " + maxMemory + " MB");
        System.out.println("Total Memory (current heap): " + totalMemory + " MB");
        System.out.println("Free Memory (unused heap): " + freeMemory + " MB");
        System.out.println("Used Memory (used heap): " + usedMemory + " MB");
    }
}