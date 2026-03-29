package rbac.managers;

import rbac.core.Permission;
import rbac.core.Role;
import rbac.exceptions.*;
import rbac.filters.RoleFilter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class RoleManager implements Repository<Role> {
    private final Map<String, Role> rolesById = new ConcurrentHashMap<>();
    private final Map<String, Role> rolesByName = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private AssignmentManager assignmentManager;

    public void setAssignmentManager(AssignmentManager assignmentManager) {
        this.assignmentManager = assignmentManager;
    }

    @Override
    public void add(Role role) {
        Objects.requireNonNull(role, "Role cannot be null");
        lock.writeLock().lock();
        try{
            if (rolesById.containsKey(role.getId())) {
                throw new DuplicateRoleException("Role with id " + role.getId() + " already exists");
            }
            if (rolesByName.containsKey(role.getName())) {
                throw new DuplicateRoleException("Role with name " + role.getName() + " already exists");
            }
            rolesById.put(role.getId(), role);
            rolesByName.put(role.getName(), role);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(Role role) {
        Objects.requireNonNull(role, "Role cannot be null");
        lock.writeLock().lock();
        try{
            if (assignmentManager != null && !assignmentManager.findByRole(role).isEmpty()) {
                throw new RoleInUseException("Role " + role.getName() + " is currently assigned to users");
            }
            boolean removed = rolesById.remove(role.getId(), role);
            if (removed) {
                rolesByName.remove(role.getName(), role);
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateRole(String oldName, String newName, String newDescription) {
        lock.writeLock().lock();
        try{
            Role role = rolesByName.get(oldName);
            if (role == null) {
                throw new RoleNotFoundException("Role " + oldName + " not found");
            }

            if (newDescription == null){
                throw new DescriptionNotFoundException("Description cannot be null");
            }

            if (!oldName.equals(newName) && rolesByName.containsKey(newName)) {
                throw new DuplicateRoleException("Role with name " + newName + " already exists");
            }

            if (!oldName.equals(newName)) {
                rolesByName.remove(oldName);
            }

            role.setName(newName);
            role.setDescription(newDescription);

            if (!oldName.equals(newName)) {
                rolesByName.put(newName, role);
            }
        } finally {
            lock.writeLock().unlock();
        }

    }

    @Override
    public Optional<Role> findById(String id) {
        lock.readLock().lock();
        try{
            return Optional.ofNullable(rolesById.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<Role> findByName(String name) {
        return Optional.ofNullable(rolesByName.get(name));
    }

    @Override
    public List<Role> findAll() {
        lock.readLock().lock();
        try{
            return new ArrayList<>(rolesById.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Role> findByFilter(RoleFilter filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        lock.readLock().lock();
        try{
            return rolesById.values().stream()
                    .filter(filter)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(sorter, "Comparator cannot be null");
        lock.readLock().lock();
        try{
            return rolesById.values().stream()
                    .filter(filter)
                    .sorted(sorter)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean exists(String name) {
            return rolesByName.containsKey(name);
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        lock.writeLock().lock();
        try{
            Role role = rolesByName.get(roleName);
            if (role == null) {
                throw new RoleNotFoundException("Role " + roleName + " not found");
            }
            role.addPermission(permission);
        } finally {
            lock.writeLock().unlock();
        }

    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        lock.writeLock().lock();
        try{
            Role role = rolesByName.get(roleName);
            if (role == null) {
                throw new RoleNotFoundException("Role " + roleName + " not found");
            }
            role.removePermission(permission);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        lock.readLock().lock();
        try{
            return rolesById.values().stream()
                    .filter(r -> r.hasPermission(permissionName, resource))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int count() {
        return rolesById.size();
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try{
            rolesById.clear();
            rolesByName.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}