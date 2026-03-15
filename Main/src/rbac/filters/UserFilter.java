package rbac.filters;

import rbac.core.User;
import java.util.Objects;
import java.util.function.Predicate;

@FunctionalInterface
public interface UserFilter extends Predicate<User> {
    default UserFilter and(UserFilter other) {
        Objects.requireNonNull(other);
        return user -> test(user) && other.test(user);
    }

    default UserFilter or(UserFilter other) {
        Objects.requireNonNull(other);
        return user -> test(user) || other.test(user);
    }
}
