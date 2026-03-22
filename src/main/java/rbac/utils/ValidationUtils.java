package rbac.utils;

import java.util.regex.Pattern;

public final class ValidationUtils {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private ValidationUtils() {}

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidDate(String date) {
        return date != null && DATE_PATTERN.matcher(date).matches();
    }

    public static String normalizeString(String input) {
        if (input == null) return null;
        return input.trim().replaceAll("\\s+", " ");
    }

    public static void requireNonNullEmpty(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }
}