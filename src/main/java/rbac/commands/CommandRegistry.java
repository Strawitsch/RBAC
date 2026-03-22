package rbac.commands;

import rbac.core.*;
import rbac.exceptions.*;
import rbac.filters.*;
import rbac.sorters.*;
import rbac.utils.*;
import rbac.reports.ReportGenerator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class CommandRegistry {

    public static void registerAll(CommandParser parser, RBACSystem system) {
        // ---------- USER COMMANDS ----------
        parser.registerCommand("user-list", "List all users (with optional filters)", (scanner, sys) -> {
            List<User> users = sys.getUserManager().findAll();
            printUserTable(users);
        });

        parser.registerCommand("user-create", "Create a new user", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username", true);
            if (username == null) return;
            if (!ValidationUtils.isValidUsername(username)) {
                System.out.println("Invalid username format. Use 3-20 letters, digits, underscores.");
                return;
            }
            String fullName = ConsoleUtils.promptString(scanner, "Enter full name", true);
            if (fullName == null) return;
            String email = ConsoleUtils.promptString(scanner, "Enter email", true);
            if (email == null) return;
            if (!ValidationUtils.isValidEmail(email)) {
                System.out.println("Invalid email format.");
                return;
            }
            try {
                User user = User.validate(username, fullName, email);
                sys.getUserManager().add(user);
                sys.getAuditLog().log("USER_CREATE", sys.getCurrentUser(), username, "User created");
                System.out.println("User created successfully.");
            } catch (IllegalArgumentException | DuplicateUserException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("user-view", "View user details", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username", true);
            if (username == null) return;
            Optional<User> opt = sys.getUserManager().findByUsername(username);
            if (opt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }
            User user = opt.get();
            System.out.println("User: " + user.format());
            List<RoleAssignment> assignments = sys.getAssignmentManager().findByUser(user);
            if (assignments.isEmpty()) {
                System.out.println("No role assignments.");
            } else {
                System.out.println("Assigned roles:");
                assignments.forEach(a -> {
                    String status = a.isActive() ? "ACTIVE" : "INACTIVE";
                    System.out.printf("  - %s [%s] (%s) by %s at %s%n",
                            a.role().getName(), a.assignmentType(), status,
                            a.metadata().assignedBy(), a.metadata().assignedAt());
                });
                Set<Permission> perms = sys.getAssignmentManager().getUserPermissions(user);
                System.out.println("Total permissions: " + perms.size());
                perms.forEach(p -> System.out.println("  - " + p.format()));
            }
        });

        parser.registerCommand("user-update", "Update user details", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username", true);
            if (username == null) return;
            Optional<User> opt = sys.getUserManager().findByUsername(username);
            if (opt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }
            String fullName = ConsoleUtils.promptString(scanner, "Enter new full name", true);
            if (fullName == null) return;
            String email = ConsoleUtils.promptString(scanner, "Enter new email", true);
            if (email == null) return;
            try {
                sys.getUserManager().update(username, fullName, email);
                sys.getAuditLog().log("USER_UPDATE", sys.getCurrentUser(), username, "User updated");
                System.out.println("User updated successfully.");
            } catch (IllegalArgumentException | UserNotFoundException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("user-delete", "Delete a user", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username", true);
            if (username == null) return;
            Optional<User> opt = sys.getUserManager().findByUsername(username);
            if (opt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }
            User user = opt.get();
            if (!ConsoleUtils.promptYesNo(scanner, "Are you sure you want to delete user " + username + "?")) {
                System.out.println("Deletion cancelled.");
                return;
            }

            List<RoleAssignment> assignments = sys.getAssignmentManager().findByUser(user);
            assignments.forEach(a -> sys.getAssignmentManager().remove(a));
            if (sys.getUserManager().remove(user)) {
                sys.getAuditLog().log("USER_DELETE", sys.getCurrentUser(), username, "User deleted");
                System.out.println("User deleted successfully.");
            } else {
                System.out.println("Failed to delete user.");
            }
        });

        parser.registerCommand("user-search", "Search users by filters", (scanner, sys) -> {
            List<String> options = Arrays.asList(
                    "Username contains",
                    "Email contains",
                    "Email domain",
                    "Full name contains"
            );
            String choice = ConsoleUtils.promptChoice(scanner, "Select filter:", options);
            if (choice == null) return;
            UserFilter filter = null;
            switch (options.indexOf(choice)) {
                case 0:
                    String sub = ConsoleUtils.promptString(scanner, "Enter substring", true);
                    if (sub != null) filter = UserFilters.byUsernameContains(sub);
                    break;
                case 1:
                    sub = ConsoleUtils.promptString(scanner, "Enter email substring", true);
                    if (sub != null) filter = user -> user.email().toLowerCase().contains(sub.toLowerCase());
                    break;
                case 2:
                    String domain = ConsoleUtils.promptString(scanner, "Enter domain (e.g. @company.com)", true);
                    if (domain != null) filter = UserFilters.byEmailDomain(domain);
                    break;
                case 3:
                    sub = ConsoleUtils.promptString(scanner, "Enter name substring", true);
                    if (sub != null) filter = UserFilters.byFullNameContains(sub);
                    break;
            }
            if (filter == null) return;
            List<User> results = sys.getUserManager().findByFilter(filter);
            if (results.isEmpty()) {
                System.out.println("No users found.");
            } else {
                printUserTable(results);
            }
        });

        // ---------- ROLE COMMANDS ----------
        parser.registerCommand("role-list", "List all roles", (scanner, sys) -> {
            List<Role> roles = sys.getRoleManager().findAll();
            System.out.println("Roles:");
            roles.forEach(r -> System.out.printf("  %s [%s] (%d permissions)%n",
                    r.getName(), r.getId(), r.getPermissions().size()));
        });

        parser.registerCommand("role-create", "Create a new role", (scanner, sys) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter role name", true);
            if (name == null) return;
            String desc = ConsoleUtils.promptString(scanner, "Enter description", false);
            Role role = new Role(name, desc == null ? "" : desc);
            try {
                sys.getRoleManager().add(role);
                sys.getAuditLog().log("ROLE_CREATE", sys.getCurrentUser(), name, "Role created");
                System.out.println("Role created successfully.");
                addPermissionsToRoleInteractive(scanner, sys, role);
            } catch (DuplicateRoleException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("role-view", "View role details", (scanner, sys) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter role name", true);
            if (name == null) return;
            Optional<Role> opt = sys.getRoleManager().findByName(name);
            if (opt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }
            System.out.println(opt.get().format());
        });

        parser.registerCommand("role-update", "Обновить название и описание роли", (scanner, sys) -> {
            String oldName = ConsoleUtils.promptString(scanner, "Введите имя роли для обновления", true);
            if (oldName == null) return;
            Role role = sys.getRoleManager().findByName(oldName).orElse(null);
            if (role == null) {
                System.out.println("Роль с именем '" + oldName + "' не найдена.");
                return;
            }
            String newName = ConsoleUtils.promptString(scanner, "Введите новое название роли (Enter - оставить без изменений)", false);
            if (newName == null) newName = oldName;
            String newDesc = ConsoleUtils.promptString(scanner, "Введите новое описание роли (Enter - оставить без изменений)", false);
            if (newDesc == null) newDesc = role.getDescription();
            try {
                sys.getRoleManager().updateRole(oldName, newName, newDesc);
                sys.getAuditLog().log("ROLE_UPDATE", sys.getCurrentUser(), oldName + "->" + newName, "Role updated");
                System.out.println("Роль успешно обновлена.");
            } catch (Exception e) {
                System.out.println("Ошибка при обновлении роли: " + e.getMessage());
            }
        });

        parser.registerCommand("role-delete", "Delete a role", (scanner, sys) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter role name", true);
            if (name == null) return;
            Optional<Role> opt = sys.getRoleManager().findByName(name);
            if (opt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }
            Role role = opt.get();
            List<RoleAssignment> assignments = sys.getAssignmentManager().findByRole(role);
            if (!assignments.isEmpty()) {
                System.out.println("Role is assigned to the following users:");
                assignments.stream().map(a -> a.user().username()).distinct().forEach(u -> System.out.println("  - " + u));
                if (!ConsoleUtils.promptYesNo(scanner, "Are you sure you want to delete it anyway?")) {
                    System.out.println("Deletion cancelled.");
                    return;
                }
                assignments.forEach(a -> sys.getAssignmentManager().remove(a));
            }
            if (sys.getRoleManager().remove(role)) {
                sys.getAuditLog().log("ROLE_DELETE", sys.getCurrentUser(), name, "Role deleted");
                System.out.println("Role deleted successfully.");
            } else {
                System.out.println("Failed to delete role.");
            }
        });

        parser.registerCommand("role-add-permission", "Add permission to role", (scanner, sys) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter role name", true);
            if (name == null) return;
            Optional<Role> opt = sys.getRoleManager().findByName(name);
            if (opt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }
            String permName = ConsoleUtils.promptString(scanner, "Enter permission name", true);
            if (permName == null) return;
            String resource = ConsoleUtils.promptString(scanner, "Enter resource", true);
            if (resource == null) return;
            String desc = ConsoleUtils.promptString(scanner, "Enter description", false);
            try {
                Permission perm = new Permission(permName, resource, desc == null ? "" : desc);
                sys.getRoleManager().addPermissionToRole(name, perm);
                sys.getAuditLog().log("PERMISSION_ADD", sys.getCurrentUser(), name + ":" + permName, "Permission added");
                System.out.println("Permission added.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("role-remove-permission", "Remove permission from role", (scanner, sys) -> {
            String name = ConsoleUtils.promptString(scanner, "Enter role name", true);
            if (name == null) return;
            Optional<Role> opt = sys.getRoleManager().findByName(name);
            if (opt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }
            Role role = opt.get();
            Set<Permission> perms = role.getPermissions();
            if (perms.isEmpty()) {
                System.out.println("Role has no permissions.");
                return;
            }
            List<Permission> permList = new ArrayList<>(perms);
            Permission toRemove = ConsoleUtils.promptChoice(scanner, "Select permission to remove:", permList);
            if (toRemove == null) return;
            try {
                sys.getRoleManager().removePermissionFromRole(name, toRemove);
                sys.getAuditLog().log("PERMISSION_REMOVE", sys.getCurrentUser(), name + ":" + toRemove.name(), "Permission removed");
                System.out.println("Permission removed.");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("role-search", "Search roles", (scanner, sys) -> {
            List<String> options = Arrays.asList(
                    "Name contains",
                    "Has specific permission",
                    "At least N permissions"
            );
            String choice = ConsoleUtils.promptChoice(scanner, "Select filter:", options);
            if (choice == null) return;
            RoleFilter filter = null;
            switch (options.indexOf(choice)) {
                case 0:
                    String sub = ConsoleUtils.promptString(scanner, "Enter substring", true);
                    if (sub != null) filter = RoleFilters.byNameContains(sub);
                    break;
                case 1:
                    String pName = ConsoleUtils.promptString(scanner, "Enter permission name", true);
                    if (pName == null) return;
                    String res = ConsoleUtils.promptString(scanner, "Enter resource", true);
                    if (res == null) return;
                    filter = RoleFilters.hasPermission(pName, res);
                    break;
                case 2:
                    int n = ConsoleUtils.promptInt(scanner, "Enter minimum number of permissions", 1, 100);
                    filter = RoleFilters.hasAtLeastNPermissions(n);
                    break;
            }
            if (filter == null) return;
            List<Role> results = sys.getRoleManager().findByFilter(filter);
            if (results.isEmpty()) {
                System.out.println("No roles found.");
            } else {
                results.forEach(r -> System.out.printf("  %s (%d permissions)%n", r.getName(), r.getPermissions().size()));
            }
        });

        // ---------- ASSIGNMENT COMMANDS ----------
        parser.registerCommand("assign-role", "Assign a role to a user", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username", true);
            if (username == null) return;
            Optional<User> userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }
            User user = userOpt.get();

            List<Role> roles = sys.getRoleManager().findAll();
            if (roles.isEmpty()) {
                System.out.println("No roles available.");
                return;
            }
            Role role = ConsoleUtils.promptChoice(scanner, "Available roles:", roles);
            if (role == null) return;

            String type = ConsoleUtils.promptChoice(scanner, "Assignment type:", Arrays.asList("PERMANENT", "TEMPORARY"));
            if (type == null) return;

            String reason = ConsoleUtils.promptString(scanner, "Reason for assignment", false);
            if (reason == null) reason = "No reason provided";

            AssignmentMetadata meta = AssignmentMetadata.now(sys.getCurrentUser(), reason);
            RoleAssignment assignment;
            if (type.equals("PERMANENT")) {
                assignment = new PermanentAssignment(user, role, meta);
            } else {
                String expDate = ConsoleUtils.promptString(scanner, "Enter expiration date (yyyy-MM-dd HH:mm)", true);
                if (expDate == null) return;
                try {
                    LocalDateTime.parse(expDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                } catch (DateTimeParseException e) {
                    System.out.println("Invalid date format. Use yyyy-MM-dd HH:mm");
                    return;
                }
                assignment = new TemporaryAssignment(user, role, meta, expDate, false);
            }

            try {
                sys.getAssignmentManager().add(assignment);
                sys.getAuditLog().log("ASSIGN_ROLE", sys.getCurrentUser(), user.username() + ":" + role.getName(),
                        "Role assigned, type=" + type);
                System.out.println("Assignment created successfully. ID: " + assignment.assignmentId());
            } catch (DuplicateAssignmentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("revoke-role", "Revoke a role from a user", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username", true);
            if (username == null) return;
            Optional<User> userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }
            User user = userOpt.get();

            List<RoleAssignment> assignments = sys.getAssignmentManager().findByUser(user).stream()
                    .filter(RoleAssignment::isActive).collect(Collectors.toList());
            if (assignments.isEmpty()) {
                System.out.println("No active assignments for this user.");
                return;
            }

            RoleAssignment toRevoke = ConsoleUtils.promptChoice(scanner, "Active assignments:", assignments);
            if (toRevoke == null) return;

            sys.getAssignmentManager().revokeAssignment(toRevoke.assignmentId());
            sys.getAuditLog().log("REVOKE_ROLE", sys.getCurrentUser(),
                    user.username() + ":" + toRevoke.role().getName(),
                    "Role revoked");
            System.out.println("Assignment revoked.");
        });

        parser.registerCommand("assignment-list-user", "List assignments for a user", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username", true);
            if (username == null) return;
            Optional<User> userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }
            User user = userOpt.get();
            List<RoleAssignment> assignments = sys.getAssignmentManager().findByUser(user);
            printAssignmentTable(assignments);
        });

        parser.registerCommand("assignment-list-role", "List users with a specific role", (scanner, sys) -> {
            String roleName = ConsoleUtils.promptString(scanner, "Enter role name", true);
            if (roleName == null) return;
            Optional<Role> roleOpt = sys.getRoleManager().findByName(roleName);
            if (roleOpt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }
            Role role = roleOpt.get();
            List<RoleAssignment> assignments = sys.getAssignmentManager().findByRole(role);
            System.out.println("Users with role " + roleName + ":");
            assignments.stream().map(a -> a.user().username()).distinct().forEach(u -> System.out.println("  - " + u));
        });

        parser.registerCommand("assignment-active", "List all active assignments", (scanner, sys) -> {
            List<RoleAssignment> active = sys.getAssignmentManager().getActiveAssignments();
            printAssignmentTable(active);
        });

        parser.registerCommand("assignment-expired", "List all expired assignments", (scanner, sys) -> {
            List<RoleAssignment> expired = sys.getAssignmentManager().getExpiredAssignments();
            printAssignmentTable(expired);
        });

        parser.registerCommand("assignment-extend", "Extend a temporary assignment", (scanner, sys) -> {
            String id = ConsoleUtils.promptString(scanner, "Enter assignment ID", true);
            if (id == null) return;
            Optional<RoleAssignment> opt = sys.getAssignmentManager().findById(id);
            if (opt.isEmpty()) {
                System.out.println("Assignment not found.");
                return;
            }
            RoleAssignment assignment = opt.get();
            if (!(assignment instanceof TemporaryAssignment)) {
                System.out.println("Assignment is not temporary.");
                return;
            }
            String newDate = ConsoleUtils.promptString(scanner, "Enter new expiration date (yyyy-MM-dd HH:mm)", true);
            if (newDate == null) return;
            try {
                sys.getAssignmentManager().extendTemporaryAssignment(id, newDate);
                sys.getAuditLog().log("ASSIGNMENT_EXTEND", sys.getCurrentUser(), id, "Extended to " + newDate);
                System.out.println("Assignment extended.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("assignment-search", "Search assignments by filters", (scanner, sys) -> {
            List<String> options = Arrays.asList(
                    "By user",
                    "By role",
                    "By type (PERMANENT/TEMPORARY)",
                    "By status (active/inactive)",
                    "Assigned after date",
                    "Expiring before date"
            );
            String choice = ConsoleUtils.promptChoice(scanner, "Select filter:", options);
            if (choice == null) return;
            AssignmentFilter filter = null;
            switch (options.indexOf(choice)) {
                case 0:
                    String uname = ConsoleUtils.promptString(scanner, "Enter username", true);
                    if (uname != null) filter = AssignmentFilters.byUsername(uname);
                    break;
                case 1:
                    String rname = ConsoleUtils.promptString(scanner, "Enter role name", true);
                    if (rname != null) filter = AssignmentFilters.byRoleName(rname);
                    break;
                case 2:
                    String type = ConsoleUtils.promptChoice(scanner, "Type:", Arrays.asList("PERMANENT", "TEMPORARY"));
                    if (type != null) filter = AssignmentFilters.byType(type);
                    break;
                case 3:
                    String status = ConsoleUtils.promptChoice(scanner, "Status:", Arrays.asList("active", "inactive"));
                    if (status != null) {
                        if (status.equals("active")) filter = AssignmentFilters.activeOnly();
                        else filter = AssignmentFilters.inactiveOnly();
                    }
                    break;
                case 4:
                    String date = ConsoleUtils.promptString(scanner, "Enter date (yyyy-MM-dd HH:mm:ss)", true);
                    if (date != null) filter = AssignmentFilters.assignedAfter(date);
                    break;
                case 5:
                    String expDate = ConsoleUtils.promptString(scanner, "Enter date (yyyy-MM-dd HH:mm:ss)", true);
                    if (expDate != null) filter = AssignmentFilters.expiringBefore(expDate);
                    break;
            }
            if (filter == null) return;
            List<RoleAssignment> results = sys.getAssignmentManager().findByFilter(filter);
            printAssignmentTable(results);
        });

        // ---------- PERMISSION COMMANDS ----------
        parser.registerCommand("permissions-user", "List all permissions of a user", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username", true);
            if (username == null) return;
            Optional<User> userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }
            User user = userOpt.get();
            Set<Permission> perms = sys.getAssignmentManager().getUserPermissions(user);
            if (perms.isEmpty()) {
                System.out.println("User has no permissions.");
                return;
            }
            Map<String, List<Permission>> byResource = perms.stream()
                    .collect(Collectors.groupingBy(Permission::resource));
            System.out.println("Permissions for " + username + ":");
            byResource.forEach((res, list) -> {
                System.out.println("  Resource: " + res);
                list.forEach(p -> System.out.println("    - " + p.name() + ": " + p.description()));
            });
        });

        parser.registerCommand("permissions-check", "Check if user has a specific permission", (scanner, sys) -> {
            String username = ConsoleUtils.promptString(scanner, "Enter username", true);
            if (username == null) return;
            Optional<User> userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }
            User user = userOpt.get();
            String permName = ConsoleUtils.promptString(scanner, "Enter permission name", true);
            if (permName == null) return;
            String resource = ConsoleUtils.promptString(scanner, "Enter resource", true);
            if (resource == null) return;

            boolean has = sys.getAssignmentManager().userHasPermission(user, permName, resource);
            if (has) {
                System.out.println("User HAS this permission.");
                List<RoleAssignment> assignments = sys.getAssignmentManager().findByUser(user).stream()
                        .filter(RoleAssignment::isActive)
                        .filter(a -> a.role().hasPermission(permName, resource))
                        .collect(Collectors.toList());
                System.out.println("Granted by roles:");
                assignments.stream().map(a -> a.role().getName()).distinct().forEach(r -> System.out.println("  - " + r));
            } else {
                System.out.println("User does NOT have this permission.");
            }
        });

        // ---------- AUDIT AND REPORTS ----------
        parser.registerCommand("audit-log", "Show audit log", (scanner, sys) -> {
            sys.getAuditLog().printLog();
        });

        parser.registerCommand("report-users", "Generate user report", (scanner, sys) -> {
            String report = ReportGenerator.generateUserReport(sys.getUserManager(), sys.getAssignmentManager());
            System.out.println(report);
            if (ConsoleUtils.promptYesNo(scanner, "Save to file?")) {
                String filename = ConsoleUtils.promptString(scanner, "Enter filename", true);
                if (filename != null) {
                    try {
                        ReportGenerator.exportToFile(report, filename);
                        System.out.println("Report saved to " + filename);
                    } catch (Exception e) {
                        System.out.println("Error saving: " + e.getMessage());
                    }
                }
            }
        });

        parser.registerCommand("report-roles", "Generate role report", (scanner, sys) -> {
            String report = ReportGenerator.generateRoleReport(sys.getRoleManager(), sys.getAssignmentManager());
            System.out.println(report);
            if (ConsoleUtils.promptYesNo(scanner, "Save to file?")) {
                String filename = ConsoleUtils.promptString(scanner, "Enter filename", true);
                if (filename != null) {
                    try {
                        ReportGenerator.exportToFile(report, filename);
                        System.out.println("Report saved to " + filename);
                    } catch (Exception e) {
                        System.out.println("Error saving: " + e.getMessage());
                    }
                }
            }
        });

        parser.registerCommand("report-matrix", "Generate permission matrix", (scanner, sys) -> {
            String report = ReportGenerator.generatePermissionMatrix(sys.getUserManager(), sys.getAssignmentManager());
            System.out.println(report);
            if (ConsoleUtils.promptYesNo(scanner, "Save to file?")) {
                String filename = ConsoleUtils.promptString(scanner, "Enter filename", true);
                if (filename != null) {
                    try {
                        ReportGenerator.exportToFile(report, filename);
                        System.out.println("Report saved to " + filename);
                    } catch (Exception e) {
                        System.out.println("Error saving: " + e.getMessage());
                    }
                }
            }
        });

        // ---------- SERVICE COMMANDS ----------
        parser.registerCommand("help", "Show this help", (scanner, sys) -> parser.printHelp());

        parser.registerCommand("stats", "Show system statistics", (scanner, sys) -> {
            System.out.println(sys.generateStatistics());
        });

        parser.registerCommand("clear", "Clear the screen", (scanner, sys) -> {
            for (int i = 0; i < 50; i++) System.out.println();
        });

        parser.registerCommand("exit", "Exit the program", (scanner, sys) -> {
            if (ConsoleUtils.promptYesNo(scanner, "Are you sure you want to exit?")) {
                sys.getAuditLog().log("EXIT", sys.getCurrentUser(), "system", "Program exit");
                System.out.println("Goodbye!");
                System.exit(0);
            }
        });

        parser.registerCommand("save", "Save data to file (not implemented)", (scanner, sys) -> {
            System.out.println("Save functionality not implemented yet.");
        });

        parser.registerCommand("load", "Load data from file (not implemented)", (scanner, sys) -> {
            System.out.println("Load functionality not implemented yet.");
        });
    }

    private static void printUserTable(List<User> users) {
        if (users.isEmpty()) {
            System.out.println("No users.");
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (User u : users) {
            rows.add(new String[]{u.username(), u.fullName(), u.email()});
        }
        System.out.println(FormatUtils.formatTable(new String[]{"Username", "Full Name", "Email"}, rows));
    }

    private static void printAssignmentTable(List<RoleAssignment> assignments) {
        if (assignments.isEmpty()) {
            System.out.println("No assignments.");
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (RoleAssignment a : assignments) {
            rows.add(new String[]{
                    a.assignmentId(),
                    a.user().username(),
                    a.role().getName(),
                    a.assignmentType(),
                    a.isActive() ? "ACTIVE" : "INACTIVE",
                    a.metadata().assignedAt()
            });
        }
        System.out.println(FormatUtils.formatTable(
                new String[]{"Assignment ID", "Username", "Role", "Type", "Status", "Assigned At"},
                rows
        ));
    }

    private static void addPermissionsToRoleInteractive(Scanner scanner, RBACSystem sys, Role role) {
        while (ConsoleUtils.promptYesNo(scanner, "Add permission?")) {
            String pName = ConsoleUtils.promptString(scanner, "Enter permission name", true);
            if (pName == null) continue;
            String resource = ConsoleUtils.promptString(scanner, "Enter resource", true);
            if (resource == null) continue;
            String desc = ConsoleUtils.promptString(scanner, "Enter description", false);
            try {
                Permission perm = new Permission(pName, resource, desc == null ? "" : desc);
                sys.getRoleManager().addPermissionToRole(role.getName(), perm);
                System.out.println("Permission added.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}