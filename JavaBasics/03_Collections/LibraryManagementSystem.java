package collections;

import java.util.*;
import java.time.LocalDate;

class Book {
    private String isbn;
    private String title;
    private String author;
    private boolean isAvailable;

    public Book(String isbn, String title, String author) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.isAvailable = true;
    }

    // Getters and setters
    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    @Override
    public String toString() {
        return "Book{" +
                "title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", available=" + isAvailable +
                '}';
    }
}

class LibraryMember {
    private int memberId;
    private String name;

    public LibraryMember(int memberId, String name) {
        this.memberId = memberId;
        this.name = name;
    }

    public int getMemberId() { return memberId; }
    public String getName() { return name; }
}

public class LibraryManagementSystem {
    // Using HashMap to store books with ISBN as key
    private Map<String, Book> bookInventory;
    
    // Using ArrayList to store library members
    private List<LibraryMember> members;
    
    // Using HashSet to store unique ISBNs of borrowed books
    private Set<String> borrowedBooks;
    
    // Using Queue for book reservations
    private Queue<Map.Entry<String, Integer>> bookReservations;
    
    // Using TreeMap to maintain sorted lending history
    private TreeMap<LocalDate, List<String>> lendingHistory;

    public LibraryManagementSystem() {
        bookInventory = new HashMap<>();
        members = new ArrayList<>();
        borrowedBooks = new HashSet<>();
        bookReservations = new LinkedList<>();
        lendingHistory = new TreeMap<>();
    }

    public void addBook(String isbn, String title, String author) {
        bookInventory.put(isbn, new Book(isbn, title, author));
    }

    public void addMember(LibraryMember member) {
        members.add(member);
    }

    public boolean borrowBook(String isbn, int memberId) {
        Book book = bookInventory.get(isbn);
        if (book != null && book.isAvailable()) {
            book.setAvailable(false);
            borrowedBooks.add(isbn);
            
            // Record in lending history
            LocalDate today = LocalDate.now();
            lendingHistory.computeIfAbsent(today, k -> new ArrayList<>()).add(isbn);
            
            return true;
        }
        return false;
    }

    public void returnBook(String isbn) {
        Book book = bookInventory.get(isbn);
        if (book != null) {
            book.setAvailable(true);
            borrowedBooks.remove(isbn);
            
            // Process any reservations
            processReservations(isbn);
        }
    }

    public void reserveBook(String isbn, int memberId) {
        bookReservations.offer(new AbstractMap.SimpleEntry<>(isbn, memberId));
    }

    private void processReservations(String isbn) {
        Iterator<Map.Entry<String, Integer>> iterator = bookReservations.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> reservation = iterator.next();
            if (reservation.getKey().equals(isbn)) {
                // Notify member that book is available
                System.out.println("Book " + isbn + " is now available for member " + reservation.getValue());
                iterator.remove();
                break;
            }
        }
    }

    public void printInventory() {
        System.out.println("\nCurrent Library Inventory:");
        for (Book book : bookInventory.values()) {
            System.out.println(book);
        }
    }

    public void printLendingHistory() {
        System.out.println("\nLending History:");
        for (Map.Entry<LocalDate, List<String>> entry : lendingHistory.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    public static void main(String[] args) {
        LibraryManagementSystem library = new LibraryManagementSystem();

        // Add books
        library.addBook("123", "Java Programming", "John Doe");
        library.addBook("456", "Data Structures", "Jane Smith");
        library.addBook("789", "Algorithms", "Alan Turing");

        // Add members
        library.addMember(new LibraryMember(1, "Alice"));
        library.addMember(new LibraryMember(2, "Bob"));

        // Demonstrate operations
        System.out.println("Initial inventory:");
        library.printInventory();

        // Borrow books
        library.borrowBook("123", 1);
        library.borrowBook("456", 2);

        // Reserve a book
        library.reserveBook("123", 2);

        System.out.println("\nAfter borrowing:");
        library.printInventory();

        // Return a book
        library.returnBook("123");

        System.out.println("\nAfter returning:");
        library.printInventory();

        // Print lending history
        library.printLendingHistory();
    }
}