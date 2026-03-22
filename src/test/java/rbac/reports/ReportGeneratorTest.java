package rbac.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rbac.commands.RBACSystem;

import static org.junit.jupiter.api.Assertions.*;

class ReportGeneratorTest {
    private RBACSystem system;

    @BeforeEach
    void setUp() {
        system = new RBACSystem();
        system.initialize();
    }

    @Test void testGenerateUserReport() {
        String report = ReportGenerator.generateUserReport(system.getUserManager(), system.getAssignmentManager());
        assertTrue(report.contains("admin"));
        assertTrue(report.contains("System Administrator"));
    }

    @Test void testGenerateRoleReport() {
        String report = ReportGenerator.generateRoleReport(system.getRoleManager(), system.getAssignmentManager());
        assertTrue(report.contains("Admin"));
        assertTrue(report.contains("Viewer"));
    }

    @Test void testGeneratePermissionMatrix() {
        String matrix = ReportGenerator.generatePermissionMatrix(system.getUserManager(), system.getAssignmentManager());
        assertTrue(matrix.contains("admin"));
        assertTrue(matrix.contains("READ"));
    }
}