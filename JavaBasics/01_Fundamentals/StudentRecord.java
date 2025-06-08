package fundamentals;

public class StudentRecord {
    // Demonstrating different data types
    private String studentName;
    private int rollNumber;
    private double gpa;
    private boolean isEnrolled;
    private char grade;
    private static int totalStudents = 0;

    // Constructor demonstrating initialization
    public StudentRecord(String name, int roll) {
        this.studentName = name;
        this.rollNumber = roll;
        this.isEnrolled = true;
        this.gpa = 0.0;
        this.grade = 'N';
        totalStudents++;
    }

    // Method demonstrating type conversion and calculations
    public void updateGrades(double[] marks) {
        double total = 0;
        for (double mark : marks) {
            total += mark;
        }
        this.gpa = total / marks.length;
        
        // Demonstrating control structures
        if (gpa >= 90) {
            grade = 'A';
        } else if (gpa >= 80) {
            grade = 'B';
        } else if (gpa >= 70) {
            grade = 'C';
        } else {
            grade = 'F';
        }
    }

    // Method demonstrating string operations
    public String generateReport() {
        return "Student: " + studentName + 
               "\nRoll Number: " + rollNumber +
               "\nGPA: " + String.format("%.2f", gpa) +
               "\nGrade: " + grade +
               "\nEnrollment Status: " + (isEnrolled ? "Active" : "Inactive");
    }

    // Static method demonstrating class-level operations
    public static int getTotalStudents() {
        return totalStudents;
    }

    // Main method demonstrating actual usage
    public static void main(String[] args) {
        // Creating students
        StudentRecord student1 = new StudentRecord("John Doe", 101);
        StudentRecord student2 = new StudentRecord("Jane Smith", 102);

        // Updating grades
        student1.updateGrades(new double[]{85.5, 90.0, 87.5, 88.0});
        student2.updateGrades(new double[]{92.0, 88.5, 95.0, 91.5});

        // Generating and printing reports
        System.out.println("=== Student Reports ===");
        System.out.println(student1.generateReport());
        System.out.println("\n----------------------");
        System.out.println(student2.generateReport());
        System.out.println("\nTotal Students: " + getTotalStudents());
    }
}