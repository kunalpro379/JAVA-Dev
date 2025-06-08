package nio;

import java.nio.file.*;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.HashMap;
import java.util.Map;

class FileProcessor {
    private final Path directory;
    private final WatchService watchService;
    private final ExecutorService executor;
    private final Map<Path, Long> fileChanges;
    private volatile boolean isRunning;

    public FileProcessor(String directoryPath) throws IOException {
        this.directory = Paths.get(directoryPath);
        this.watchService = FileSystems.getDefault().newWatchService();
        this.executor = Executors.newFixedThreadPool(3);
        this.fileChanges = new HashMap<>();
        this.isRunning = true;

        // Register directory with watch service
        directory.register(watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE);
    }

    public void startMonitoring() {
        // Start the watch service in a separate thread
        executor.submit(() -> {
            try {
                while (isRunning) {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        handleFileEvent(event);
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void handleFileEvent(WatchEvent<?> event) {
        @SuppressWarnings("unchecked")
        WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
        Path filename = pathEvent.context();
        Path fullPath = directory.resolve(filename);

        executor.submit(() -> {
            try {
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    processNewFile(fullPath);
                } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                    processModifiedFile(fullPath);
                } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                    processDeletedFile(fullPath);
                }
            } catch (IOException e) {
                System.err.println("Error processing file: " + e.getMessage());
            }
        });
    }

    private void processNewFile(Path file) throws IOException {
        System.out.println("New file detected: " + file);
        if (Files.isRegularFile(file)) {
            // Demonstrate NIO reading
            readFileContent(file);
            fileChanges.put(file, Files.getLastModifiedTime(file).toMillis());
        }
    }

    private void processModifiedFile(Path file) throws IOException {
        if (!Files.exists(file)) return;

        Long lastModified = fileChanges.get(file);
        long currentModified = Files.getLastModifiedTime(file).toMillis();

        if (lastModified == null || lastModified < currentModified) {
            System.out.println("File modified: " + file);
            readFileContent(file);
            fileChanges.put(file, currentModified);
        }
    }

    private void processDeletedFile(Path file) {
        System.out.println("File deleted: " + file);
        fileChanges.remove(file);
    }

    private void readFileContent(Path file) throws IOException {
        // Using NIO Channel and Buffer for efficient reading
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            StringBuilder content = new StringBuilder();

            while (channel.read(buffer) != -1) {
                buffer.flip();
                content.append(StandardCharsets.UTF_8.decode(buffer));
                buffer.clear();
            }

            System.out.println("File content: " + content.toString().trim());
        }
    }

    public void writeFile(String filename, String content) throws IOException {
        Path file = directory.resolve(filename);
        
        // Using NIO for writing
        try (FileChannel channel = FileChannel.open(file,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            
            ByteBuffer buffer = ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        }
    }

    public void copyFile(String source, String target) throws IOException {
        Path sourcePath = directory.resolve(source);
        Path targetPath = directory.resolve(target);

        // Using NIO for efficient file copy
        try (FileChannel sourceChannel = FileChannel.open(sourcePath, StandardOpenOption.READ);
             FileChannel targetChannel = FileChannel.open(targetPath,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.WRITE)) {

            sourceChannel.transferTo(0, sourceChannel.size(), targetChannel);
        }
    }

    public void shutdown() {
        isRunning = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
            watchService.close();
        } catch (Exception e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        try {
            // Create a temporary directory for demonstration
            Path tempDir = Files.createTempDirectory("nio_demo");
            FileProcessor processor = new FileProcessor(tempDir.toString());
            processor.startMonitoring();

            System.out.println("Monitoring directory: " + tempDir);

            // Demonstrate file operations
            processor.writeFile("test.txt", "Hello, NIO!");
            Thread.sleep(1000);

            processor.writeFile("test2.txt", "Another file");
            Thread.sleep(1000);

            processor.copyFile("test.txt", "test_copy.txt");
            Thread.sleep(1000);

            // Clean up
            processor.shutdown();
            
            // Delete temporary directory and its contents
            Files.walk(tempDir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        System.err.println("Failed to delete: " + path);
                    }
                });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}