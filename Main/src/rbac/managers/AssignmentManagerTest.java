package rbac.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rbac.core.*;
import rbac.exceptions.*;
import rbac.filters.AssignmentFilters;
import rbac.sorters.AssignmentSorters;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssignmentManagerTest {
    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;
    private User user;
    private Role role;
    private Permission permission;
    private AssignmentMetadata metadata;
    private PermanentAssignment permAssignment;
    private TemporaryAssignment tempAssignment;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager(userManager, roleManager);
        roleManager.setAssignmentManager(assignmentManager);

        user = User.validate("testuser", "Test User", "test@example.com");
        permission = new Permission("READ", "files", "Read files");
        role = new Role("TestRole", "Test role");
        role.addPermission(permission);

        userManager.add(user);
        roleManager.add(role);

        metadata = AssignmentMetadata.now("admin", "test assignment");
        permAssignment = new PermanentAssignment(user, role, metadata);
        tempAssignment = new TemporaryAssignment(user, role, metadata,
                LocalDateTime.now().plusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), false);
    }

    @Test
    void addPermanentAssignment_success() {
        assignmentManager.add(permAssignment);
        assertEquals(1, assignmentManager.count());
        assertTrue(assignmentManager.findById(permAssignment.assignmentId()).isPresent());
    }

    @Test
    void addTemporaryAssignment_success() {
        assignmentManager.add(tempAssignment);
        assertEquals(1, assignmentManager.count());
    }

    @Test
    void addAssignment_userNotFound_throwsException() {
        User ghost = User.validate("ghost", "Ghost", "ghost@ex.com");
        PermanentAssignment bad = new PermanentAssignment(ghost, role, metadata);
        assertThrows(UserNotFoundException.class, () -> assignmentManager.add(bad));
    }

    @Test
    void addAssignment_roleNotFound_throwsException() {
        Role ghostRole = new Role("GhostRole", "ghost");
        PermanentAssignment bad = new PermanentAssignment(user, ghostRole, metadata);
        assertThrows(RoleNotFoundException.class, () -> assignmentManager.add(bad));
    }

    @Test
    void addAssignment_duplicateActive_throwsException() {
        assignmentManager.add(permAssignment);
        PermanentAssignment duplicate = new PermanentAssignment(user, role, metadata);
        assertThrows(DuplicateAssignmentException.class, () -> assignmentManager.add(duplicate));
    }

    @Test
    void removeAssignment_success() {
        assignmentManager.add(permAssignment);
        boolean removed = assignmentManager.remove(permAssignment);
        assertTrue(removed);
        assertEquals(0, assignmentManager.count());
    }

    @Test
    void findByUser_returnsAssignments() {
        assignmentManager.add(permAssignment);
        assignmentManager.add(tempAssignment);
        List<RoleAssignment> found = assignmentManager.findByUser(user);
        assertEquals(2, found.size());
    }

    @Test
    void findByRole_returnsAssignments() {
        assignmentManager.add(permAssignment);
        List<RoleAssignment> found = assignmentManager.findByRole(role);
        assertEquals(1, found.size());
    }

    @Test
    void findByFilter_returnsMatching() {
        assignmentManager.add(permAssignment);
        assignmentManager.add(tempAssignment);
        List<RoleAssignment> active = assignmentManager.findByFilter(AssignmentFilters.activeOnly());
        assertEquals(2, active.size());
    }

    @Test
    void findAll_withFilterAndSorter() {
        assignmentManager.add(permAssignment);
        assignmentManager.add(tempAssignment);
        List<RoleAssignment> sorted = assignmentManager.findAll(
                AssignmentFilters.byUser(user),
                AssignmentSorters.byAssignmentDate()
        );
        assertEquals(2, sorted.size());
    }

    @Test
    void getActiveAssignments_returnsActive() {
        assignmentManager.add(permAssignment);
        assignmentManager.add(tempAssignment);
        assertEquals(2, assignmentManager.getActiveAssignments().size());
        permAssignment.revoke();
        assertEquals(1, assignmentManager.getActiveAssignments().size());
    }

    @Test
    void getExpiredAssignments_returnsExpired() {
        assignmentManager.add(permAssignment);
        assignmentManager.add(tempAssignment);
        assertEquals(0, assignmentManager.getExpiredAssignments().size());
        String past = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        TemporaryAssignment expired = new TemporaryAssignment(user, role, metadata, past, false);
        assignmentManager.add(expired);
        assertEquals(1, assignmentManager.getExpiredAssignments().size());
    }

    @Test
    void userHasRole_returnsTrue() {
        assignmentManager.add(permAssignment);
        assertTrue(assignmentManager.userHasRole(user, role));
        assertFalse(assignmentManager.userHasRole(user, mock(Role.class)));
    }

    @Test
    void userHasPermission_returnsTrue() {
        assignmentManager.add(permAssignment);
        assertTrue(assignmentManager.userHasPermission(user, "READ", "files"));
        assertFalse(assignmentManager.userHasPermission(user, "WRITE", "files"));
    }

    @Test
    void getUserPermissions_returnsSet() {
        assignmentManager.add(permAssignment);
        Set<Permission> perms = assignmentManager.getUserPermissions(user);
        assertEquals(1, perms.size());
        assertTrue(perms.contains(permission));
    }

    @Test
    void revokeAssignment_permanent_setsInactive() {
        assignmentManager.add(permAssignment);
        assignmentManager.revokeAssignment(permAssignment.assignmentId());
        assertFalse(permAssignment.isActive());
    }

    @Test
    void revokeAssignment_temporary_removes() {
        assignmentManager.add(tempAssignment);
        String id = tempAssignment.assignmentId();
        assignmentManager.revokeAssignment(id);
        assertFalse(assignmentManager.findById(id).isPresent());
    }

    @Test
    void revokeAssignment_notFound_throwsException() {
        assertThrows(AssignmentNotFoundException.class, () -> assignmentManager.revokeAssignment("unknown"));
    }

    @Test
    void extendTemporaryAssignment_success() {
        assignmentManager.add(tempAssignment);
        String newDate = LocalDateTime.now().plusDays(10).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        assignmentManager.extendTemporaryAssignment(tempAssignment.assignmentId(), newDate);
        assertFalse(tempAssignment.isExpired());
    }

    @Test
    void extendTemporaryAssignment_notTemporary_throwsException() {
        assignmentManager.add(permAssignment);
        assertThrows(IllegalArgumentException.class,
                () -> assignmentManager.extendTemporaryAssignment(permAssignment.assignmentId(), "2025-01-01 00:00"));
    }

    @Test
    void clear_removesAll() {
        assignmentManager.add(permAssignment);
        assignmentManager.add(tempAssignment);
        assignmentManager.clear();
        assertEquals(0, assignmentManager.count());
    }
}