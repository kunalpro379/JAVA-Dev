package multithreading;

import java.util.concurrent.*;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Random;

// Task class representing a unit of work
class Task {
    private int id;
    private int processingTime;
    private TaskPriority priority;

    public enum TaskPriority {
        HIGH, MEDIUM, LOW
    }

    public Task(int id, int processingTime, TaskPriority priority) {
        this.id = id;
        this.processingTime = processingTime;
        this.priority = priority;
    }

    public int getId() { return id; }
    public int getProcessingTime() { return processingTime; }
    public TaskPriority getPriority() { return priority; }
}

// Thread-safe task queue with producer-consumer pattern
class TaskQueue {
    private Queue<Task> tasks = new LinkedList<>();
    private final int maxSize;
    private final Object lock = new Object();

    public TaskQueue(int maxSize) {
        this.maxSize = maxSize;
    }

    public void addTask(Task task) throws InterruptedException {
        synchronized (lock) {
            while (tasks.size() >= maxSize) {
                // Queue is full, wait for space
                lock.wait();
            }
            tasks.offer(task);
            System.out.println("Added task " + task.getId() + " with priority " + task.getPriority());
            lock.notifyAll(); // Notify waiting consumers
        }
    }

    public Task getTask() throws InterruptedException {
        synchronized (lock) {
            while (tasks.isEmpty()) {
                // Queue is empty, wait for tasks
                lock.wait();
            }
            Task task = tasks.poll();
            lock.notifyAll(); // Notify waiting producers
            return task;
        }
    }
}

// Worker thread that processes tasks
class TaskProcessor implements Runnable {
    private TaskQueue taskQueue;
    private String processorName;
    private volatile boolean running = true;

    public TaskProcessor(TaskQueue taskQueue, String name) {
        this.taskQueue = taskQueue;
        this.processorName = name;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Task task = taskQueue.getTask();
                System.out.println(processorName + " processing task " + task.getId());
                
                // Simulate processing time
                Thread.sleep(task.getProcessingTime());
                
                System.out.println(processorName + " completed task " + task.getId());
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}

// Task generator that simulates incoming tasks
class TaskGenerator implements Runnable {
    private TaskQueue taskQueue;
    private Random random = new Random();
    private volatile boolean running = true;
    private int taskIdCounter = 0;

    public TaskGenerator(TaskQueue taskQueue) {
        this.taskQueue = taskQueue;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                // Generate random task
                Task.TaskPriority priority = Task.TaskPriority.values()[
                    random.nextInt(Task.TaskPriority.values().length)];
                int processingTime = random.nextInt(1000) + 500; // 500-1500ms
                
                Task task = new Task(++taskIdCounter, processingTime, priority);
                taskQueue.addTask(task);
                
                // Wait before generating next task
                Thread.sleep(random.nextInt(1000));
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}

public class TaskProcessingSystem {
    public static void main(String[] args) {
        // Create task queue with maximum capacity
        TaskQueue taskQueue = new TaskQueue(10);
        
        // Create thread pool for task processors
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        
        // Create and start task processors
        TaskProcessor[] processors = new TaskProcessor[3];
        for (int i = 0; i < processors.length; i++) {
            processors[i] = new TaskProcessor(taskQueue, "Processor-" + (i + 1));
            executorService.submit(processors[i]);
        }
        
        // Create and start task generator
        TaskGenerator generator = new TaskGenerator(taskQueue);
        Thread generatorThread = new Thread(generator);
        generatorThread.start();
        
        // Run the system for some time
        try {
            Thread.sleep(10000); // Run for 10 seconds
            
            // Stop task generation and processing
            generator.stop();
            for (TaskProcessor processor : processors) {
                processor.stop();
            }
            
            // Shutdown executor service
            executorService.shutdown();
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            System.out.println("Task Processing System shutdown complete.");
        }
    }
}