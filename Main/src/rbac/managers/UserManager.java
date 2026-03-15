package rbac.managers;

import rbac.core.User;
import rbac.exceptions.*;
import rbac.filters.UserFilter;

import java.util.*;
import java.util.stream.Collectors;

public class UserManager implements Repository<User> {
    private final Map<String, User> users = new HashMap<>();

    @Override
    public void add(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        if (users.containsKey(user.username())) {
            throw new DuplicateUserException("User with username " + user.username() + " already exists");
        }
        users.put(user.username(), user);
    }

    @Override
    public boolean remove(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        return users.remove(user.username(), user);
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    public Optional<User> findByUsername(String username) {
        return findById(username);
    }

    public Optional<User> findByEmail(String email) {
        return users.values().stream()
                .filter(u -> u.email().equals(email))
                .findFirst();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    public List<User> findByFilter(UserFilter filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        return users.values().stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(sorter, "Comparator cannot be null");
        return users.values().stream()
                .filter(filter)
                .sorted(sorter)
                .collect(Collectors.toList());
    }

    public boolean exists(String username) {
        return users.containsKey(username);
    }

    public void update(String username, String newFullName, String newEmail) {
        User existing = users.get(username);
        if (existing == null) {
            throw new UserNotFoundException("User " + username + " not found");
        }
        User updated = User.validate(username, newFullName, newEmail);
        users.put(username, updated);
    }

    @Override
    public int count() {
        return users.size();
    }

    @Override
    public void clear() {
        users.clear();
    }
}