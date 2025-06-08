import java.util.Scanner;

public class BankingSystem {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Account account1 = new Account("John Doe", 1000);
        Account account2 = new Account("Jane Smith", 2000);

        while (true) {
            System.out.println("Welcome to the Banking System");
            System.out.println("1. Deposit");
            System.out.println("2. Withdraw");
            System.out.println("3. Transfer");
            System.out.println("4. Check Balance");
            System.out.println("5. Exit");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    System.out.print("Enter account number (1 or 2): ");
                    int accNum = scanner.nextInt();
                    System.out.print("Enter amount to deposit: ");
                    double amount = scanner.nextDouble();
                    if (accNum == 1) {
                        account1.deposit(amount);
                    } else if (accNum == 2) {
                        account2.deposit(amount);
                    } else {
                        System.out.println("Invalid account number.");
                    }
                    break;
                case 2:
                    System.out.print("Enter account number (1 or 2): ");
                    accNum = scanner.nextInt();
                    System.out.print("Enter amount to withdraw: ");
                    amount = scanner.nextDouble();
                    if (accNum == 1) {
                        account1.withdraw(amount);
                    } else if (accNum == 2) {
                        account2.withdraw(amount);
                    } else {
                        System.out.println("Invalid account number.");
                    }
                    break;
                case 3:
                    System.out.print("Enter amount to transfer: ");
                    amount = scanner.nextDouble();
                    System.out.print("Transfer from account (1 or 2): ");
                    int fromAcc = scanner.nextInt();
                    System.out.print("Transfer to account (1 or 2): ");
                    int toAcc = scanner.nextInt();
                    if (fromAcc == 1 && toAcc == 2) {
                        account1.transfer(account2, amount);
                    } else if (fromAcc == 2 && toAcc == 1) {
                        account2.transfer(account1, amount);
                    } else {
                        System.out.println("Invalid account numbers.");
                    }
                    break;
                case 4:
                    System.out.println("Account 1 balance: " + account1.getBalance());
                    System.out.println("Account 2 balance: " + account2.getBalance());
                    break;
                case 5:
                    System.out.println("Exiting...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
}

class Account {
    private String owner;
    private double balance;

    public Account(String owner, double balance) {
        this.owner = owner;
        this.balance = balance;
    }

    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount; // using assignment and arithmetic operators
            System.out.println("Deposited " + amount + ". New balance: " + balance);
        } else {
            System.out.println("Invalid deposit amount.");
        }
    }

    public void withdraw(double amount) {
        if (amount > 0 && amount <= balance) { // using relational and logical operators
            balance -= amount; // using assignment and arithmetic operators
            System.out.println("Withdrew " + amount + ". New balance: " + balance);
        } else {
            System.out.println("Invalid withdrawal amount or insufficient funds.");
        }
    }

    public void transfer(Account toAccount, double amount) {
        if (amount > 0 && amount <= balance) { // using relational and logical operators
            balance -= amount; // using assignment and arithmetic operators
            toAccount.balance += amount; // using assignment and arithmetic operators
            System.out.println("Transferred " + amount + " to " + toAccount.owner + ". New balance: " + balance);
        } else {
            System.out.println("Invalid transfer amount or insufficient funds.");
        }
    }

    public double getBalance() {
        return balance;
    }
}
