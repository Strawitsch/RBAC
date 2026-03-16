package rbac.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rbac.core.User;
import rbac.managers.AssignmentManager;
import rbac.managers.RoleManager;
import rbac.managers.UserManager;

import static org.junit.jupiter.api.Assertions.*;

public class RBACSystemTest {
    private RBACSystem system;

    @BeforeEach
    void setUp() {
        system = new RBACSystem();
        system.initialize();
    }

    @Test
    void testInitialization() {
        UserManager um = system.getUserManager();
        RoleManager rm = system.getRoleManager();
        AssignmentManager am = system.getAssignmentManager();

        assertEquals(1, um.count()); // admin
        assertEquals(3, rm.count()); // Admin, Manager, Viewer
        assertEquals(1, am.count()); // admin назначение

        assertTrue(um.exists("admin"));
        assertTrue(rm.exists("Admin"));
        assertTrue(rm.exists("Manager"));
        assertTrue(rm.exists("Viewer"));
    }

    @Test
    void testGenerateStatistics() {
        String stats = system.generateStatistics();
        assertNotNull(stats);
        assertTrue(stats.contains("Users: 1"));
        assertTrue(stats.contains("Roles: 3"));
        assertTrue(stats.contains("Assignments: total 1"));
    }
}