package domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Reservation {
    private final String guestId;
    private String hotelName;
    private String postalCode;
    private int roomNumber;
    private int guestCount;
    private LocalDate checkInDate;
    private LocalTime checkInTime;
    private LocalDate checkOutDate;
    private LocalTime checkOutTime;
    private ReservationStatus status;
    private final LocalDateTime createdAt;
    private LocalDate cancelRequestDate;
    private LocalTime cancelRequestTime;
    private Double rating;
    private boolean automaticSuspensionCancellation;

    public Reservation(String guestId,
                       String hotelName,
                       String postalCode,
                       int roomNumber,
                       int guestCount,
                       LocalDate checkInDate,
                       LocalTime checkInTime,
                       LocalDate checkOutDate,
                       LocalTime checkOutTime,
                       ReservationStatus status,
                       LocalDateTime createdAt,
                       LocalDate cancelRequestDate,
                       LocalTime cancelRequestTime,
                       Double rating) {
        this(guestId, hotelName, postalCode, roomNumber, guestCount,
                checkInDate, checkInTime, checkOutDate, checkOutTime,
                status, createdAt, cancelRequestDate, cancelRequestTime, rating, false);
    }

    public Reservation(String guestId,
                       String hotelName,
                       String postalCode,
                       int roomNumber,
                       int guestCount,
                       LocalDate checkInDate,
                       LocalTime checkInTime,
                       LocalDate checkOutDate,
                       LocalTime checkOutTime,
                       ReservationStatus status,
                       LocalDateTime createdAt,
                       LocalDate cancelRequestDate,
                       LocalTime cancelRequestTime,
                       Double rating,
                       boolean automaticSuspensionCancellation) {
        this.guestId = guestId;
        this.hotelName = hotelName;
        this.postalCode = postalCode;
        this.roomNumber = roomNumber;
        this.guestCount = guestCount;
        this.checkInDate = checkInDate;
        this.checkInTime = checkInTime;
        this.checkOutDate = checkOutDate;
        this.checkOutTime = checkOutTime;
        this.status = status;
        this.createdAt = createdAt;
        this.cancelRequestDate = cancelRequestDate;
        this.cancelRequestTime = cancelRequestTime;
        this.rating = rating;
        this.automaticSuspensionCancellation = automaticSuspensionCancellation;
    }

    public String getGuestId() {
        return guestId;
    }

    public String getGuestIdKey() {
        return guestId.toLowerCase();
    }

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelSnapshot(String hotelName, String postalCode, int roomNumber) {
        this.hotelName = hotelName;
        this.postalCode = postalCode;
        this.roomNumber = roomNumber;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public int getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(int guestCount) {
        this.guestCount = guestCount;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public LocalTime getCheckOutTime() {
        return checkOutTime;
    }

    public LocalDateTime getCheckInDateTime() {
        return LocalDateTime.of(checkInDate, checkInTime);
    }

    public LocalDateTime getCheckOutDateTime() {
        return LocalDateTime.of(checkOutDate, checkOutTime);
    }

    public void setStayPeriod(LocalDate checkInDate, LocalTime checkInTime,
                              LocalDate checkOutDate, LocalTime checkOutTime) {
        this.checkInDate = checkInDate;
        this.checkInTime = checkInTime;
        this.checkOutDate = checkOutDate;
        this.checkOutTime = checkOutTime;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDate getCancelRequestDate() {
        return cancelRequestDate;
    }

    public LocalTime getCancelRequestTime() {
        return cancelRequestTime;
    }

    public LocalDateTime getCancelRequestDateTime() {
        if (cancelRequestDate == null || cancelRequestTime == null) {
            return null;
        }
        return LocalDateTime.of(cancelRequestDate, cancelRequestTime);
    }

    public void setCancelRequest(LocalDate date, LocalTime time) {
        this.cancelRequestDate = date;
        this.cancelRequestTime = time;
    }

    public void clearCancelRequest() {
        this.cancelRequestDate = null;
        this.cancelRequestTime = null;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public boolean isAutomaticSuspensionCancellation() {
        return automaticSuspensionCancellation;
    }

    public void markAutomaticSuspensionCancellation() {
        this.automaticSuspensionCancellation = true;
    }

    public boolean isBlockingReservation() {
        return status != ReservationStatus.CANCELLED;
    }

    public boolean overlaps(LocalDateTime from, LocalDateTime to) {
        return getCheckInDateTime().isBefore(to) && from.isBefore(getCheckOutDateTime());
    }
}
