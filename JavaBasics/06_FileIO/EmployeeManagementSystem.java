package fileio;

import java.io.*;
import java.util.*;
import java.nio.file.*;

class Employee implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int id;
    private String name;
    private String department;
    private double salary;
    private Map<String, String> attributes;

    public Employee(int id, String name, String department, double salary) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.salary = salary;
        this.attributes = new HashMap<>();
    }

    public void addAttribute(String key, String value) {
        attributes.put(key, value);
    }

    @Override
    public String toString() {
        return String.format("Employee{id=%d, name='%s', department='%s', salary=%.2f, attributes=%s}",
                id, name, department, salary, attributes);
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public double getSalary() { return salary; }
    public Map<String, String> getAttributes() { return new HashMap<>(attributes); }
}

public class EmployeeManagementSystem {
    private List<Employee> employees;
    private static final String DATA_FILE = "employees.dat";
    private static final String CSV_FILE = "employees.csv";
    private static final String BACKUP_DIR = "backups";

    public EmployeeManagementSystem() {
        this.employees = new ArrayList<>();
        createBackupDirectory();
    }

    private void createBackupDirectory() {
        try {
            Files.createDirectories(Paths.get(BACKUP_DIR));
        } catch (IOException e) {
            System.err.println("Could not create backup directory: " + e.getMessage());
        }
    }

    // Save employees to binary file using serialization
    public void saveEmployees() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(DATA_FILE))) {
            oos.writeObject(employees);
            
            // Create backup
            String backupFile = BACKUP_DIR + File.separator + 
                              "backup_" + System.currentTimeMillis() + ".dat";
            Files.copy(Paths.get(DATA_FILE), Paths.get(backupFile), 
                      StandardCopyOption.REPLACE_EXISTING);
            
            System.out.println("Employees saved successfully with backup");
            
        } catch (IOException e) {
            System.err.println("Error saving employees: " + e.getMessage());
        }
    }

    // Load employees from binary file
    @SuppressWarnings("unchecked")
    public void loadEmployees() {
        if (!Files.exists(Paths.get(DATA_FILE))) {
            System.out.println("No existing data file found");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(DATA_FILE))) {
            employees = (List<Employee>) ois.readObject();
            System.out.println("Employees loaded successfully");
            
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading employees: " + e.getMessage());
            // Try to recover from backup
            recoverFromBackup();
        }
    }

    // Export employees to CSV file
    public void exportToCSV() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(CSV_FILE))) {
            // Write header
            writer.println("ID,Name,Department,Salary,Attributes");
            
            // Write employee data
            for (Employee emp : employees) {
                writer.printf("%d,%s,%s,%.2f,%s%n",
                    emp.getId(),
                    emp.getName(),
                    emp.getDepartment(),
                    emp.getSalary(),
                    emp.getAttributes().toString()
                );
            }
            
            System.out.println("Employees exported to CSV successfully");
            
        } catch (IOException e) {
            System.err.println("Error exporting to CSV: " + e.getMessage());
        }
    }

    // Import employees from CSV file
    public void importFromCSV() {
        try (BufferedReader reader = new BufferedReader(new FileReader(CSV_FILE))) {
            String line = reader.readLine(); // Skip header
            
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                Employee emp = new Employee(
                    Integer.parseInt(data[0]),
                    data[1],
                    data[2],
                    Double.parseDouble(data[3])
                );
                
                // Parse attributes if present
                if (data.length > 4) {
                    String[] attrs = data[4].substring(1, data[4].length() - 1).split(", ");
                    for (String attr : attrs) {
                        String[] keyValue = attr.split("=");
                        if (keyValue.length == 2) {
                            emp.addAttribute(keyValue[0], keyValue[1]);
                        }
                    }
                }
                
                employees.add(emp);
            }
            
            System.out.println("Employees imported from CSV successfully");
            
        } catch (IOException e) {
            System.err.println("Error importing from CSV: " + e.getMessage());
        }
    }

    private void recoverFromBackup() {
        try {
            Path backupDir = Paths.get(BACKUP_DIR);
            if (!Files.exists(backupDir)) {
                System.err.println("No backup directory found");
                return;
            }

            // Find latest backup file
            Optional<Path> latestBackup = Files.list(backupDir)
                .filter(path -> path.toString().endsWith(".dat"))
                .max(Comparator.comparingLong(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis();
                    } catch (IOException e) {
                        return 0L;
                    }
                }));

            if (latestBackup.isPresent()) {
                try (ObjectInputStream ois = new ObjectInputStream(
                        new FileInputStream(latestBackup.get().toFile()))) {
                    employees = (List<Employee>) ois.readObject();
                    System.out.println("Recovered from backup successfully");
                }
            } else {
                System.err.println("No backup files found");
            }
            
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error recovering from backup: " + e.getMessage());
        }
    }

    public void addEmployee(Employee emp) {
        employees.add(emp);
    }

    public List<Employee> getEmployees() {
        return new ArrayList<>(employees);
    }

    public static void main(String[] args) {
        EmployeeManagementSystem ems = new EmployeeManagementSystem();

        // Add some sample employees
        Employee emp1 = new Employee(1, "John Doe", "IT", 75000);
        emp1.addAttribute("location", "New York");
        emp1.addAttribute("level", "Senior");

        Employee emp2 = new Employee(2, "Jane Smith", "HR", 65000);
        emp2.addAttribute("location", "London");
        emp2.addAttribute("joined", "2022");

        ems.addEmployee(emp1);
        ems.addEmployee(emp2);

        // Demonstrate file operations
        System.out.println("Initial employees:");
        ems.getEmployees().forEach(System.out::println);

        // Save to binary file
        ems.saveEmployees();

        // Export to CSV
        ems.exportToCSV();

        // Clear and load from binary file
        ems.employees.clear();
        ems.loadEmployees();

        System.out.println("\nEmployees after loading from binary file:");
        ems.getEmployees().forEach(System.out::println);

        // Clear and import from CSV
        ems.employees.clear();
        ems.importFromCSV();

        System.out.println("\nEmployees after importing from CSV:");
        ems.getEmployees().forEach(System.out::println);
    }
}