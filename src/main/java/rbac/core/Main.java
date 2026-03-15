package rbac.core;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== RBAC SYSTEM INTEGRATION TEST ===\n");

        try {
            // 1. Создание пользователя
            User admin = User.validate("admin_root", "System Administrator", "admin@corp.com");
            User developer = User.validate("dev_ivan", "Ivan Ivanov", "ivan@dev.ru");

            // 2. Создание прав (Permissions)
            Permission readUsers = new Permission("READ", "users", "Can view user list");
            Permission writeUsers = new Permission("WRITE", "users", "Can edit users");
            Permission deleteUsers = new Permission("DELETE", "users", "Can remove users");

            // 3. Создание роли и добавление прав
            Role adminRole = new Role("Administrator", "Full system access");
            adminRole.addPermission(readUsers);
            adminRole.addPermission(writeUsers);
            adminRole.addPermission(deleteUsers);

            Role viewerRole = new Role("Viewer", "Read-only access");
            viewerRole.addPermission(readUsers);

            // 4. Демонстрация rbac.core.Role.format()
            System.out.println(adminRole.format());

            // 5. Создание метаданных назначения
            AssignmentMetadata meta = AssignmentMetadata.now(admin.username(), "Standard onboarding");

            // 6. Постоянное назначение (Permanent Assignment)
            PermanentAssignment permAsgn = new PermanentAssignment(developer, viewerRole, meta);
            System.out.println("--- Permanent Assignment ---");
            System.out.println(permAsgn.summary());

            permAsgn.revoke();
            System.out.println("\nAfter Revoke:");
            System.out.println("Is Active: " + permAsgn.isActive());

            System.out.println("\n----------------------------\n");

            // 7. Временное назначение (Temporary Assignment)
            // Установим дату: сегодня + 1 час (формат yyyy-MM-dd HH:mm)
            String expiryDate = "2026-12-31 23:59";
            // Примечание: В реальном тесте используйте актуальную дату

            TemporaryAssignment tempAsgn = new TemporaryAssignment(
                    developer,
                    adminRole,
                    AssignmentMetadata.now(admin.username(), "Urgent bugfix task"),
                    expiryDate,
                    false
            );

            System.out.println("--- Temporary Assignment ---");
            System.out.println(tempAsgn.summary());

            // 8. Проверка наличия конкретного права через роль в назначении
            boolean canDelete = tempAsgn.role().hasPermission("DELETE", "users");
            System.out.println("\nDoes developer have DELETE permission via Temp rbac.core.Role? " + canDelete);

        } catch (Exception e) {
            System.err.println("Test failed with error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}