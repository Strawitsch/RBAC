package rbac.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rbac.core.Permission;
import rbac.core.Role;
import rbac.exceptions.DuplicateRoleException;
import rbac.exceptions.RoleInUseException;
import rbac.exceptions.RoleNotFoundException;
import rbac.filters.RoleFilters;
import rbac.sorters.RoleSorters;
import rbac.core.RoleAssignment;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleManagerTest {
    private RoleManager roleManager;
    @Mock private AssignmentManager assignmentManager;
    private Role roleAdmin;
    private Role roleViewer;
    private Permission permRead;
    private Permission permWrite;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        roleManager = new RoleManager();
        roleManager.setAssignmentManager(assignmentManager);
        permRead = new Permission("READ", "docs", "Read documents");
        permWrite = new Permission("WRITE", "docs", "Write documents");
        roleAdmin = new Role("Admin", "Administrator role");
        roleViewer = new Role("Viewer", "Viewer role");
    }

    @Test
    void addRole_success() {
        roleManager.add(roleAdmin);
        assertEquals(1, roleManager.count());
        assertTrue(roleManager.exists("Admin"));
    }

    @Test
    void addRole_duplicateName_throwsException() {
        roleManager.add(roleAdmin);
        Role anotherAdmin = new Role("Admin", "Another admin");
        assertThrows(DuplicateRoleException.class, () -> roleManager.add(anotherAdmin));
    }

    @Test
    void removeRole_success_whenNotAssigned() {
        roleManager.add(roleAdmin);
        when(assignmentManager.findByRole(roleAdmin)).thenReturn(List.of());
        boolean removed = roleManager.remove(roleAdmin);
        assertTrue(removed);
        assertEquals(0, roleManager.count());
    }

    @Test
    void removeRole_whenAssigned_throwsException() {
        roleManager.add(roleAdmin);
        when(assignmentManager.findByRole(roleAdmin)).thenReturn(List.of(mock(RoleAssignment.class)));
        assertThrows(RoleInUseException.class, () -> roleManager.remove(roleAdmin));
    }

    @Test
    void findById_returnsRole() {
        roleManager.add(roleAdmin);
        Optional<Role> found = roleManager.findById(roleAdmin.getId());
        assertTrue(found.isPresent());
        assertEquals(roleAdmin, found.get());
    }

    @Test
    void findByName_returnsRole() {
        roleManager.add(roleAdmin);
        Optional<Role> found = roleManager.findByName("Admin");
        assertTrue(found.isPresent());
        assertEquals(roleAdmin, found.get());
    }

    @Test
    void findAll_returnsAllRoles() {
        roleManager.add(roleAdmin);
        roleManager.add(roleViewer);
        List<Role> all = roleManager.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void findByFilter_returnsMatchingRoles() {
        roleAdmin.addPermission(permRead);
        roleAdmin.addPermission(permWrite);
        roleViewer.addPermission(permRead);
        roleManager.add(roleAdmin);
        roleManager.add(roleViewer);

        List<Role> result = roleManager.findByFilter(RoleFilters.hasAtLeastNPermissions(2));
        assertEquals(1, result.size());
        assertEquals(roleAdmin, result.get(0));
    }

    @Test
    void findAll_withFilterAndSorter() {
        roleManager.add(roleViewer);
        roleManager.add(roleAdmin);
        List<Role> result = roleManager.findAll(
                RoleFilters.byNameContains("er"),
                RoleSorters.byName()
        );
        assertEquals(2, result.size());
        assertEquals("Admin", result.get(0).getName());
    }

    @Test
    void addPermissionToRole_success() {
        roleManager.add(roleAdmin);
        roleManager.addPermissionToRole("Admin", permRead);
        assertTrue(roleAdmin.hasPermission(permRead));
    }

    @Test
    void addPermissionToRole_roleNotFound_throwsException() {
        assertThrows(RoleNotFoundException.class, () -> roleManager.addPermissionToRole("Ghost", permRead));
    }

    @Test
    void removePermissionFromRole_success() {
        roleManager.add(roleAdmin);
        roleManager.addPermissionToRole("Admin", permRead);
        roleManager.removePermissionFromRole("Admin", permRead);
        assertFalse(roleAdmin.hasPermission(permRead));
    }

    @Test
    void findRolesWithPermission_returnsCorrectRoles() {
        roleAdmin.addPermission(permRead);
        roleViewer.addPermission(permRead);
        roleManager.add(roleAdmin);
        roleManager.add(roleViewer);
        List<Role> result = roleManager.findRolesWithPermission("READ", "docs");
        assertEquals(2, result.size());
    }

    @Test
    void clear_removesAll() {
        roleManager.add(roleAdmin);
        roleManager.add(roleViewer);
        roleManager.clear();
        assertEquals(0, roleManager.count());
    }
}