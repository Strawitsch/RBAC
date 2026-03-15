package rbac.managers;

import rbac.core.Permission;
import rbac.core.Role;
import rbac.exceptions.*;
import rbac.filters.RoleFilter;

import java.util.*;
import java.util.stream.Collectors;

public class RoleManager implements Repository<Role> {
    private final Map<String, Role> rolesById = new HashMap<>();
    private final Map<String, Role> rolesByName = new HashMap<>();
    private AssignmentManager assignmentManager;

    public void setAssignmentManager(AssignmentManager assignmentManager) {
        this.assignmentManager = assignmentManager;
    }

    @Override
    public void add(Role role) {
        Objects.requireNonNull(role, "Role cannot be null");
        if (rolesById.containsKey(role.getId())) {
            throw new DuplicateRoleException("Role with id " + role.getId() + " already exists");
        }
        if (rolesByName.containsKey(role.getName())) {
            throw new DuplicateRoleException("Role with name " + role.getName() + " already exists");
        }
        rolesById.put(role.getId(), role);
        rolesByName.put(role.getName(), role);
    }

    @Override
    public boolean remove(Role role) {
        Objects.requireNonNull(role, "Role cannot be null");
        // Проверка, назначена ли роль
        if (assignmentManager != null && !assignmentManager.findByRole(role).isEmpty()) {
            throw new RoleInUseException("Role " + role.getName() + " is currently assigned to users");
        }
        boolean removed = rolesById.remove(role.getId(), role);
        if (removed) {
            rolesByName.remove(role.getName(), role);
        }
        return removed;
    }

    @Override
    public Optional<Role> findById(String id) {
        return Optional.ofNullable(rolesById.get(id));
    }

    public Optional<Role> findByName(String name) {
        return Optional.ofNullable(rolesByName.get(name));
    }

    @Override
    public List<Role> findAll() {
        return new ArrayList<>(rolesById.values());
    }

    public List<Role> findByFilter(RoleFilter filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        return rolesById.values().stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(sorter, "Comparator cannot be null");
        return rolesById.values().stream()
                .filter(filter)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public boolean exists(String name) {
        return rolesByName.containsKey(name);
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        Role role = rolesByName.get(roleName);
        if (role == null) {
            throw new RoleNotFoundException("Role " + roleName + " not found");
        }
        role.addPermission(permission);
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        Role role = rolesByName.get(roleName);
        if (role == null) {
            throw new RoleNotFoundException("Role " + roleName + " not found");
        }
        role.removePermission(permission);
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        return rolesById.values().stream()
                .filter(r -> r.hasPermission(permissionName, resource))
                .collect(Collectors.toList());
    }

    @Override
    public int count() {
        return rolesById.size();
    }

    @Override
    public void clear() {
        rolesById.clear();
        rolesByName.clear();
    }
}