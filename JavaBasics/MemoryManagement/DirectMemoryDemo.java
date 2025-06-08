package MemoryManagement;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates direct (off-heap) memory usage in Java using NIO ByteBuffer.
 * Direct ByteBuffers are allocated outside the Java heap in native memory.
 */
public class DirectMemoryDemo {

    public static void demonstrate() {
        System.out.println("Direct (Off-Heap) Memory Management in Java");
        
        // Show JVM memory before allocation
        JVMMemoryStructure.printMemoryStats();
        
        // Demonstrate Direct ByteBuffer allocation and usage
        demonstrateDirectByteBuffer();
        
        // Compare heap vs off-heap performance
        comparePerformance();
        
        // Demonstrate limitations and best practices
        demonstrateBestPractices();
    }
    
    private static void demonstrateDirectByteBuffer() {
        System.out.println("\n--- Direct ByteBuffer Basics ---");
        System.out.println("Direct ByteBuffers are allocated in native memory (outside Java heap)");
        
        // Allocate a 100MB direct buffer
        int bufferSize = 100 * 1024 * 1024; // 100MB
        System.out.println("Allocating a 100MB direct ByteBuffer...");
        
        try {
            ByteBuffer directBuffer = ByteBuffer.allocateDirect(bufferSize);
            System.out.println("Direct buffer allocated: capacity = " + 
                             directBuffer.capacity() / (1024 * 1024) + "MB");
            
            // Write some data to the buffer
            System.out.println("Writing data to direct buffer...");
            for (int i = 0; i < 1000; i++) {
                directBuffer.putInt(i, i);
            }
            
            // Read data from the buffer
            directBuffer.flip();
            System.out.println("Reading first 5 integers from direct buffer:");
            for (int i = 0; i < 5; i++) {
                System.out.println("  Value at position " + i + ": " + directBuffer.getInt());
            }
            
            System.out.println("\nDirect buffer advantages:");
            System.out.println("1. Potentially better performance for I/O operations");
            System.out.println("2. No copying between JVM heap and native memory for I/O");
            System.out.println("3. Not limited by Java heap size constraints");
            
            // Note: We don't explicitly free the buffer - Java will eventually clean it up
            // but this is less predictable than heap memory garbage collection
        }
        catch (OutOfMemoryError e) {
            System.out.println("Failed to allocate direct buffer: " + e.getMessage());
            System.out.println("This could happen if native memory is limited");
        }
        
        // Show JVM memory after allocation
        System.out.println("\nJVM heap memory stats after direct allocation:");
        JVMMemoryStructure.printMemoryStats();
        System.out.println("Notice that heap memory doesn't show the 100MB we allocated!");
    }
    
    private static void comparePerformance() {
        System.out.println("\n--- Performance Comparison: Heap vs Direct Buffer ---");
        
        // Size of buffers
        int bufferSize = 10 * 1024 * 1024; // 10MB
        
        // Allocate buffers
        ByteBuffer heapBuffer = ByteBuffer.allocate(bufferSize);
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(bufferSize);
        
        // Time writing to heap buffer
        long startTime = System.nanoTime();
        for (int i = 0; i < bufferSize; i++) {
            heapBuffer.put(i, (byte) i);
        }
        long heapWriteTime = System.nanoTime() - startTime;
        
        // Time writing to direct buffer
        startTime = System.nanoTime();
        for (int i = 0; i < bufferSize; i++) {
            directBuffer.put(i, (byte) i);
        }
        long directWriteTime = System.nanoTime() - startTime;
        
        // Time reading from heap buffer
        startTime = System.nanoTime();
        for (int i = 0; i < bufferSize; i++) {
            byte b = heapBuffer.get(i);
        }
        long heapReadTime = System.nanoTime() - startTime;
        
        // Time reading from direct buffer
        startTime = System.nanoTime();
        for (int i = 0; i < bufferSize; i++) {
            byte b = directBuffer.get(i);
        }
        long directReadTime = System.nanoTime() - startTime;
        
        // Print results
        System.out.println("Write time - Heap: " + (heapWriteTime / 1000000) + 
                         "ms, Direct: " + (directWriteTime / 1000000) + "ms");
        System.out.println("Read time - Heap: " + (heapReadTime / 1000000) + 
                         "ms, Direct: " + (directReadTime / 1000000) + "ms");
        
        System.out.println("\nNote: Results can vary between runs and systems");
        System.out.println("Direct buffers typically perform better for I/O operations");
        System.out.println("but may be slower for simple in-memory operations");
    }
    
    private static void demonstrateBestPractices() {
        System.out.println("\n--- Direct Memory Best Practices ---");
        
        System.out.println("1. Use direct buffers for large, long-lived buffers that interact with I/O");
        System.out.println("2. Reuse direct buffers when possible instead of creating new ones");
        System.out.println("3. Be aware that allocation and deallocation is more expensive");
        
        System.out.println("\n--- Limitations ---");
        System.out.println("1. Direct memory allocation is slower than heap allocation");
        System.out.println("2. Not managed by regular garbage collection cycles");
        System.out.println("3. Can cause OutOfMemoryError if too much direct memory is allocated");
        System.out.println("4. Limited control over when buffers are actually freed");
        
        System.out.println("\n--- JVM Flags for Direct Memory ---");
        System.out.println("-XX:MaxDirectMemorySize=<size>  Set maximum direct memory size");
    }
}