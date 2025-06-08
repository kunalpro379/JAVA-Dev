package jdbc;

import java.sql.*;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDate;

class Employee {
    private int id;
    private String firstName;
    private String lastName;
    private String email;
    private BigDecimal salary;
    private LocalDate hireDate;
    private String department;

    // Constructor
    public Employee(int id, String firstName, String lastName, String email, 
                   BigDecimal salary, LocalDate hireDate, String department) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.salary = salary;
        this.hireDate = hireDate;
        this.department = department;
    }

    // Getters and setters
    public int getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public BigDecimal getSalary() { return salary; }
    public LocalDate getHireDate() { return hireDate; }
    public String getDepartment() { return department; }

    public void setSalary(BigDecimal salary) { this.salary = salary; }
    public void setDepartment(String department) { this.department = department; }

    @Override
    public String toString() {
        return String.format("Employee{id=%d, name='%s %s', email='%s', salary=%s, hireDate=%s, department='%s'}",
            id, firstName, lastName, email, salary, hireDate, department);
    }
}

class DatabaseException extends RuntimeException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class EmployeeDatabase implements AutoCloseable {
    private final Connection connection;
    private static final String CREATE_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS employees (
            id INT PRIMARY KEY,
            first_name VARCHAR(50) NOT NULL,
            last_name VARCHAR(50) NOT NULL,
            email VARCHAR(100) UNIQUE NOT NULL,
            salary DECIMAL(10,2) NOT NULL,
            hire_date DATE NOT NULL,
            department VARCHAR(50)
        )
    """;

    public EmployeeDatabase(String url) {
        try {
            // For H2 Database
            Class.forName("org.h2.Driver");
            connection = DriverManager.getConnection(url, "sa", "");
            initializeDatabase();
        } catch (ClassNotFoundException | SQLException e) {
            throw new DatabaseException("Failed to initialize database", e);
        }
    }

    private void initializeDatabase() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create tables", e);
        }
    }

    // Create operation with prepared statement
    public void addEmployee(Employee employee) {
        String sql = "INSERT INTO employees (id, first_name, last_name, email, salary, hire_date, department) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, employee.getId());
            pstmt.setString(2, employee.getFirstName());
            pstmt.setString(3, employee.getLastName());
            pstmt.setString(4, employee.getEmail());
            pstmt.setBigDecimal(5, employee.getSalary());
            pstmt.setDate(6, Date.valueOf(employee.getHireDate()));
            pstmt.setString(7, employee.getDepartment());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to add employee", e);
        }
    }

    // Read operation with result set
    public Optional<Employee> getEmployee(int id) {
        String sql = "SELECT * FROM employees WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(createEmployeeFromResultSet(rs));
            }
            return Optional.empty();
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve employee", e);
        }
    }

    // Update operation with transaction
    public void updateEmployeeSalary(int id, BigDecimal newSalary) {
        String sql = "UPDATE employees SET salary = ? WHERE id = ?";
        
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setBigDecimal(1, newSalary);
                pstmt.setInt(2, id);
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected == 0) {
                    throw new DatabaseException("Employee not found with id: " + id);
                }
                connection.commit();
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                throw new DatabaseException("Failed to rollback transaction", rollbackEx);
            }
            throw new DatabaseException("Failed to update employee salary", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new DatabaseException("Failed to reset auto-commit", e);
            }
        }
    }

    // Delete operation
    public void deleteEmployee(int id) {
        String sql = "DELETE FROM employees WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected == 0) {
                throw new DatabaseException("Employee not found with id: " + id);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete employee", e);
        }
    }

    // Advanced query with joins and aggregation
    public Map<String, Double> getAverageSalaryByDepartment() {
        String sql = """
            SELECT department, AVG(salary) as avg_salary 
            FROM employees 
            GROUP BY department
        """;
        
        Map<String, Double> result = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String department = rs.getString("department");
                double avgSalary = rs.getDouble("avg_salary");
                result.put(department, avgSalary);
            }
            return result;
            
        } catch (SQLException e) {
            throw new DatabaseException("Failed to calculate average salaries", e);
        }
    }

    // Batch processing example
    public void addEmployeesBatch(List<Employee> employees) {
        String sql = "INSERT INTO employees (id, first_name, last_name, email, salary, hire_date, department) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                for (Employee emp : employees) {
                    pstmt.setInt(1, emp.getId());
                    pstmt.setString(2, emp.getFirstName());
                    pstmt.setString(3, emp.getLastName());
                    pstmt.setString(4, emp.getEmail());
                    pstmt.setBigDecimal(5, emp.getSalary());
                    pstmt.setDate(6, Date.valueOf(emp.getHireDate()));
                    pstmt.setString(7, emp.getDepartment());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                connection.commit();
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                throw new DatabaseException("Failed to rollback transaction", rollbackEx);
            }
            throw new DatabaseException("Failed to add employees in batch", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new DatabaseException("Failed to reset auto-commit", e);
            }
        }
    }

    private Employee createEmployeeFromResultSet(ResultSet rs) throws SQLException {
        return new Employee(
            rs.getInt("id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("email"),
            rs.getBigDecimal("salary"),
            rs.getDate("hire_date").toLocalDate(),
            rs.getString("department")
        );
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to close database connection", e);
        }
    }

    public static void main(String[] args) {
        // Using H2 in-memory database for demonstration
        String dbUrl = "jdbc:h2:mem:employeedb;DB_CLOSE_DELAY=-1";
        
        try (EmployeeDatabase db = new EmployeeDatabase(dbUrl)) {
            // Add sample employees
            List<Employee> employees = Arrays.asList(
                new Employee(1, "John", "Doe", "john.doe@company.com",
                    new BigDecimal("75000.00"), LocalDate.of(2020, 1, 15), "IT"),
                new Employee(2, "Jane", "Smith", "jane.smith@company.com",
                    new BigDecimal("82000.00"), LocalDate.of(2019, 6, 1), "HR"),
                new Employee(3, "Bob", "Johnson", "bob.johnson@company.com",
                    new BigDecimal("65000.00"), LocalDate.of(2021, 3, 10), "IT")
            );
            
            // Demonstrate batch insert
            System.out.println("Adding employees in batch...");
            db.addEmployeesBatch(employees);

            // Demonstrate retrieval
            System.out.println("\nRetrieving employee with ID 1:");
            db.getEmployee(1).ifPresent(System.out::println);

            // Demonstrate update
            System.out.println("\nUpdating salary for employee 2...");
            db.updateEmployeeSalary(2, new BigDecimal("85000.00"));
            db.getEmployee(2).ifPresent(System.out::println);

            // Demonstrate aggregation
            System.out.println("\nAverage salaries by department:");
            Map<String, Double> avgSalaries = db.getAverageSalaryByDepartment();
            avgSalaries.forEach((dept, avg) -> 
                System.out.printf("%s: $%.2f%n", dept, avg));

            // Demonstrate delete
            System.out.println("\nDeleting employee 3...");
            db.deleteEmployee(3);
            
            // Verify deletion
            System.out.println("Trying to retrieve deleted employee:");
            Optional<Employee> deleted = db.getEmployee(3);
            System.out.println(deleted.isPresent() ? "Still exists!" : "Successfully deleted");
            
        } catch (DatabaseException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}