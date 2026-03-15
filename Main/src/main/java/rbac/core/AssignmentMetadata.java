package rbac.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static AssignmentMetadata now(String assignedBy, String reason) {
        String currentTime = LocalDateTime.now().format(FORMATTER);
        String finalReason = (reason == null || reason.isBlank()) ? "No reason provided" : reason;

        return new AssignmentMetadata(assignedBy, currentTime, finalReason);
    }

    public String format() {
        return String.format("Assigned by: %s | At: %s | Reason: %s",
                assignedBy, assignedAt, reason);
    }
}
