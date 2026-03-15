package rbac.filters;

import rbac.core.RoleAssignment;
import java.util.Objects;
import java.util.function.Predicate;

@FunctionalInterface
public interface AssignmentFilter extends Predicate<RoleAssignment> {
    default AssignmentFilter and(AssignmentFilter other) {
        Objects.requireNonNull(other);
        return a -> test(a) && other.test(a);
    }

    default AssignmentFilter or(AssignmentFilter other) {
        Objects.requireNonNull(other);
        return a -> test(a) || other.test(a);
    }
}
