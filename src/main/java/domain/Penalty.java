package domain;

import java.time.LocalDate;

public final class Penalty {
    public static final String GLOBAL_POSTAL_CODE = "ALL";

    private final String guestId;
    private String postalCode;
    private final LocalDate endDate;

    public Penalty(String guestId, String postalCode, LocalDate endDate) {
        this.guestId = guestId;
        this.postalCode = postalCode;
        this.endDate = endDate;
    }

    public String getGuestId() {
        return guestId;
    }

    public String getGuestIdKey() {
        return guestId.toLowerCase();
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isGlobal() {
        return GLOBAL_POSTAL_CODE.equals(postalCode);
    }

    public boolean isActiveOn(LocalDate today) {
        return !today.isAfter(endDate);
    }

    public boolean affects(String targetPostalCode) {
        return isGlobal() || postalCode.equals(targetPostalCode);
    }
}
