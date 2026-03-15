package rbac.filters;

import rbac.core.Role;
import java.util.Objects;
import java.util.function.Predicate;

@FunctionalInterface
public interface RoleFilter extends Predicate<Role> {
    default RoleFilter and(RoleFilter other) {
        Objects.requireNonNull(other);
        return role -> test(role) && other.test(role);
    }

    default RoleFilter or(RoleFilter other) {
        Objects.requireNonNull(other);
        return role -> test(role) || other.test(role);
    }
}