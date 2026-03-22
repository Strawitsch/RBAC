package rbac.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {
    @Test void testValidUsername() {
        assertTrue(ValidationUtils.isValidUsername("john_doe"));
        assertTrue(ValidationUtils.isValidUsername("user123"));
        assertFalse(ValidationUtils.isValidUsername("ab"));
        assertFalse(ValidationUtils.isValidUsername("too_long_username_more_than_20"));
    }
    @Test void testValidEmail() {
        assertTrue(ValidationUtils.isValidEmail("user@example.com"));
        assertFalse(ValidationUtils.isValidEmail("user@example"));
        assertFalse(ValidationUtils.isValidEmail("user@.com"));
    }
    @Test void testNormalizeString() {
        assertEquals("hello world", ValidationUtils.normalizeString("  hello   world  "));
        assertNull(ValidationUtils.normalizeString(null));
    }
}