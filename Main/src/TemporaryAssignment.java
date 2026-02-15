import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;

public class TemporaryAssignment extends AbstractRoleAssignment {
    private LocalDateTime expiresAt;
    boolean autoRenew;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata,
                               String expirationDate, boolean autoRenew) {
        super(user, role, metadata);
        this.expiresAt = LocalDateTime.parse(expirationDate, FORMATTER);
        this.autoRenew = autoRenew;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void extend(String newExpirationDate) {
        this.expiresAt = LocalDateTime.parse(newExpirationDate, FORMATTER);
    }

    @Override
    public boolean isActive() {
        return !isExpired();
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public String getTimeRemaining() {
        if (isExpired()) return "Expired";

        Duration duration = Duration.between(LocalDateTime.now(), expiresAt);
        long days = duration.toDays();
        long hours = duration.toHoursPart();

        return String.format("%d days, %d hours remaining", days, hours);
    }

    @Override
    public String summary() {
        String baseSummary = super.summary();
        return String.format("%s\nExpires at: %s (Auto-renew: %b)\nTime left: %s",
                baseSummary,
                expiresAt.format(FORMATTER),
                autoRenew,
                getTimeRemaining());
    }
}
