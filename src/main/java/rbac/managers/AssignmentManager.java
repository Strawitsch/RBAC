package rbac.managers;

import rbac.core.*;
import rbac.exceptions.*;
import rbac.filters.AssignmentFilter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class AssignmentManager implements Repository<RoleAssignment> {
    private final Map<String, RoleAssignment> assignments = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
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
        lock.writeLock().lock();
        try {
            boolean alreadyAssigned = assignments.values().stream()
                    .anyMatch(a -> a.user().equals(assignment.user())
                            && a.role().equals(assignment.role())
                            && a.isActive());
            if (alreadyAssigned) {
                throw new DuplicateAssignmentException("User already has active assignment for role " + assignment.role().getName());
            }
            assignments.put(assignment.assignmentId(), assignment);
        } finally {
           lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        Objects.requireNonNull(assignment, "Assignment cannot be null");
        lock.writeLock().lock();
        try {
            return assignments.remove(assignment.assignmentId(), assignment);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(assignments.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<RoleAssignment> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(assignments.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findByUser(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(a -> a.user().equals(user))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }

    }

    public List<RoleAssignment> findByRole(Role role) {
        Objects.requireNonNull(role, "Role cannot be null");
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(a -> a.role().equals(role))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(filter)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findByFilterParallel(AssignmentFilter filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        lock.readLock().lock();
        try {
            return assignments.values().parallelStream()
                    .filter(filter)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(sorter, "Comparator cannot be null");
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(filter)
                    .sorted(sorter)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> getActiveAssignments() {
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(RoleAssignment::isActive)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> getExpiredAssignments() {
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(a -> !a.isActive())
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean userHasRole(User user, Role role) {
        Objects.requireNonNull(user, "User cannot be null");
        Objects.requireNonNull(role, "Role cannot be null");
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .anyMatch(a -> a.user().equals(user) && a.role().equals(role) && a.isActive());
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        Objects.requireNonNull(user, "User cannot be null");
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(a -> a.user().equals(user) && a.isActive())
                    .map(RoleAssignment::role)
                    .anyMatch(role -> role.hasPermission(permissionName, resource));
        } finally {
            lock.readLock().unlock();
        }
    }

    public Set<Permission> getUserPermissions(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(a -> a.user().equals(user) && a.isActive())
                    .flatMap(a -> a.role().getPermissions().stream())
                    .collect(Collectors.toSet());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void revokeAssignment(String assignmentId) {
        lock.writeLock().lock();
        try {
            RoleAssignment assignment = assignments.get(assignmentId);
            if (assignment == null) {
                throw new AssignmentNotFoundException("Assignment " + assignmentId + " not found");
            }
            if (assignment instanceof PermanentAssignment perm) {
                perm.revoke();
            } else if (assignment instanceof TemporaryAssignment) {
                assignments.remove(assignmentId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        lock.writeLock().lock();
        try {
            RoleAssignment assignment = assignments.get(assignmentId);
            if (assignment == null) {
                throw new AssignmentNotFoundException("Assignment " + assignmentId + " not found");
            }
            if (!(assignment instanceof TemporaryAssignment temp)) {
                throw new IllegalArgumentException("Assignment is not temporary");
            }
            temp.extend(newExpirationDate);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int count() {
            return assignments.size();
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            assignments.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
