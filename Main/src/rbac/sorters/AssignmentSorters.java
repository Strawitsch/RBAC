package rbac.sorters;

import rbac.core.RoleAssignment;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

public final class AssignmentSorters {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private AssignmentSorters() {}

    public static Comparator<RoleAssignment> byUsername() {
        return Comparator.comparing(a -> a.user().username(), String.CASE_INSENSITIVE_ORDER);
    }

    public static Comparator<RoleAssignment> byRoleName() {
        return Comparator.comparing(a -> a.role().getName(), String.CASE_INSENSITIVE_ORDER);
    }

    public static Comparator<RoleAssignment> byAssignmentDate() {
        return Comparator.comparing(a -> LocalDateTime.parse(a.metadata().assignedAt(), FORMATTER));
    }
}
