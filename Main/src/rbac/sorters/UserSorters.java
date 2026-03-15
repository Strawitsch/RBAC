package rbac.sorters;

import rbac.core.User;
import java.util.Comparator;

public final class UserSorters {
    private UserSorters() {}

    public static Comparator<User> byUsername() {
        return Comparator.comparing(User::username, String.CASE_INSENSITIVE_ORDER);
    }

    public static Comparator<User> byFullName() {
        return Comparator.comparing(User::fullName, String.CASE_INSENSITIVE_ORDER);
    }

    public static Comparator<User> byEmail() {
        return Comparator.comparing(User::email, String.CASE_INSENSITIVE_ORDER);
    }
}