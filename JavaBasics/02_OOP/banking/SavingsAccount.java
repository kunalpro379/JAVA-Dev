package banking;

import java.util.*;
import java.time.LocalDate;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SavingsAccount extends BankAccount {
    private double interestRate;
    private static final double DEFAULT_INTEREST_RATE = 0.045; // 4.5% annual interest
    private Map<String, SavingsGoal> savingsGoals;
    private final ScheduledExecutorService scheduler;
    
    // Inner class for savings goals
    private static class SavingsGoal {
        private final String name;
        private final double targetAmount;
        private double currentAmount;
        private final LocalDate targetDate;
        
        public SavingsGoal(String name, double targetAmount, LocalDate targetDate) {
            this.name = name;
            this.targetAmount = targetAmount;
            this.currentAmount = 0.0;
            this.targetDate = targetDate;
        }
        
        public double getRemainingAmount() {
            return targetAmount - currentAmount;
        }
        
        public boolean isAchieved() {
            return currentAmount >= targetAmount;
        }
        
        @Override
        public String toString() {
            return String.format("%s: $%.2f/$%.2f (Target date: %s)", 
                name, currentAmount, targetAmount, targetDate);
        }
    }

    public SavingsAccount(String accountNumber, String accountHolder, double initialBalance) {
        super(accountNumber, accountHolder, initialBalance);
        this.interestRate = calculateTieredInterestRate(initialBalance);
        this.savingsGoals = new HashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        startAutomaticInterestCalculation();
    }

    // Tiered interest rates based on balance
    private double calculateTieredInterestRate(double balance) {
        if (balance >= 100000) return 0.065;      // 6.5% for 100k+
        else if (balance >= 50000) return 0.055;  // 5.5% for 50k+
        else if (balance >= 10000) return 0.045;  // 4.5% for 10k+
        else return 0.035;                        // 3.5% for under 10k
    }

    @Override
    public double calculateInterest() {
        return balance * calculateTieredInterestRate(balance);
    }

    // Automated daily interest calculation
    private void startAutomaticInterestCalculation() {
        scheduler.scheduleAtFixedRate(() -> {
            double dailyInterest = calculateInterest() / 365.0;
            deposit(dailyInterest);
            logTransaction("DAILY_INTEREST", dailyInterest);
        }, 1, 1, TimeUnit.DAYS);
    }

    public void createSavingsGoal(String name, double targetAmount, LocalDate targetDate) {
        if (targetAmount <= 0) {
            throw new IllegalArgumentException("Target amount must be positive");
        }
        savingsGoals.put(name, new SavingsGoal(name, targetAmount, targetDate));
    }

    public void contributeToGoal(String goalName, double amount) {
        SavingsGoal goal = savingsGoals.get(goalName);
        if (goal == null) {
            throw new IllegalArgumentException("Savings goal not found: " + goalName);
        }
        
        if (amount > balance) {
            throw new IllegalArgumentException("Insufficient funds for goal contribution");
        }

        goal.currentAmount += amount;
        if (goal.isAchieved()) {
            logTransaction("GOAL_ACHIEVED", amount);
            System.out.println("Congratulations! Savings goal '" + goalName + "' has been achieved!");
        }
    }

    public String getSavingsGoalStatus(String goalName) {
        SavingsGoal goal = savingsGoals.get(goalName);
        if (goal == null) {
            throw new IllegalArgumentException("Savings goal not found: " + goalName);
        }
        return goal.toString();
    }

    public List<String> getAllSavingsGoals() {
        return savingsGoals.values().stream()
            .map(SavingsGoal::toString)
            .toList();
    }

    @Override
    public void deposit(double amount) {
        super.deposit(amount);
        // Update interest rate based on new balance
        this.interestRate = calculateTieredInterestRate(getBalance());
    }

    public double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(double rate) {
        if (rate >= 0 && rate <= 0.1) { // Maximum 10% interest rate
            this.interestRate = rate;
        } else {
            throw new IllegalArgumentException("Invalid interest rate");
        }
    }

    // Cleanup resources when account is closed
    public void close() {
        scheduler.shutdown();
    }
}