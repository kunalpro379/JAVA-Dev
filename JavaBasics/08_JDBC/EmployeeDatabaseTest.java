package jdbc;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeDatabaseTest {
    private static final String TEST_DB_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private EmployeeDatabase db;
    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        db = new EmployeeDatabase(TEST_DB_URL);
        testEmployee = new Employee(
            1,
            "John",
            "Doe",
            "john.doe@test.com",
            new BigDecimal("50000.00"),
            LocalDate.now(),
            "IT"
        );
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void addEmployee_ShouldStoreEmployee() {
        // When
        db.addEmployee(testEmployee);

        // Then
        Optional<Employee> retrieved = db.getEmployee(testEmployee.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(testEmployee.getEmail(), retrieved.get().getEmail());
    }

    @Test
    void addEmployeesBatch_ShouldStoreMultipleEmployees() {
        // Given
        Employee employee2 = new Employee(
            2,
            "Jane",
            "Smith",
            "jane.smith@test.com",
            new BigDecimal("60000.00"),
            LocalDate.now(),
            "HR"
        );
        List<Employee> employees = Arrays.asList(testEmployee, employee2);

        // When
        db.addEmployeesBatch(employees);

        // Then
        assertTrue(db.getEmployee(1).isPresent());
        assertTrue(db.getEmployee(2).isPresent());
    }

    @Test
    void updateEmployeeSalary_ShouldModifySalary() {
        // Given
        db.addEmployee(testEmployee);
        BigDecimal newSalary = new BigDecimal("55000.00");

        // When
        db.updateEmployeeSalary(testEmployee.getId(), newSalary);

        // Then
        Optional<Employee> updated = db.getEmployee(testEmployee.getId());
        assertTrue(updated.isPresent());
        assertEquals(0, newSalary.compareTo(updated.get().getSalary()));
    }

    @Test
    void deleteEmployee_ShouldRemoveEmployee() {
        // Given
        db.addEmployee(testEmployee);

        // When
        db.deleteEmployee(testEmployee.getId());

        // Then
        Optional<Employee> deleted = db.getEmployee(testEmployee.getId());
        assertTrue(deleted.isEmpty());
    }

    @Test
    void getAverageSalaryByDepartment_ShouldCalculateCorrectly() {
        // Given
        Employee employee2 = new Employee(
            2,
            "Jane",
            "Smith",
            "jane.smith@test.com",
            new BigDecimal("60000.00"),
            LocalDate.now(),
            "IT"
        );
        db.addEmployeesBatch(Arrays.asList(testEmployee, employee2));

        // When
        Map<String, Double> avgSalaries = db.getAverageSalaryByDepartment();

        // Then
        assertEquals(55000.00, avgSalaries.get("IT"), 0.01);
    }

    @Test
    void updateEmployeeSalary_ShouldThrowException_WhenEmployeeNotFound() {
        // When & Then
        assertThrows(DatabaseException.class, () ->
            db.updateEmployeeSalary(999, new BigDecimal("50000.00"))
        );
    }

    @Test
    void deleteEmployee_ShouldThrowException_WhenEmployeeNotFound() {
        // When & Then
        assertThrows(DatabaseException.class, () ->
            db.deleteEmployee(999)
        );
    }

    @Test
    void addEmployee_ShouldThrowException_WhenDuplicateEmail() {
        // Given
        db.addEmployee(testEmployee);
        Employee duplicateEmail = new Employee(
            2,
            "Jane",
            "Smith",
            testEmployee.getEmail(), // Same email as testEmployee
            new BigDecimal("60000.00"),
            LocalDate.now(),
            "HR"
        );

        // When & Then
        assertThrows(DatabaseException.class, () ->
            db.addEmployee(duplicateEmail)
        );
    }
}