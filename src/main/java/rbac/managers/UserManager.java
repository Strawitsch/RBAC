package rbac.managers;

import rbac.core.User;
import rbac.exceptions.*;
import rbac.filters.UserFilter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class UserManager implements Repository<User> {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void add(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        lock.writeLock().lock();
        try{
            if (users.containsKey(user.username())) {
                throw new DuplicateUserException("User with username " + user.username() + " already exists");
            }
            users.put(user.username(), user);
        } finally {
            lock.writeLock().unlock();
        }

    }

    @Override
    public boolean remove(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        lock.writeLock().lock();
        try{
            return users.remove(user.username(), user);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<User> findById(String id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(users.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<User> findByUsername(String username) {
            return findById(username);

    }

    public Optional<User> findByEmail(String email) {
        lock.readLock().lock();
        try{
            return users.values().stream()
                    .filter(u -> u.email().equals(email))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<User> findAll() {
        lock.readLock().lock();
        try{
            return new ArrayList<>(users.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> findByFilter(UserFilter filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        lock.readLock().lock();
        try{
            return users.values().stream()
                    .filter(filter)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> findByFilterParallel(UserFilter filter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        lock.readLock().lock();
        try {
            return users.values().parallelStream()
                    .filter(filter)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        Objects.requireNonNull(filter, "Filter cannot be null");
        Objects.requireNonNull(sorter, "Comparator cannot be null");
        lock.readLock().lock();
        try{
            return users.values().stream()
                    .filter(filter)
                    .sorted(sorter)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean exists(String username) {
        lock.readLock().lock();
        try{
            return users.containsKey(username);
        } finally {
            lock.readLock().unlock();
        }

    }

    public void update(String username, String newFullName, String newEmail) {
        lock.writeLock().lock();
        try{
            User existing = users.get(username);
            if (existing == null) {
                throw new UserNotFoundException("User " + username + " not found");
            }
            User updated = User.validate(username, newFullName, newEmail);
            users.put(username, updated);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int count() {
        return users.size();
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try{
            users.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}