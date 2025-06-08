package MemoryManagement;

/**
 * Demonstrates the lifecycle of objects in Java:
 * 1. Object Creation
 * 2. Object Usage
 * 3. Object Destruction (eligible for garbage collection)
 * 4. Finalization (optional, deprecated in modern Java)
 * 5. Garbage Collection
 */
public class ObjectLifecycle {
    
    public static void demonstrate() {
        // Object Creation Phase
        System.out.println("1. Object Creation Phase:");
        System.out.println("   - Memory is allocated on the heap");
        System.out.println("   - Fields are initialized to default values");
        System.out.println("   - Constructor is executed");
        
        // Create a new object
        System.out.println("\nCreating a new LifecycleObject...");
        LifecycleObject obj = new LifecycleObject("Demo Object");
        
        // Object Usage Phase
        System.out.println("\n2. Object Usage Phase:");
        System.out.println("   - Object's methods and fields are accessed");
        obj.performAction();
        
        // Object becomes eligible for Garbage Collection
        System.out.println("\n3. Object becomes eligible for Garbage Collection when:");
        System.out.println("   - No references to the object exist");
        System.out.println("   - All references to the object go out of scope");
        System.out.println("   - References are explicitly set to null");
        
        // Making our object eligible for garbage collection
        System.out.println("\nMaking our object eligible for garbage collection...");
        obj = null; // Remove the reference
        
        // Requesting garbage collection (no guarantee it will run)
        System.out.println("\n4. Garbage Collection Phase (if triggered):");
        System.out.println("   - JVM decides when to run the garbage collector");
        System.out.println("   - Objects with no references are reclaimed");
        System.out.println("   - Memory is freed");
        
        System.out.println("\nRequesting garbage collection (not guaranteed to run immediately)...");
        System.gc();
        
        // Wait a bit to allow GC to potentially run
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println("\nNote: Modern JVM implementations optimize garbage collection.");
        System.out.println("The finalize() method is deprecated as of Java 9.");
        System.out.println("Instead, use try-with-resources or explicit cleanup methods.");
    }
    
    // Inner class to demonstrate object lifecycle
    static class LifecycleObject {
        private String name;
        
        // Constructor - part of creation phase
        public LifecycleObject(String name) {
            this.name = name;
            System.out.println("Constructor executed: Object '" + name + "' created");
        }
        
        // Method used during usage phase
        public void performAction() {
            System.out.println("Object '" + name + "' is being used");
        }
        
        // Finalize method - called before garbage collection (deprecated in modern Java)
        @Override
        protected void finalize() throws Throwable {
            try {
                System.out.println("Finalize method called for object '" + name + "'");
                System.out.println("(Note: finalize() is deprecated in modern Java)");
            } finally {
                super.finalize();
            }
        }
    }
}