package rbac.sorters;

import rbac.core.Role;
import java.util.Comparator;

public final class RoleSorters {
    private RoleSorters() {}

    public static Comparator<Role> byName() {
        return Comparator.comparing(Role::getName, String.CASE_INSENSITIVE_ORDER);
    }

    public static Comparator<Role> byPermissionCount() {
        return Comparator.comparingInt(r -> r.getPermissions().size());
    }
}
