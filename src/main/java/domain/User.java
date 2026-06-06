package domain;

public class User {
    private final String id;
    private final String password;
    private final String name;
    private final Role role;

    public User(String id, String password, String name, Role role) {
        this.id = id;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getIdKey() {
        return id.toLowerCase();
    }

    public String getPassword() {
        return password;
    }

    public boolean matchesPassword(String rawPassword) {
        return password.equals(rawPassword);
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }
}
