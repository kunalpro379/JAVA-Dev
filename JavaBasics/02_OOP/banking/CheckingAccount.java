package banking;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.Duration;

public class CheckingAccount extends BankAccount {
    private double overdraftLimit;
    private static final double DEFAULT_OVERDRAFT_LIMIT = 1000.0;
    private static final double OVERDRAFT_FEE = 35.0;
    private static final double CHECKING_INTEREST_RATE = 0.01; // 1% interest rate
    private List<Consumer<String>> notificationObservers = new ArrayList<>();
    private static final int FRAUD_DETECTION_THRESHOLD = 5000;
    private LocalDateTime lastLargeTransaction;

    public CheckingAccount(String accountNumber, String accountHolder, double initialBalance) {
        super(accountNumber, accountHolder, initialBalance);
        this.overdraftLimit = DEFAULT_OVERDRAFT_LIMIT;
        this.lastLargeTransaction = LocalDateTime.now().minusDays(1);
    }

    @Override
    public double calculateInterest() {
        return balance * CHECKING_INTEREST_RATE;
    }

    // Implement Observer pattern for notifications
    public void addNotificationObserver(Consumer<String> observer) {
        notificationObservers.add(observer);
    }

    private void notifyObservers(String message) {
        notificationObservers.forEach(observer -> observer.accept(message));
    }

    // Asynchronous fraud detection
    private CompletableFuture<Boolean> checkForFraudAsync(double amount) {
        return CompletableFuture.supplyAsync(() -> {
            // Check for rapid large transactions
            if (amount >= FRAUD_DETECTION_THRESHOLD) {
                Duration timeSinceLastLarge = Duration.between(lastLargeTransaction, LocalDateTime.now());
                if (timeSinceLastLarge.toHours() < 24) {
                    notifyObservers("FRAUD ALERT: Multiple large transactions detected within 24 hours!");
                    return true;
                }
                lastLargeTransaction = LocalDateTime.now();
            }
            return false;
        });
    }

    @Override
    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Invalid withdrawal amount");
        }

        checkForFraudAsync(amount).thenAccept(isFraudulent -> {
            if (!isFraudulent) {
                try {
                    if (balance - amount >= -overdraftLimit) {
                        if (balance - amount < 0) {
                            // Apply overdraft fee
                            balance -= OVERDRAFT_FEE;
                            logTransaction("OVERDRAFT_FEE", -OVERDRAFT_FEE);
                            notifyObservers(String.format("Overdraft fee of $%.2f applied to account %s", 
                                OVERDRAFT_FEE, getAccountNumber()));
                        }
                        balance -= amount;
                        logTransaction("WITHDRAWAL", -amount);
                        
                        // Low balance notification
                        if (balance < 100) {
                            notifyObservers(String.format("Low balance alert: Current balance is $%.2f", balance));
                        }
                        
                        System.out.println("Withdrawn: $" + amount);
                    } else {
                        throw new IllegalArgumentException("Amount exceeds overdraft limit");
                    }
                } catch (Exception e) {
                    notifyObservers("Transaction failed: " + e.getMessage());
                    throw e;
                }
            }
        });
    }

    public double getOverdraftLimit() {
        return overdraftLimit;
    }

    public void setOverdraftLimit(double limit) {
        if (limit >= 0) {
            this.overdraftLimit = limit;
            notifyObservers(String.format("Overdraft limit updated to $%.2f", limit));
        } else {
            throw new IllegalArgumentException("Invalid overdraft limit");
        }
    }
}