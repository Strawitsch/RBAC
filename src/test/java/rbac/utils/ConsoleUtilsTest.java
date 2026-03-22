package rbac.utils;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleUtilsTest {
    @Test void testPromptString() {
        String input = "test\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        Scanner scanner = new Scanner(System.in);
        String result = ConsoleUtils.promptString(scanner, "Enter", true);
        assertEquals("test", result);
    }

    @Test void testPromptInt() {
        String input = "5\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        Scanner scanner = new Scanner(System.in);
        int result = ConsoleUtils.promptInt(scanner, "Enter", 1, 10);
        assertEquals(5, result);
    }

    @Test void testPromptYesNo() {
        String input = "yes\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        Scanner scanner = new Scanner(System.in);
        assertTrue(ConsoleUtils.promptYesNo(scanner, "Confirm?"));
    }

    @Test void testPromptChoice() {
        String input = "2\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        Scanner scanner = new Scanner(System.in);
        String result = ConsoleUtils.promptChoice(scanner, "Choose", Arrays.asList("One", "Two", "Three"));
        assertEquals("Two", result);
    }
}
