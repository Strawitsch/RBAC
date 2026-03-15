package rbac.filters;

import rbac.core.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class AssignmentFilters {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private AssignmentFilters() {}

    public static AssignmentFilter byUser(User user) {
        return a -> a.user().equals(user);
    }

    public static AssignmentFilter byUsername(String username) {
        return a -> a.user().username().equals(username);
    }

    public static AssignmentFilter byRole(Role role) {
        return a -> a.role().equals(role);
    }

    public static AssignmentFilter byRoleName(String roleName) {
        return a -> a.role().getName().equals(roleName);
    }

    public static AssignmentFilter activeOnly() {
        return RoleAssignment::isActive;
    }

    public static AssignmentFilter inactiveOnly() {
        return a -> !a.isActive();
    }

    public static AssignmentFilter byType(String type) {
        return a -> a.assignmentType().equalsIgnoreCase(type);
    }

    public static AssignmentFilter assignedBy(String username) {
        return a -> a.metadata().assignedBy().equals(username);
    }

    public static AssignmentFilter assignedAfter(String date) {
        LocalDateTime threshold = LocalDateTime.parse(date, FORMATTER);
        return a -> {
            LocalDateTime assigned = LocalDateTime.parse(a.metadata().assignedAt(), FORMATTER);
            return assigned.isAfter(threshold);
        };
    }

    public static AssignmentFilter expiringBefore(String date) {
        LocalDateTime threshold = LocalDateTime.parse(date, FORMATTER);
        return a -> {
            if (a instanceof TemporaryAssignment temp) {
                return temp.isExpired() ? true : temp.getExpiresAt().isBefore(threshold);
            }
            return false;
        };
    }
}
