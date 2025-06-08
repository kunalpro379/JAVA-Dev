package banking;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.time.LocalDateTime;
import java.util.Queue;

// Abstract class demonstrating abstraction
public abstract class BankAccount {
    private String accountNumber;
    private String accountHolder;
    protected double balance;
    private static double minimumBalance = 100.0;
    private final ReentrantLock lock = new ReentrantLock();
    private Queue<Transaction> transactionHistory;

    // Inner class for transaction records
    protected static class Transaction {
        private final LocalDateTime timestamp;
        private final String type;
        private final double amount;
        private final double balanceAfter;

        public Transaction(String type, double amount, double balanceAfter) {
            this.timestamp = LocalDateTime.now();
            this.type = type;
            this.amount = amount;
            this.balanceAfter = balanceAfter;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: $%.2f (Balance: $%.2f)",
                timestamp, type, amount, balanceAfter);
        }
    }

    // Constructor with encapsulation
    public BankAccount(String accountNumber, String accountHolder, double initialBalance) {
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.balance = initialBalance;
        this.transactionHistory = new ConcurrentLinkedQueue<>();
        logTransaction("INITIAL_DEPOSIT", initialBalance);
    }

    // Abstract method for interest calculation
    public abstract double calculateInterest();

    // Thread-safe deposit with logging
    public void deposit(double amount) {
        lock.lock();
        try {
            if (amount > 0) {
                balance += amount;
                logTransaction("DEPOSIT", amount);
                System.out.println("Deposited: $" + amount);
            } else {
                throw new IllegalArgumentException("Invalid deposit amount");
            }
        } finally {
            lock.unlock();
        }
    }

    // Thread-safe withdrawal with logging
    public void withdraw(double amount) {
        lock.lock();
        try {
            if (amount > 0 && (balance - amount) >= minimumBalance) {
                balance -= amount;
                logTransaction("WITHDRAWAL", -amount);
                System.out.println("Withdrawn: $" + amount);
            } else {
                throw new IllegalArgumentException("Invalid withdrawal amount or insufficient funds");
            }
        } finally {
            lock.unlock();
        }
    }

    protected void logTransaction(String type, double amount) {
        transactionHistory.offer(new Transaction(type, amount, balance));
    }

    public Queue<Transaction> getTransactionHistory() {
        return new ConcurrentLinkedQueue<>(transactionHistory);
    }

    // Getters and setters demonstrating encapsulation
    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public double getBalance() {
        lock.lock();
        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }

    protected static double getMinimumBalance() {
        return minimumBalance;
    }
}