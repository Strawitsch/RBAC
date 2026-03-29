package rbac.commands;

import rbac.core.*;
import rbac.managers.*;
import rbac.audit.AuditLog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.stream.Collectors;

public class RBACSystem {
    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;
    private final AuditLog auditLog;
    private String currentUser;

    public RBACSystem() {
        this.userManager = new UserManager();
        this.roleManager = new RoleManager();
        this.assignmentManager = new AssignmentManager(userManager, roleManager);
        this.roleManager.setAssignmentManager(assignmentManager);
        this.auditLog = new AuditLog();
    }

    public UserManager getUserManager() { return userManager; }
    public RoleManager getRoleManager() { return roleManager; }
    public AssignmentManager getAssignmentManager() { return assignmentManager; }
    public AuditLog getAuditLog() { return auditLog; }
    public String getCurrentUser() { return currentUser; }
    public void setCurrentUser(String username) { this.currentUser = username; }
    private final ExecutorService backgroundExecutor = Executors.newCachedThreadPool();

    public ExecutorService getBackgroundExecutor() { return backgroundExecutor; }

    public void shutdown() {
        backgroundExecutor.shutdown();
        auditLog.shutdown();
    }

    public void initialize() {
        Permission readUsers = new Permission("READ", "users", "Can view user list");
        Permission writeUsers = new Permission("WRITE", "users", "Can edit users");
        Permission deleteUsers = new Permission("DELETE", "users", "Can remove users");
        Permission readRoles = new Permission("READ", "roles", "Can view roles");
        Permission writeRoles = new Permission("WRITE", "roles", "Can create/edit roles");
        Permission deleteRoles = new Permission("DELETE", "roles", "Can delete roles");
        Permission readAssignments = new Permission("READ", "assignments", "Can view assignments");
        Permission writeAssignments = new Permission("WRITE", "assignments", "Can assign/revoke roles");

        Role adminRole = new Role("Admin", "Full system access");
        adminRole.addPermission(readUsers);
        adminRole.addPermission(writeUsers);
        adminRole.addPermission(deleteUsers);
        adminRole.addPermission(readRoles);
        adminRole.addPermission(writeRoles);
        adminRole.addPermission(deleteRoles);
        adminRole.addPermission(readAssignments);
        adminRole.addPermission(writeAssignments);

        Role managerRole = new Role("Manager", "Can manage users and assignments");
        managerRole.addPermission(readUsers);
        managerRole.addPermission(writeUsers);
        managerRole.addPermission(readRoles);
        managerRole.addPermission(readAssignments);
        managerRole.addPermission(writeAssignments);

        Role viewerRole = new Role("Viewer", "Read-only access");
        viewerRole.addPermission(readUsers);
        viewerRole.addPermission(readRoles);
        viewerRole.addPermission(readAssignments);

        roleManager.add(adminRole);
        roleManager.add(managerRole);
        roleManager.add(viewerRole);

        User admin = User.validate("admin", "System Administrator", "admin@system.local");
        userManager.add(admin);

        AssignmentMetadata meta = AssignmentMetadata.now("system", "Initial admin assignment");
        PermanentAssignment assignment = new PermanentAssignment(admin, adminRole, meta);
        assignmentManager.add(assignment);

        this.currentUser = admin.username();
        auditLog.log("SYSTEM_INIT", "system", "system", "System initialized");
    }

    public String generateStatistics() {
        int userCount = userManager.count();
        int roleCount = roleManager.count();
        int assignmentCount = assignmentManager.count();
        long activeAssignments = assignmentManager.getActiveAssignments().size();
        long expiredAssignments = assignmentManager.getExpiredAssignments().size();
        double avgRolesPerUser = userCount == 0 ? 0 :
                assignmentManager.findAll().stream()
                        .map(a -> a.user().username())
                        .distinct()
                        .count() / (double) userCount;

        Map<String, Long> roleCountMap = assignmentManager.findAll().stream()
                .filter(RoleAssignment::isActive)
                .collect(Collectors.groupingBy(a -> a.role().getName(), Collectors.counting()));
        String topRoles = roleCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .collect(Collectors.joining(", "));

        return String.format("""
                === System Statistics ===
                Users: %d
                Roles: %d
                Assignments: total %d | active %d | expired %d
                Avg roles per user: %.2f
                Top roles: %s
                """, userCount, roleCount, assignmentCount, activeAssignments, expiredAssignments,
                avgRolesPerUser, topRoles.isEmpty() ? "none" : topRoles);
    }
}