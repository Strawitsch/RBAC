package rbac.audit;

import org.junit.jupiter.api.Test;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {
    @Test void testLogAndRetrieve() throws IOException {
        AuditLog log = new AuditLog();
        log.log("TEST", "user", "target", "details");
        assertEquals(1, log.getAll().size());
        assertEquals("TEST", log.getAll().get(0).action());
        assertEquals("user", log.getByPerformer("user").get(0).performer());

        File temp = File.createTempFile("audit", ".csv");
        log.saveToFile(temp.getAbsolutePath());
        assertTrue(temp.exists());
        temp.delete();
    }
}