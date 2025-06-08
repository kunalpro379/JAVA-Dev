package banking;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BankingApp {
    private List<BankAccount> accounts;
    private final ExecutorService transactionProcessor;
    private static final Consumer<String> SMS_NOTIFIER = message -> 
        System.out.println("SMS NOTIFICATION: " + message);
    private static final Consumer<String> EMAIL_NOTIFIER = message -> 
        System.out.println("EMAIL NOTIFICATION: " + message);

    public BankingApp() {
        this.accounts = new ArrayList<>();
        this.transactionProcessor = Executors.newFixedThreadPool(4);
    }

    public void addAccount(BankAccount account) {
        accounts.add(account);
        if (account instanceof CheckingAccount) {
            CheckingAccount checkingAccount = (CheckingAccount) account;
            // Add notification observers
            checkingAccount.addNotificationObserver(SMS_NOTIFIER);
            checkingAccount.addNotificationObserver(EMAIL_NOTIFIER);
        }
    }

    // Process transactions asynchronously
    public void processTransactionAsync(Runnable transaction) {
        transactionProcessor.submit(() -> {
            try {
                transaction.run();
            } catch (Exception e) {
                System.err.println("Transaction Error: " + e.getMessage());
            }
        });
    }

    // Demonstrating polymorphism through method overloading
    public void processMonthlyInterest() {
        accounts.forEach(account -> {
            if (account instanceof SavingsAccount) {
                SavingsAccount savingsAccount = (SavingsAccount) account;
                double interest = savingsAccount.calculateInterest();
                processTransactionAsync(() -> savingsAccount.deposit(interest));
                System.out.println("Added monthly interest: $" + String.format("%.2f", interest));
            }
        });
    }

    public void printAccountSummaries() {
        System.out.println("\n=== Account Summaries ===");
        accounts.forEach(account -> {
            System.out.println("\nAccount Holder: " + account.getAccountHolder());
            System.out.println("Account Number: " + account.getAccountNumber());
            System.out.println("Current Balance: $" + String.format("%.2f", account.getBalance()));
            System.out.println("Account Type: " + account.getClass().getSimpleName());
            
            if (account instanceof CheckingAccount) {
                System.out.println("Overdraft Limit: $" + 
                    String.format("%.2f", ((CheckingAccount) account).getOverdraftLimit()));
            } else if (account instanceof SavingsAccount) {
                SavingsAccount savingsAccount = (SavingsAccount) account;
                System.out.println("Interest Rate: " + 
                    String.format("%.1f%%", savingsAccount.getInterestRate() * 100));
                System.out.println("\nSavings Goals:");
                savingsAccount.getAllSavingsGoals().forEach(goal -> 
                    System.out.println("  " + goal));
            }
            
            System.out.println("\nRecent Transactions:");
            account.getTransactionHistory()
                .stream()
                .limit(5)
                .forEach(transaction -> System.out.println("  " + transaction));
        });
    }

    public void shutdown() {
        transactionProcessor.shutdown();
        try {
            if (!transactionProcessor.awaitTermination(60, TimeUnit.SECONDS)) {
                transactionProcessor.shutdownNow();
            }
        } catch (InterruptedException e) {
            transactionProcessor.shutdownNow();
        }
        
        // Close all savings accounts to clean up schedulers
        accounts.stream()
            .filter(account -> account instanceof SavingsAccount)
            .map(account -> (SavingsAccount) account)
            .forEach(SavingsAccount::close);
    }

    public static void main(String[] args) {
        BankingApp bank = new BankingApp();

        try {
            // Create different types of accounts
            SavingsAccount savings = new SavingsAccount("SAV001", "John Doe", 50000.0);
            CheckingAccount checking = new CheckingAccount("CHK001", "Jane Smith", 2000.0);

            // Add accounts to the bank
            bank.addAccount(savings);
            bank.addAccount(checking);

            // Create savings goals
            savings.createSavingsGoal("Vacation", 10000.0, LocalDate.now().plusMonths(6));
            savings.createSavingsGoal("New Car", 25000.0, LocalDate.now().plusYears(1));

            // Demonstrate operations
            System.out.println("=== Initial Account States ===");
            bank.printAccountSummaries();

            // Perform some transactions
            bank.processTransactionAsync(() -> {
                savings.deposit(5000.0);
                savings.contributeToGoal("Vacation", 2000.0);
            });

            bank.processTransactionAsync(() -> {
                checking.withdraw(2500.0); // This will trigger overdraft
            });

            // Wait for async transactions to complete
            Thread.sleep(1000);

            System.out.println("\n=== After Transactions ===");
            bank.printAccountSummaries();

            // Process monthly interest
            System.out.println("\n=== Processing Monthly Interest ===");
            bank.processMonthlyInterest();

            // Wait for interest processing
            Thread.sleep(1000);

            System.out.println("\n=== After Interest ===");
            bank.printAccountSummaries();

            // Demonstrate fraud detection
            System.out.println("\n=== Testing Fraud Detection ===");
            bank.processTransactionAsync(() -> {
                checking.withdraw(5500.0); // This should trigger fraud detection
                checking.withdraw(5500.0); // This should trigger fraud alert
            });

            // Wait for fraud detection
            Thread.sleep(1000);

            // Clean up resources
            bank.shutdown();

        } catch (Exception e) {
            System.err.println("Error in banking operations: " + e.getMessage());
            e.printStackTrace();
        }
    }
}