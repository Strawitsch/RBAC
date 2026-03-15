package rbac.managers;

import rbac.core.*;
import rbac.exceptions.*;
import rbac.filters.AssignmentFilter;

import java.util.*;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {
    private final Map<String, RoleAssignment> assignments = new HashMap<>();
    private final UserManager userManager;
    private final RoleManager roleManager;

    public AssignmentManager(UserManager userManager, RoleManager roleManager) {
        this.userManager = userManager;
        this.roleManager = roleManager;
    }

    @Override
    public void add(RoleAssignment assignment) {
        Objects.requireNonNull(assignment, "Assignment cannot be null");
        if (!userManager.exists(assignment.user().username())) {
            throw new UserNotFoundException("User " + assignment.user().username() + " not found in system");
        }
        if (!roleManager.exists(assignment.role().getName())) {
            throw new RoleNotFoundException("Role " + assignment.role().getName() + " not found in system");
        }
        boolean alreadyAssigned = assignments.values().stream()
                .anyMatch(a -> a.user().equals(assignment.user())
                        && a.role().equals(assignment.role())
                        && a.isActive());
        if (alreadyAssigned) {
            throw new DuplicateAssignmentException("User already has active assignment for role " + assignment.role().getName());
        }
        assignments.put(assignment.assignmentId(), assignment);
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        Objects.requireNonNull(assignment, "Assignment cannot be null");
        return assignments.remove(assignment.assignmentId(), assignment);
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        return Optional.ofNullable(assignments.get(id));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return new ArrayList<>(assignments.values());
    }

    public List<RoleAssignment> findByUser(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        return assignments.values().stream()
                .filter(a -> a.user().equals(user))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByRole(Role role) {
        Objects.requireNonNull(role, "Role cannot be null");
        return assignments.values().stream()
                .filter(a -> a.role().equals(role))
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        return assignments.values().stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(sorter, "Comparator cannot be null");
        return assignments.values().stream()
                .filter(filter)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getActiveAssignments() {
        return assignments.values().stream()
                .filter(RoleAssignment::isActive)
                .collect(Collectors.toList());
    }

    public List<RoleAssignment> getExpiredAssignments() {
        return assignments.values().stream()
                .filter(a -> !a.isActive())
                .collect(Collectors.toList());
    }

    public boolean userHasRole(User user, Role role) {
        Objects.requireNonNull(user, "User cannot be null");
        Objects.requireNonNull(role, "Role cannot be null");
        return assignments.values().stream()
                .anyMatch(a -> a.user().equals(user) && a.role().equals(role) && a.isActive());
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        Objects.requireNonNull(user, "User cannot be null");
        return assignments.values().stream()
                .filter(a -> a.user().equals(user) && a.isActive())
                .map(RoleAssignment::role)
                .anyMatch(role -> role.hasPermission(permissionName, resource));
    }

    public Set<Permission> getUserPermissions(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        return assignments.values().stream()
                .filter(a -> a.user().equals(user) && a.isActive())
                .flatMap(a -> a.role().getPermissions().stream())
                .collect(Collectors.toSet());
    }

    public void revokeAssignment(String assignmentId) {
        RoleAssignment assignment = assignments.get(assignmentId);
        if (assignment == null) {
            throw new AssignmentNotFoundException("Assignment " + assignmentId + " not found");
        }
        if (assignment instanceof PermanentAssignment perm) {
            perm.revoke();
        } else if (assignment instanceof TemporaryAssignment) {
            assignments.remove(assignmentId);
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        RoleAssignment assignment = assignments.get(assignmentId);
        if (assignment == null) {
            throw new AssignmentNotFoundException("Assignment " + assignmentId + " not found");
        }
        if (!(assignment instanceof TemporaryAssignment temp)) {
            throw new IllegalArgumentException("Assignment is not temporary");
        }
        temp.extend(newExpirationDate);
    }

    @Override
    public int count() {
        return assignments.size();
    }

    @Override
    public void clear() {
        assignments.clear();
    }
}
