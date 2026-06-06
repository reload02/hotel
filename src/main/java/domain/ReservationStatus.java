package domain;

public enum ReservationStatus {
    RESERVED("RESERVED", "예약됨"),
    CANCEL_PENDING("CANCEL_PENDING", "취소 승인 대기"),
    CANCELLED("CANCELLED", "취소됨"),
    CHECKED_IN("CHECKED_IN", "입실"),
    CHECKED_OUT("CHECKED_OUT", "퇴실");

    private final String code;
    private final String displayName;

    ReservationStatus(String code, String displayName) {
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
