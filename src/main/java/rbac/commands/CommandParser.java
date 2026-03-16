package rbac.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CommandParser {
    private final Map<String, Command> commands = new HashMap<>();
    private final Map<String, String> descriptions = new HashMap<>();

    public void registerCommand(String name, String description, Command command) {
        commands.put(name.toLowerCase(), command);
        descriptions.put(name.toLowerCase(), description);
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        Command cmd = commands.get(commandName.toLowerCase());
        if (cmd == null) {
            System.out.println("Unknown command: " + commandName);
            return;
        }
        cmd.execute(scanner, system);
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        String[] parts = input.trim().split("\\s+", 2);
        String cmdName = parts[0];
        executeCommand(cmdName, scanner, system);
    }

    public void printHelp() {
        System.out.println("Available commands:");
        commands.keySet().stream().sorted().forEach(cmd ->
                System.out.printf("  %-20s - %s%n", cmd, descriptions.get(cmd))
        );
    }
}