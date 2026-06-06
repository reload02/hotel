package domain;

public enum Role {
    HOST("host", "사장"),
    GUEST("guest", "고객"),
    ADMIN("admin", "관리자");

    private final String code;
    private final String displayName;

    Role(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }
}
