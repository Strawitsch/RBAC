package rbac.audit;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AuditLog {
    public record AuditEntry(String timestamp, String action, String performer, String target, String details) {}

    private final List<AuditEntry> entries = new ArrayList<>();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void log(String action, String performer, String target, String details) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        entries.add(new AuditEntry(timestamp, action, performer, target, details));
    }

    public List<AuditEntry> getAll() {
        return new ArrayList<>(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        return entries.stream()
                .filter(e -> e.performer().equals(performer))
                .collect(Collectors.toList());
    }

    public List<AuditEntry> getByAction(String action) {
        return entries.stream()
                .filter(e -> e.action().equalsIgnoreCase(action))
                .collect(Collectors.toList());
    }

    public void printLog() {
        if (entries.isEmpty()) {
            System.out.println("No audit entries.");
            return;
        }
        System.out.printf("%-20s %-15s %-15s %-20s %s%n",
                "Timestamp", "Action", "Performer", "Target", "Details");
        System.out.println("----------------------------------------------------------------------------------------");
        entries.forEach(e -> System.out.printf("%-20s %-15s %-15s %-20s %s%n",
                e.timestamp(), e.action(), e.performer(), e.target(), e.details()));
    }

    public void saveToFile(String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Timestamp,Action,Performer,Target,Details");
            for (AuditEntry e : entries) {
                writer.printf("%s,%s,%s,%s,%s%n",
                        e.timestamp(), e.action(), e.performer(), e.target(), e.details());
            }
        }
    }
}
