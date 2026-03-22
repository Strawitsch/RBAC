package rbac.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class DateUtils {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtils() {}

    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMAT);
    }

    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATETIME_FORMAT);
    }

    public static boolean isBefore(String date1, String date2) {
        return LocalDate.parse(date1).isBefore(LocalDate.parse(date2));
    }

    public static boolean isAfter(String date1, String date2) {
        return LocalDate.parse(date1).isAfter(LocalDate.parse(date2));
    }

    public static String addDays(String date, int days) {
        return LocalDate.parse(date).plusDays(days).format(DATE_FORMAT);
    }

    public static String formatRelativeTime(String date) {
        LocalDate d = LocalDate.parse(date);
        LocalDate now = LocalDate.now();
        long days = ChronoUnit.DAYS.between(now, d);
        if (days < 0) {
            return (-days) + " days ago";
        } else if (days == 0) {
            return "today";
        } else {
            return "in " + days + " days";
        }
    }
}