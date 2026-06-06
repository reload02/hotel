package service;

import domain.Role;
import domain.User;
import persistence.FlatFileStore;

public class AuthService {
    private final FlatFileStore store;

    public AuthService(FlatFileStore store) {
        this.store = store;
    }

    public User authenticate(String id, String password) {
        User user = findUserById(id);
        if (user == null) {
            return null;
        }
        return user.matchesPassword(password) ? user : null;
    }

    public User findUserById(String id) {
        for (User user : store.users()) {
            if (user.getIdKey().equals(id.toLowerCase())) {
                return user;
            }
        }
        return null;
    }

    public boolean existsUserId(String id) {
        return findUserById(id) != null;
    }

    public User register(String name, String id, String password, Role role) {
        User user = new User(id, password, name, role);
        store.users().add(user);
        store.saveUsers();
        return user;
    }
}
