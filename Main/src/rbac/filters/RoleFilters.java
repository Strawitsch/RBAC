package rbac.filters;

import rbac.core.Permission;
import rbac.core.Role;

public final class RoleFilters {
    private RoleFilters() {}

    public static RoleFilter byName(String name) {
        return role -> role.getName().equals(name);
    }

    public static RoleFilter byNameContains(String substring) {
        String lower = substring.toLowerCase();
        return role -> role.getName().toLowerCase().contains(lower);
    }

    public static RoleFilter hasPermission(Permission permission) {
        return role -> role.hasPermission(permission);
    }

    public static RoleFilter hasPermission(String permissionName, String resource) {
        return role -> role.hasPermission(permissionName, resource);
    }

    public static RoleFilter hasAtLeastNPermissions(int n) {
        return role -> role.getPermissions().size() >= n;
    }
}