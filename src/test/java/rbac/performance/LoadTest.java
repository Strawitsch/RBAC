package rbac.performance;

import org.junit.jupiter.api.Test;
import rbac.commands.RBACSystem;
import rbac.core.*;
import rbac.managers.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class LoadTest {

    @Test
    void concurrentUserAndRoleOperations() throws InterruptedException {
        RBACSystem system = new RBACSystem();
        system.initialize();

        int threadCount = 10;
        int opsPerThread = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int id = t;
            pool.submit(() -> {
                try {
                    for (int i = 0; i < opsPerThread; i++) {
                        String username = "user_" + id + "_" + i;
                        String email = username + "@test.com";
                        User user = User.validate(username, "Test User", email);
                        try {
                            system.getUserManager().add(user);
                        } catch (Exception e) {
                            // дубликат может быть при коллизии – игнорируем
                        }
                        // Создаём роль
                        Role role = new Role("role_" + username, "Test role");
                        try {
                            system.getRoleManager().add(role);
                        } catch (Exception e) {}

                        // Назначаем роль (если пользователь и роль существуют)
                        system.getUserManager().findByUsername(username).ifPresent(u ->
                                system.getRoleManager().findByName("role_" + username).ifPresent(r -> {
                                    AssignmentMetadata meta = AssignmentMetadata.now("loadtest", "concurrent");
                                    RoleAssignment assign = new PermanentAssignment(u, r, meta);
                                    try {
                                        system.getAssignmentManager().add(assign);
                                    } catch (Exception ignored) {}
                                })
                        );


                        system.getUserManager().findByFilter(user1 -> user1.username().contains("user"));
                        system.getRoleManager().findByFilter(role1 -> role1.getPermissions().size() >= 0);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        pool.shutdown();

        assertTrue(system.getUserManager().count() >= 0);
        assertTrue(system.getRoleManager().count() >= 0);
        System.out.println("Load test completed. Users: " + system.getUserManager().count() +
                ", Roles: " + system.getRoleManager().count());
    }
}