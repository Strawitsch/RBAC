package rbac.commands;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Scanner;

import rbac.core.User;

import static org.junit.jupiter.api.Assertions.*;

public class CommandParserTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final InputStream originalIn = System.in;

    private RBACSystem system;
    private CommandParser parser;

    @BeforeEach
    void setUp() {
        system = new RBACSystem();
        system.initialize();
        parser = new CommandParser();
        CommandRegistry.registerAll(parser, system);
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    private void provideInput(String data) {
        ByteArrayInputStream testIn = new ByteArrayInputStream(data.getBytes());
        System.setIn(testIn);
    }

    @Test
    void testHelpCommand() {
        provideInput("help\n");
        Scanner scanner = new Scanner(System.in);
        parser.parseAndExecute("help", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("Available commands:"));
        assertTrue(output.contains("help"));
    }

    @Test
    void testUnknownCommand() {
        provideInput("\n");
        Scanner scanner = new Scanner(System.in);
        parser.parseAndExecute("unknown", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("Unknown command: unknown"));
    }

    @Test
    void testUserCreateCommand() {
        String input = "john_doe\nJohn Doe\njohn@example.com\n";
        provideInput(input);
        Scanner scanner = new Scanner(System.in);
        parser.parseAndExecute("user-create", scanner, system);
        assertTrue(system.getUserManager().exists("john_doe"));
        String output = outContent.toString();
        assertTrue(output.contains("User created successfully"));
    }

    @Test
    void testUserListCommand() {
        // сначала добавим пользователя
        system.getUserManager().add(User.validate("jane", "Jane Doe", "jane@example.com"));
        provideInput("\n");
        Scanner scanner = new Scanner(System.in);
        parser.parseAndExecute("user-list", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("jane"));
        assertTrue(output.contains("admin"));
    }

    @Test
    void testStatsCommand() {
        provideInput("\n");
        Scanner scanner = new Scanner(System.in);
        parser.parseAndExecute("stats", scanner, system);
        String output = outContent.toString();
        assertTrue(output.contains("System Statistics"));
        assertTrue(output.contains("Users: 1"));
    }
}