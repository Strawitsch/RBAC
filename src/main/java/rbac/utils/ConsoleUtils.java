package rbac.utils;

import java.util.List;
import java.util.Scanner;

public final class ConsoleUtils {
    private ConsoleUtils() {}

    public static String promptString(Scanner scanner, String message, boolean required) {
        while (true) {
            System.out.print(message + (required ? " (required): " : " (optional, press Enter to skip): "));
            String input = scanner.nextLine().trim();
            if (!required && input.isEmpty()) return null;
            if (required && input.isEmpty()) {
                System.out.println("Input is required. Please enter a value.");
                continue;
            }
            return input;
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(message + " (range " + min + "-" + max + "): ");
            try {
                int val = Integer.parseInt(scanner.nextLine().trim());
                if (val < min || val > max) {
                    System.out.println("Value out of range. Try again.");
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Try again.");
            }
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        while (true) {
            System.out.print(message + " (yes/no): ");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("yes")) return true;
            if (input.equals("no")) return false;
            System.out.println("Please enter 'yes' or 'no'.");
        }
    }

    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (options.isEmpty()) return null;
        while (true) {
            System.out.println(message);
            for (int i = 0; i < options.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, options.get(i));
            }
            System.out.print("Your choice (1-" + options.size() + "): ");
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice >= 1 && choice <= options.size()) {
                    return options.get(choice - 1);
                }
                System.out.println("Invalid choice. Try again.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Try again.");
            }
        }
    }
}