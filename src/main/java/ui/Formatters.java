package ui;

import domain.Hotel;
import domain.Penalty;
import domain.Reservation;
import domain.Room;
import validation.SpecParsers;

public final class Formatters {
    private Formatters() {
    }

    public static String hotelLine(Hotel hotel) {
        return hotel.getName() + " | " + hotel.getPostalCode();
    }

    public static String roomLine(Room room) {
        return room.getRoomNumber() + " | "
                + SpecParsers.formatTime(room.getCheckInTime()) + ", "
                + SpecParsers.formatTime(room.getCheckOutTime()) + " | "
                + room.getCapacity() + "명";
    }

    public static String ownedRoomLine(Room room) {
        return room.getRoomNumber() + ", "
                + room.getCapacity() + "명, "
                + SpecParsers.formatTime(room.getCheckInTime()) + ", "
                + SpecParsers.formatTime(room.getCheckOutTime());
    }

    public static String reservationShort(Reservation reservation) {
        return "[" + reservation.getStatus().code() + "] "
                + reservation.getHotelName() + " " + reservation.getPostalCode() + " "
                + reservation.getRoomNumber() + "호 "
                + SpecParsers.formatDateTime(reservation.getCheckInDateTime()) + " ~ "
                + SpecParsers.formatDateTime(reservation.getCheckOutDateTime());
    }

    public static String reservationLong(Reservation reservation) {
        String rating = reservation.getRating() == null ? "미작성" : SpecParsers.formatRating(reservation.getRating());
        return "[" + reservation.getStatus().code() + "] "
                + reservation.getHotelName() + " "
                + reservation.getPostalCode() + " "
                + reservation.getRoomNumber() + "호 "
                + SpecParsers.formatDateTime(reservation.getCheckInDateTime()) + " ~ "
                + SpecParsers.formatDateTime(reservation.getCheckOutDateTime()) + " | "
                + "인원 " + reservation.getGuestCount() + "명 | "
                + "생성 " + SpecParsers.formatDateTime(reservation.getCreatedAt()) + " | "
                + "평점 " + rating;
    }

    public static String bookingSummary(Hotel hotel, Room room, int guestCount,
                                        java.time.LocalDate checkInDate,
                                        java.time.LocalTime checkInTime,
                                        java.time.LocalDate checkOutDate,
                                        java.time.LocalTime checkOutTime) {
        return hotel.getName() + ", " + hotel.getPostalCode() + ", " + room.getRoomNumber() + "호, "
                + guestCount + "명, "
                + SpecParsers.formatDate(checkInDate) + " " + SpecParsers.formatTime(checkInTime) + " ~ "
                + SpecParsers.formatDate(checkOutDate) + " " + SpecParsers.formatTime(checkOutTime);
    }

    public static String penaltyLine(Penalty penalty) {
        String scope = penalty.isGlobal() ? "전체 업소" : penalty.getPostalCode();
        return scope + " 예약 제한, 종료일 " + SpecParsers.formatDate(penalty.getEndDate());
    }
}
