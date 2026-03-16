package rbac.commands;

import rbac.core.*;
import rbac.exceptions.*;
import rbac.filters.*;
import rbac.sorters.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class CommandRegistry {

    public static void registerAll(CommandParser parser, RBACSystem system) {
        parser.registerCommand("user-list", "List all users (with optional filters)", (scanner, sys) -> {
            List<User> users = sys.getUserManager().findAll();
            printUserTable(users);
        });

        parser.registerCommand("user-create", "Create a new user", (scanner, sys) -> {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Enter full name: ");
            String fullName = scanner.nextLine().trim();
            System.out.print("Enter email: ");
            String email = scanner.nextLine().trim();
            try {
                User user = User.validate(username, fullName, email);
                sys.getUserManager().add(user);
                System.out.println("User created successfully.");
            } catch (IllegalArgumentException | DuplicateUserException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("user-view", "View user details", (scanner, sys) -> {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();
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
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();
            Optional<User> opt = sys.getUserManager().findByUsername(username);
            if (opt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }
            System.out.print("Enter new full name: ");
            String fullName = scanner.nextLine().trim();
            System.out.print("Enter new email: ");
            String email = scanner.nextLine().trim();
            try {
                sys.getUserManager().update(username, fullName, email);
                System.out.println("User updated successfully.");
            } catch (IllegalArgumentException | UserNotFoundException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("user-delete", "Delete a user", (scanner, sys) -> {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();
            Optional<User> opt = sys.getUserManager().findByUsername(username);
            if (opt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }
            User user = opt.get();
            System.out.print("Are you sure you want to delete user " + username + "? (yes/no): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            if (!confirm.equals("yes")) {
                System.out.println("Deletion cancelled.");
                return;
            }

            List<RoleAssignment> assignments = sys.getAssignmentManager().findByUser(user);
            assignments.forEach(a -> sys.getAssignmentManager().remove(a));
            if (sys.getUserManager().remove(user)) {
                System.out.println("User deleted successfully.");
            } else {
                System.out.println("Failed to delete user.");
            }
        });

        parser.registerCommand("user-search", "Search users by filters", (scanner, sys) -> {
            System.out.println("Select filter:");
            System.out.println("1. Username contains");
            System.out.println("2. Email contains");
            System.out.println("3. Email domain");
            System.out.println("4. Full name contains");
            System.out.print("Choice (1-4): ");
            String choice = scanner.nextLine().trim();
            UserFilter filter = null;
            switch (choice) {
                case "1":
                    System.out.print("Enter substring: ");
                    String sub = scanner.nextLine().trim();
                    filter = UserFilters.byUsernameContains(sub);
                    break;
                case "2":
                    System.out.print("Enter email substring: ");
                    sub = scanner.nextLine().trim();
                    filter = user -> user.email().toLowerCase().contains(sub.toLowerCase());
                    break;
                case "3":
                    System.out.print("Enter domain (e.g. @company.com): ");
                    String domain = scanner.nextLine().trim();
                    filter = UserFilters.byEmailDomain(domain);
                    break;
                case "4":
                    System.out.print("Enter name substring: ");
                    sub = scanner.nextLine().trim();
                    filter = UserFilters.byFullNameContains(sub);
                    break;
                default:
                    System.out.println("Invalid choice.");
                    return;
            }
            List<User> results = sys.getUserManager().findByFilter(filter);
            if (results.isEmpty()) {
                System.out.println("No users found.");
            } else {
                printUserTable(results);
            }
        });

        // Role commands
        parser.registerCommand("role-list", "List all roles", (scanner, sys) -> {
            List<Role> roles = sys.getRoleManager().findAll();
            System.out.println("Roles:");
            roles.forEach(r -> System.out.printf("  %s [%s] (%d permissions)%n",
                    r.getName(), r.getId(), r.getPermissions().size()));
        });

        parser.registerCommand("role-create", "Create a new role", (scanner, sys) -> {
            System.out.print("Enter role name: ");
            String name = scanner.nextLine().trim();
            System.out.print("Enter description: ");
            String desc = scanner.nextLine().trim();
            Role role = new Role(name, desc);
            try {
                sys.getRoleManager().add(role);
                System.out.println("Role created successfully.");
                // Предложить добавить права
                addPermissionsToRoleInteractive(scanner, sys, role);
            } catch (DuplicateRoleException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("role-view", "View role details", (scanner, sys) -> {
            System.out.print("Enter role name: ");
            String name = scanner.nextLine().trim();
            Optional<Role> opt = sys.getRoleManager().findByName(name);
            if (opt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }
            System.out.println(opt.get().format());
        });

        parser.registerCommand("role-update", "Обновить название и описание роли",
                (scanner, sys) -> {
                    System.out.print("Введите имя роли для обновления: ");
                    String oldName = scanner.nextLine().trim();

                    Role role = sys.getRoleManager().findByName(oldName).orElse(null);
                    if (role == null) {
                        System.out.println("Роль с именем '" + oldName + "' не найдена.");
                        return;
                    }

                    System.out.print("Введите новое название роли (Enter - оставить без изменений): ");
                    String newName = scanner.nextLine().trim();
                    if (newName.isEmpty()) {
                        newName = oldName;
                    }

                    System.out.print("Введите новое описание роли (Enter - оставить без изменений): ");
                    String newDesc = scanner.nextLine().trim();
                    if (newDesc.isEmpty()) {
                        newDesc = role.getDescription();
                    }

                    try {
                        system.getRoleManager().updateRole(oldName, newName, newDesc);
                        System.out.println("Роль успешно обновлена.");
                    } catch (Exception e) {
                        System.out.println("Ошибка при обновлении роли: " + e.getMessage());
                    }
                });

        parser.registerCommand("role-delete", "Delete a role", (scanner, sys) -> {
            System.out.print("Enter role name: ");
            String name = scanner.nextLine().trim();
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
                System.out.print("Are you sure you want to delete it anyway? (yes/no): ");
                String confirm = scanner.nextLine().trim().toLowerCase();
                if (!confirm.equals("yes")) {
                    System.out.println("Deletion cancelled.");
                    return;
                }
                assignments.forEach(a -> sys.getAssignmentManager().remove(a));
            }
            if (sys.getRoleManager().remove(role)) {
                System.out.println("Role deleted successfully.");
            } else {
                System.out.println("Failed to delete role.");
            }
        });

        parser.registerCommand("role-add-permission", "Add permission to role", (scanner, sys) -> {
            System.out.print("Enter role name: ");
            String name = scanner.nextLine().trim();
            Optional<Role> opt = sys.getRoleManager().findByName(name);
            if (opt.isEmpty()) {
                System.out.println("Role not found.");
                return;
            }
            Role role = opt.get();
            System.out.print("Enter permission name: ");
            String permName = scanner.nextLine().trim().toUpperCase();
            System.out.print("Enter resource: ");
            String resource = scanner.nextLine().trim().toLowerCase();
            System.out.print("Enter description: ");
            String desc = scanner.nextLine().trim();
            try {
                Permission perm = new Permission(permName, resource, desc);
                sys.getRoleManager().addPermissionToRole(name, perm);
                System.out.println("Permission added.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("role-remove-permission", "Remove permission from role", (scanner, sys) -> {
            System.out.print("Enter role name: ");
            String name = scanner.nextLine().trim();
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
            for (int i = 0; i < permList.size(); i++) {
                System.out.printf("%d. %s%n", i+1, permList.get(i).format());
            }
            System.out.print("Enter number to remove: ");
            String input = scanner.nextLine().trim();
            try {
                int idx = Integer.parseInt(input) - 1;
                if (idx < 0 || idx >= permList.size()) {
                    System.out.println("Invalid number.");
                    return;
                }
                Permission toRemove = permList.get(idx);
                sys.getRoleManager().removePermissionFromRole(name, toRemove);
                System.out.println("Permission removed.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
            }
        });

        parser.registerCommand("role-search", "Search roles", (scanner, sys) -> {
            System.out.println("Select filter:");
            System.out.println("1. Name contains");
            System.out.println("2. Has specific permission");
            System.out.println("3. At least N permissions");
            System.out.print("Choice (1-3): ");
            String choice = scanner.nextLine().trim();
            RoleFilter filter = null;
            switch (choice) {
                case "1":
                    System.out.print("Enter substring: ");
                    String sub = scanner.nextLine().trim();
                    filter = RoleFilters.byNameContains(sub);
                    break;
                case "2":
                    System.out.print("Enter permission name: ");
                    String pName = scanner.nextLine().trim();
                    System.out.print("Enter resource: ");
                    String res = scanner.nextLine().trim();
                    filter = RoleFilters.hasPermission(pName, res);
                    break;
                case "3":
                    System.out.print("Enter minimum number of permissions: ");
                    try {
                        int n = Integer.parseInt(scanner.nextLine().trim());
                        filter = RoleFilters.hasAtLeastNPermissions(n);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number.");
                        return;
                    }
                    break;
                default:
                    System.out.println("Invalid choice.");
                    return;
            }
            List<Role> results = sys.getRoleManager().findByFilter(filter);
            if (results.isEmpty()) {
                System.out.println("No roles found.");
            } else {
                results.forEach(r -> System.out.printf("  %s (%d permissions)%n", r.getName(), r.getPermissions().size()));
            }
        });

        parser.registerCommand("assign-role", "Assign a role to a user", (scanner, sys) -> {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();
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
            System.out.println("Available roles:");
            for (int i = 0; i < roles.size(); i++) {
                System.out.printf("%d. %s%n", i+1, roles.get(i).getName());
            }
            System.out.print("Select role number: ");
            String roleInput = scanner.nextLine().trim();
            int roleIdx;
            try {
                roleIdx = Integer.parseInt(roleInput) - 1;
                if (roleIdx < 0 || roleIdx >= roles.size()) {
                    System.out.println("Invalid number.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
                return;
            }
            Role role = roles.get(roleIdx);

            System.out.print("Assignment type (PERMANENT/TEMPORARY): ");
            String type = scanner.nextLine().trim().toUpperCase();
            if (!type.equals("PERMANENT") && !type.equals("TEMPORARY")) {
                System.out.println("Invalid type. Use PERMANENT or TEMPORARY.");
                return;
            }

            System.out.print("Reason for assignment: ");
            String reason = scanner.nextLine().trim();

            AssignmentMetadata meta = AssignmentMetadata.now(sys.getCurrentUser(), reason);
            RoleAssignment assignment;
            if (type.equals("PERMANENT")) {
                assignment = new PermanentAssignment(user, role, meta);
            } else {
                System.out.print("Enter expiration date (yyyy-MM-dd HH:mm): ");
                String expDate = scanner.nextLine().trim();
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
                System.out.println("Assignment created successfully. ID: " + assignment.assignmentId());
            } catch (DuplicateAssignmentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("revoke-role", "Revoke a role from a user", (scanner, sys) -> {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();
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

            System.out.println("Active assignments:");
            for (int i = 0; i < assignments.size(); i++) {
                RoleAssignment a = assignments.get(i);
                System.out.printf("%d. %s [%s] assigned at %s%n",
                        i+1, a.role().getName(), a.assignmentType(), a.metadata().assignedAt());
            }
            System.out.print("Select assignment number to revoke: ");
            String idxInput = scanner.nextLine().trim();
            int idx;
            try {
                idx = Integer.parseInt(idxInput) - 1;
                if (idx < 0 || idx >= assignments.size()) {
                    System.out.println("Invalid number.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
                return;
            }
            RoleAssignment toRevoke = assignments.get(idx);
            sys.getAssignmentManager().revokeAssignment(toRevoke.assignmentId());
            System.out.println("Assignment revoked.");
        });

        parser.registerCommand("assignment-list-user", "List assignments for a user", (scanner, sys) -> {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();
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
            System.out.print("Enter role name: ");
            String roleName = scanner.nextLine().trim();
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
            System.out.print("Enter assignment ID: ");
            String id = scanner.nextLine().trim();
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
            System.out.print("Enter new expiration date (yyyy-MM-dd HH:mm): ");
            String newDate = scanner.nextLine().trim();
            try {
                sys.getAssignmentManager().extendTemporaryAssignment(id, newDate);
                System.out.println("Assignment extended.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });

        parser.registerCommand("assignment-search", "Search assignments by filters", (scanner, sys) -> {
            System.out.println("Select filter:");
            System.out.println("1. By user");
            System.out.println("2. By role");
            System.out.println("3. By type (PERMANENT/TEMPORARY)");
            System.out.println("4. By status (active/inactive)");
            System.out.println("5. Assigned after date");
            System.out.println("6. Expiring before date");
            System.out.print("Choice (1-6): ");
            String choice = scanner.nextLine().trim();
            AssignmentFilter filter = null;
            switch (choice) {
                case "1":
                    System.out.print("Enter username: ");
                    String uname = scanner.nextLine().trim();
                    filter = AssignmentFilters.byUsername(uname);
                    break;
                case "2":
                    System.out.print("Enter role name: ");
                    String rname = scanner.nextLine().trim();
                    filter = AssignmentFilters.byRoleName(rname);
                    break;
                case "3":
                    System.out.print("Enter type (PERMANENT/TEMPORARY): ");
                    String type = scanner.nextLine().trim().toUpperCase();
                    filter = AssignmentFilters.byType(type);
                    break;
                case "4":
                    System.out.print("Status (active/inactive): ");
                    String status = scanner.nextLine().trim().toLowerCase();
                    if (status.equals("active")) {
                        filter = AssignmentFilters.activeOnly();
                    } else if (status.equals("inactive")) {
                        filter = AssignmentFilters.inactiveOnly();
                    } else {
                        System.out.println("Invalid status.");
                        return;
                    }
                    break;
                case "5":
                    System.out.print("Enter date (yyyy-MM-dd HH:mm:ss): ");
                    String date = scanner.nextLine().trim();
                    filter = AssignmentFilters.assignedAfter(date);
                    break;
                case "6":
                    System.out.print("Enter date (yyyy-MM-dd HH:mm:ss): ");
                    String expDate = scanner.nextLine().trim();
                    filter = AssignmentFilters.expiringBefore(expDate);
                    break;
                default:
                    System.out.println("Invalid choice.");
                    return;
            }
            List<RoleAssignment> results = sys.getAssignmentManager().findByFilter(filter);
            printAssignmentTable(results);
        });

        // Permission commands
        parser.registerCommand("permissions-user", "List all permissions of a user", (scanner, sys) -> {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();
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
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();
            Optional<User> userOpt = sys.getUserManager().findByUsername(username);
            if (userOpt.isEmpty()) {
                System.out.println("User not found.");
                return;
            }
            User user = userOpt.get();
            System.out.print("Enter permission name: ");
            String permName = scanner.nextLine().trim().toUpperCase();
            System.out.print("Enter resource: ");
            String resource = scanner.nextLine().trim().toLowerCase();

            boolean has = sys.getAssignmentManager().userHasPermission(user, permName, resource);
            if (has) {
                System.out.println("User HAS this permission.");
                // Найти из какой роли
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

        // Service commands
        parser.registerCommand("help", "Show this help", (scanner, sys) -> parser.printHelp());

        parser.registerCommand("stats", "Show system statistics", (scanner, sys) -> {
            System.out.println(sys.generateStatistics());
        });

        parser.registerCommand("clear", "Clear the screen", (scanner, sys) -> {
            for (int i = 0; i < 50; i++) System.out.println();
        });

        parser.registerCommand("exit", "Exit the program", (scanner, sys) -> {
            System.out.print("Are you sure you want to exit? (yes/no): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            if (confirm.equals("yes")) {
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
        System.out.printf("%-20s %-30s %-30s%n", "Username", "Full Name", "Email");
        System.out.println("-------------------------------------------------------------------");
        users.forEach(u -> System.out.printf("%-20s %-30s %-30s%n",
                u.username(), u.fullName(), u.email()));
    }

    private static void printAssignmentTable(List<RoleAssignment> assignments) {
        if (assignments.isEmpty()) {
            System.out.println("No assignments.");
            return;
        }
        System.out.printf("%-20s %-20s %-20s %-10s %-10s %-20s%n",
                "Assignment ID", "Username", "Role", "Type", "Status", "Assigned At");
        System.out.println("----------------------------------------------------------------------------------------------------");
        assignments.forEach(a -> System.out.printf("%-20s %-20s %-20s %-10s %-10s %-20s%n",
                a.assignmentId(), a.user().username(), a.role().getName(),
                a.assignmentType(), a.isActive() ? "ACTIVE" : "INACTIVE",
                a.metadata().assignedAt()));
    }

    private static void addPermissionsToRoleInteractive(Scanner scanner, RBACSystem sys, Role role) {
        while (true) {
            System.out.print("Add permission? (yes/no): ");
            String answer = scanner.nextLine().trim().toLowerCase();
            if (!answer.equals("yes")) break;
            System.out.print("Enter permission name: ");
            String pName = scanner.nextLine().trim().toUpperCase();
            System.out.print("Enter resource: ");
            String res = scanner.nextLine().trim().toLowerCase();
            System.out.print("Enter description: ");
            String desc = scanner.nextLine().trim();
            try {
                Permission perm = new Permission(pName, res, desc);
                sys.getRoleManager().addPermissionToRole(role.getName(), perm);
                System.out.println("Permission added.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}