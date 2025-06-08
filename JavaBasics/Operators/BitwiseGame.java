import java.util.Scanner;

public class BitwiseGame {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int playerFlags = 0; // All flags are initially unset

        while (true) {
            System.out.println("Bitwise Game Menu:");
            System.out.println("1. Set Flag");
            System.out.println("2. Clear Flag");
            System.out.println("3. Toggle Flag");
            System.out.println("4. Check Flag");
            System.out.println("5. Exit");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();

            System.out.print("Enter flag position (0-31): ");
            int position = scanner.nextInt();

            switch (choice) {
                case 1:
                    playerFlags |= (1 << position); // Set the flag at the given position
                    System.out.println("Flag set at position " + position);
                    break;
                case 2:
                    playerFlags &= ~(1 << position); // Clear the flag at the given position
                    System.out.println("Flag cleared at position " + position);
                    break;
                case 3:
                    playerFlags ^= (1 << position); // Toggle the flag at the given position
                    System.out.println("Flag toggled at position " + position);
                    break;
                case 4:
                    boolean isSet = (playerFlags & (1 << position)) != 0; // Check if the flag is set
                    System.out.println("Flag at position " + position + " is " + (isSet ? "set" : "not set"));
                    break;
                case 5:
                    System.out.println("Exiting...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
            System.out.println("Current flags: " + Integer.toBinaryString(playerFlags));
            System.out.println();
        }
    }
}
