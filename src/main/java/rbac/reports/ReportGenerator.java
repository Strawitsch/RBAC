package rbac.reports;

import rbac.core.*;
import rbac.managers.*;
import rbac.utils.FormatUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class ReportGenerator {
    public static String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append(FormatUtils.formatHeader("User Report"));

        List<String[]> rows = new ArrayList<>();
        for (User user : userManager.findAll()) {
            List<RoleAssignment> assignments = assignmentManager.findByUser(user);
            String roles = assignments.stream()
                    .map(a -> a.role().getName() + (a.isActive() ? "" : " (inactive)"))
                    .collect(Collectors.joining(", "));
            rows.add(new String[]{user.username(), user.fullName(), user.email(), roles});
        }
        sb.append(FormatUtils.formatTable(
                new String[]{"Username", "Full Name", "Email", "Roles"},
                rows
        ));
        return sb.toString();
    }

    public static String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append(FormatUtils.formatHeader("Role Report"));

        List<String[]> rows = new ArrayList<>();
        for (Role role : roleManager.findAll()) {
            long userCount = assignmentManager.findByRole(role).stream()
                    .map(a -> a.user().username())
                    .distinct()
                    .count();
            rows.add(new String[]{role.getName(), String.valueOf(role.getPermissions().size()), String.valueOf(userCount)});
        }
        sb.append(FormatUtils.formatTable(
                new String[]{"Role", "Permissions", "Users"},
                rows
        ));
        return sb.toString();
    }

    public static String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder sb = new StringBuilder();
        sb.append(FormatUtils.formatHeader("Permission Matrix"));

        Set<Permission> allPermissions = assignmentManager.findAll().stream()
                .flatMap(a -> a.role().getPermissions().stream())
                .collect(Collectors.toSet());
        Map<String, Set<String>> userPermissions = new HashMap<>();
        for (User user : userManager.findAll()) {
            Set<String> permNames = assignmentManager.getUserPermissions(user).stream()
                    .map(p -> p.name() + ":" + p.resource())
                    .collect(Collectors.toSet());
            userPermissions.put(user.username(), permNames);
        }

        List<String[]> rows = new ArrayList<>();
        for (User user : userManager.findAll()) {
            rows.add(new String[]{user.username(), String.join(", ", userPermissions.get(user.username()))});
        }
        sb.append(FormatUtils.formatTable(
                new String[]{"User", "Permissions"},
                rows
        ));
        return sb.toString();
    }

    public static void exportToFile(String report, String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println(report);
        }
    }
}