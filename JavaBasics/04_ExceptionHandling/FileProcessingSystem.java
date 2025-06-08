package exceptionhandling;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// Custom Exceptions
class InvalidFileFormatException extends Exception {
    public InvalidFileFormatException(String message) {
        super(message);
    }
}

class FileSizeLimitExceededException extends Exception {
    public FileSizeLimitExceededException(String message) {
        super(message);
    }
}

class ProcessingFailedException extends Exception {
    public ProcessingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class FileProcessingSystem {
    private static final long MAX_FILE_SIZE = 1024 * 1024; // 1MB
    private static final String[] ALLOWED_EXTENSIONS = {".txt", ".csv", ".json"};
    private List<String> processedFiles;
    private List<String> failedFiles;

    public FileProcessingSystem() {
        this.processedFiles = new ArrayList<>();
        this.failedFiles = new ArrayList<>();
    }

    public void processFile(String filePath) throws InvalidFileFormatException, 
                                                  FileSizeLimitExceededException, 
                                                  ProcessingFailedException {
        try {
            // Validate file extension
            if (!hasValidExtension(filePath)) {
                failedFiles.add(filePath);
                throw new InvalidFileFormatException(
                    "Invalid file format. Allowed formats: txt, csv, json"
                );
            }

            File file = new File(filePath);
            
            // Check file existence
            if (!file.exists()) {
                failedFiles.add(filePath);
                throw new FileNotFoundException("File not found: " + filePath);
            }

            // Check file size
            if (file.length() > MAX_FILE_SIZE) {
                failedFiles.add(filePath);
                throw new FileSizeLimitExceededException(
                    "File size exceeds limit of 1MB"
                );
            }

            // Process the file
            processFileContent(file);
            processedFiles.add(filePath);
            
        } catch (FileNotFoundException e) {
            throw new ProcessingFailedException(
                "File processing failed: " + e.getMessage(), e
            );
        } catch (IOException e) {
            failedFiles.add(filePath);
            throw new ProcessingFailedException(
                "Error reading file: " + e.getMessage(), e
            );
        }
    }

    private boolean hasValidExtension(String filePath) {
        for (String ext : ALLOWED_EXTENSIONS) {
            if (filePath.toLowerCase().endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private void processFileContent(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Simulate processing with potential errors
                if (line.contains("ERROR")) {
                    throw new IOException("Error found in file content");
                }
                // Process the line...
                System.out.println("Processing: " + line);
            }
        }
    }

    public List<String> getProcessedFiles() {
        return new ArrayList<>(processedFiles);
    }

    public List<String> getFailedFiles() {
        return new ArrayList<>(failedFiles);
    }

    // Demonstration of try-with-resources and exception handling
    public static void main(String[] args) {
        FileProcessingSystem processor = new FileProcessingSystem();
        String[] filesToProcess = {
            "sample.txt",
            "data.csv",
            "invalid.exe",
            "toolarge.txt"
        };

        for (String file : filesToProcess) {
            try {
                System.out.println("\nProcessing file: " + file);
                processor.processFile(file);
                System.out.println("File processed successfully!");
                
            } catch (InvalidFileFormatException e) {
                System.err.println("Invalid format: " + e.getMessage());
                
            } catch (FileSizeLimitExceededException e) {
                System.err.println("Size limit exceeded: " + e.getMessage());
                
            } catch (ProcessingFailedException e) {
                System.err.println("Processing failed: " + e.getMessage());
                // Log the full stack trace for debugging
                e.printStackTrace();
                
            } finally {
                System.out.println("Cleanup operations for " + file);
            }
        }

        // Print summary
        System.out.println("\nProcessing Summary:");
        System.out.println("Successful files: " + processor.getProcessedFiles());
        System.out.println("Failed files: " + processor.getFailedFiles());
    }
}