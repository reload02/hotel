package domain;

import java.time.LocalDateTime;

public final class Suspension {
    private String postalCode;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final String reason;

    public Suspension(String postalCode, LocalDateTime startAt, LocalDateTime endAt, String reason) {
        this.postalCode = postalCode;
        this.startAt = startAt;
        this.endAt = endAt;
        this.reason = reason;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public String getReason() {
        return reason;
    }

    public boolean isActiveAt(LocalDateTime now) {
        return !now.isBefore(startAt) && now.isBefore(endAt);
    }

    public boolean overlaps(LocalDateTime from, LocalDateTime to) {
        return startAt.isBefore(to) && from.isBefore(endAt);
    }
}
