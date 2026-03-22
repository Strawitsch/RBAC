package rbac.utils;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class FormatUtilsTest {
    @Test void testFormatTable() {
        String[] headers = {"Name", "Age"};
        var rows = Arrays.asList(new String[]{"Alice", "30"}, new String[]{"Bob", "25"});
        String table = FormatUtils.formatTable(headers, rows);
        assertTrue(table.contains("Name"));
        assertTrue(table.contains("Alice"));
    }

    @Test void testFormatBox() {
        String box = FormatUtils.formatBox("Hello");
        assertTrue(box.contains("Hello"));
        assertTrue(box.startsWith("+"));
    }
}
