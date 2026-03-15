package rbac.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rbac.core.User;
import rbac.exceptions.DuplicateUserException;
import rbac.exceptions.UserNotFoundException;
import rbac.filters.UserFilters;
import rbac.sorters.UserSorters;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserManagerTest {
    private UserManager userManager;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        user1 = User.validate("john_doe", "John Doe", "john@example.com");
        user2 = User.validate("jane_doe", "Jane Doe", "jane@example.com");
    }

    @Test
    void addUser_success() {
        userManager.add(user1);
        assertEquals(1, userManager.count());
        assertTrue(userManager.exists("john_doe"));
    }

    @Test
    void addUser_duplicate_throwsException() {
        userManager.add(user1);
        assertThrows(DuplicateUserException.class, () -> userManager.add(user1));
    }

    @Test
    void removeUser_success() {
        userManager.add(user1);
        boolean removed = userManager.remove(user1);
        assertTrue(removed);
        assertEquals(0, userManager.count());
    }

    @Test
    void findById_returnsUser() {
        userManager.add(user1);
        Optional<User> found = userManager.findById("john_doe");
        assertTrue(found.isPresent());
        assertEquals(user1, found.get());
    }

    @Test
    void findByUsername_returnsUser() {
        userManager.add(user1);
        Optional<User> found = userManager.findByUsername("john_doe");
        assertTrue(found.isPresent());
        assertEquals(user1, found.get());
    }

    @Test
    void findByEmail_returnsUser() {
        userManager.add(user1);
        Optional<User> found = userManager.findByEmail("john@example.com");
        assertTrue(found.isPresent());
        assertEquals(user1, found.get());
    }

    @Test
    void findAll_returnsAllUsers() {
        userManager.add(user1);
        userManager.add(user2);
        List<User> all = userManager.findAll();
        assertEquals(2, all.size());
        assertTrue(all.contains(user1));
        assertTrue(all.contains(user2));
    }

    @Test
    void findByFilter_returnsMatchingUsers() {
        userManager.add(user1);
        userManager.add(user2);
        List<User> result = userManager.findByFilter(UserFilters.byUsernameContains("john"));
        assertEquals(1, result.size());
        assertEquals(user1, result.get(0));
    }

    @Test
    void findAll_withFilterAndSorter() {
        userManager.add(user1);
        userManager.add(user2);
        List<User> result = userManager.findAll(
                UserFilters.byUsernameContains("doe"),
                UserSorters.byUsername()
        );
        assertEquals(2, result.size());
        assertEquals("jane_doe", result.get(0).username());
    }

    @Test
    void exists_returnsTrueForExisting() {
        userManager.add(user1);
        assertTrue(userManager.exists("john_doe"));
        assertFalse(userManager.exists("nonexistent"));
    }

    @Test
    void update_existingUser_success() {
        userManager.add(user1);
        userManager.update("john_doe", "John Updated", "john.updated@example.com");
        User updated = userManager.findByUsername("john_doe").orElseThrow();
        assertEquals("John Updated", updated.fullName());
        assertEquals("john.updated@example.com", updated.email());
    }

    @Test
    void update_nonexistentUser_throwsException() {
        assertThrows(UserNotFoundException.class, () -> userManager.update("nobody", "No", "no@no.com"));
    }

    @Test
    void clear_removesAll() {
        userManager.add(user1);
        userManager.add(user2);
        userManager.clear();
        assertEquals(0, userManager.count());
    }
}