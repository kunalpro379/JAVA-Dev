package MemoryManagement;

/**
 * This class demonstrates various Java Memory Management concepts.
 * Run this class to see demonstrations of:
 * - JVM Memory Structure (Heap, Stack, Metaspace)
 * - Object Lifecycles
 * - Garbage Collection mechanisms
 * - Memory Leaks and their prevention
 * - Memory Optimization techniques
 * - Direct memory management with java.nio.DirectByteBuffer
 */
public class MemoryManagementDemo {
    
    public static void main(String[] args) {
        System.out.println("===== Java Memory Management Demonstrations =====\n");
        
        // Run the demonstrations
        System.out.println("\n----- JVM Memory Structure -----");
        JVMMemoryStructure.demonstrate();
        
        System.out.println("\n----- Object Lifecycle -----");
        ObjectLifecycle.demonstrate();
        
        System.out.println("\n----- Garbage Collection -----");
        GarbageCollectionDemo.demonstrate();
        
        System.out.println("\n----- Memory Leaks -----");
        MemoryLeakDemo.demonstrate();
        
        System.out.println("\n----- Memory Optimization -----");
        MemoryOptimization.demonstrate();
        
        System.out.println("\n----- Off-Heap Memory -----");
        DirectMemoryDemo.demonstrate();
        
        System.out.println("\n===== All demonstrations completed =====");
    }
}