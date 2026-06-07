package service;

import domain.Hotel;
import domain.Penalty;
import domain.Reservation;
import domain.ReservationStatus;
import domain.Room;
import domain.User;
import persistence.FlatFileStore;
import ui.ConsoleIO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GuestService {
    private final FlatFileStore store;
    private final ConsoleIO io;
    private final PenaltyService penaltyService;
    private final SuspensionService suspensionService;

    public GuestService(FlatFileStore store, ConsoleIO io,
                        PenaltyService penaltyService,
                        SuspensionService suspensionService) {
        this.store = store;
        this.io = io;
        this.penaltyService = penaltyService;
        this.suspensionService = suspensionService;
    }

    public List<Hotel> listOpenHotels(LocalDateTime now) {
        List<Hotel> results = new ArrayList<>();
        for (Hotel hotel : store.hotels()) {
            if (!isHotelSuspendedAt(hotel.getPostalCode(), now)) {
                results.add(hotel);
            }
        }
        results.sort(Comparator.comparing(Hotel::getName).thenComparing(Hotel::getPostalCode));
        return results;
    }

    public List<Hotel> searchOpenHotels(String keyword, LocalDateTime now) {
        String normalized = keyword.replace(" ", "").toLowerCase();
        List<Hotel> results = new ArrayList<>();
        for (Hotel hotel : listOpenHotels(now)) {
            if (hotel.normalizedNameForSearch().contains(normalized)) {
                results.add(hotel);
            }
        }
        return results;
    }

    public boolean isHotelSuspendedAt(String postalCode, LocalDateTime now) {
        return suspensionService.isActiveAt(postalCode, now);
    }

    public boolean isHotelSuspensionOverlapping(String postalCode, LocalDateTime from, LocalDateTime to) {
        return suspensionService.overlaps(postalCode, from, to);
    }

    public Hotel findHotelByPostalCode(String postalCode) {
        for (Hotel hotel : store.hotels()) {
            if (hotel.getPostalCode().equals(postalCode)) {
                return hotel;
            }
        }
        return null;
    }

    public List<Room> roomsByHotel(String postalCode) {
        List<Room> results = new ArrayList<>();
        for (Room room : store.rooms()) {
            if (room.getPostalCode().equals(postalCode)) {
                results.add(room);
            }
        }
        results.sort(Comparator.comparingInt(Room::getRoomNumber));
        return results;
    }

    public Room findRoom(String postalCode, int roomNumber) {
        for (Room room : store.rooms()) {
            if (room.getPostalCode().equals(postalCode) && room.getRoomNumber() == roomNumber) {
                return room;
            }
        }
        return null;
    }

    public boolean isDateBlocked(String postalCode, int roomNumber, LocalDate date,
                                 LocalDate baselineDate, Reservation exclude) {
        if (!date.isAfter(baselineDate)) {
            return true;
        }
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        return isTimeOverlapping(postalCode, roomNumber, dayStart, dayEnd, exclude)
                || isHotelSuspensionOverlapping(postalCode, dayStart, dayEnd);
    }

    public boolean isTimeOverlapping(String postalCode, int roomNumber,
                                     LocalDateTime newStart, LocalDateTime newEnd,
                                     Reservation exclude) {
        for (Reservation reservation : store.reservations()) {
            if (reservation == exclude) {
                continue;
            }
            if (!reservation.getPostalCode().equals(postalCode) || reservation.getRoomNumber() != roomNumber) {
                continue;
            }
            if (reservation.isBlockingReservation() && reservation.overlaps(newStart, newEnd)) {
                return true;
            }
        }
        return false;
    }

    public Reservation createReservation(User guest, Hotel hotel, Room room, int guestCount,
                                         LocalDate checkInDate, LocalTime checkInTime,
                                         LocalTime checkOutTime) {
        LocalDate checkOutDate = checkInDate.plusDays(1);
        validateReservable(guest, hotel.getPostalCode(), room, guestCount,
                LocalDateTime.of(checkInDate, checkInTime),
                LocalDateTime.of(checkOutDate, checkOutTime), null);
        Reservation reservation = new Reservation(
                guest.getId(), hotel.getName(), hotel.getPostalCode(), room.getRoomNumber(), guestCount,
                checkInDate, checkInTime, checkOutDate, checkOutTime,
                ReservationStatus.RESERVED, store.baselineDateTime(), null, null, null
        );
        store.reservations().add(reservation);
        store.saveReservations();
        return reservation;
    }

    public void replaceReservation(User guest,
                                   Reservation reservation,
                                   Hotel hotel,
                                   Room room,
                                   int guestCount,
                                   LocalDate checkInDate,
                                   LocalTime checkInTime,
                                   LocalTime checkOutTime) {
        if (!reservation.getGuestIdKey().equals(guest.getIdKey())
                || reservation.getStatus() != ReservationStatus.RESERVED
                || !store.baselineDateTime().isBefore(reservation.getCheckInDateTime())) {
            throw new IllegalArgumentException("변경할 수 없는 예약입니다.");
        }
        if (hasActivePenaltyOn(guest, reservation.getPostalCode())) {
            throw new IllegalArgumentException("페널티를 보유하고 있어 예약이 불가합니다.");
        }
        if (!reservation.getPostalCode().equals(hotel.getPostalCode())) {
            throw new IllegalArgumentException("예약변경 시 업소를 변경할 수 없습니다.");
        }
        LocalDate checkOutDate = checkInDate.plusDays(1);
        LocalDateTime newStart = LocalDateTime.of(checkInDate, checkInTime);
        LocalDateTime newEnd = LocalDateTime.of(checkOutDate, checkOutTime);
        validateReservable(guest, hotel.getPostalCode(), room, guestCount, newStart, newEnd, reservation);

        long daysUntilOldCheckIn = ChronoUnit.DAYS.between(store.baselineDate(), reservation.getCheckInDate());
        if (daysUntilOldCheckIn <= 3) {
            penaltyService.grantHotel(guest, reservation.getPostalCode(), store.baselineDate().plusDays(3));
            io.println("예약 날짜로부터 3일 이내에 예약을 변경하여 해당 업소 한정 3일간 예약 불가 페널티를 부여합니다.");
        }

        reservation.setHotelSnapshot(hotel.getName(), hotel.getPostalCode(), room.getRoomNumber());
        reservation.setGuestCount(guestCount);
        reservation.setStayPeriod(checkInDate, checkInTime, checkOutDate, checkOutTime);
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.clearCancelRequest();
        reservation.setRating(null);
        store.saveReservations();
    }

    public void updateReservation(Reservation reservation) {
        store.saveReservations();
    }

    public List<Reservation> reservationsOf(User guest) {
        List<Reservation> results = new ArrayList<>();
        for (Reservation reservation : store.reservations()) {
            if (reservation.getGuestIdKey().equals(guest.getIdKey())) {
                results.add(reservation);
            }
        }
        results.sort(Comparator.comparing(Reservation::getCheckInDateTime)
                .thenComparing(Reservation::getPostalCode)
                .thenComparingInt(Reservation::getRoomNumber));
        return results;
    }

    public int recentCancellationCount(User guest) {
        LocalDate today = store.baselineDate();
        int count = 0;
        for (Reservation reservation : store.reservations()) {
            if (!reservation.getGuestIdKey().equals(guest.getIdKey())) {
                continue;
            }
            if (reservation.getCancelRequestDate() == null) {
                continue;
            }
            if (reservation.getStatus() != ReservationStatus.CANCELLED
                    && reservation.getStatus() != ReservationStatus.CANCEL_PENDING) {
                continue;
            }
            if (suspensionService.isAutomaticSuspensionCancellation(reservation)) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(reservation.getCancelRequestDate(), today);
            if (days >= 0 && days < 7) {
                count++;
            }
        }
        return count;
    }

    public boolean canCancel(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.RESERVED
                && store.baselineDateTime().isBefore(reservation.getCheckInDateTime());
    }

    public boolean canChange(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.RESERVED
                && store.baselineDateTime().isBefore(reservation.getCheckInDateTime());
    }

    public boolean canCheckIn(Reservation reservation) {
        LocalDateTime now = store.baselineDateTime();
        return reservation.getStatus() == ReservationStatus.RESERVED
                && now.toLocalDate().equals(reservation.getCheckInDate())
                && !now.isBefore(reservation.getCheckInDateTime());
    }

    public boolean canCheckOut(Reservation reservation) {
        LocalDateTime now = store.baselineDateTime();
        return reservation.getStatus() == ReservationStatus.CHECKED_IN
                && now.toLocalDate().equals(reservation.getCheckOutDate())
                && !now.isBefore(reservation.getCheckOutDateTime());
    }

    public boolean canWriteReview(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.CHECKED_OUT
                && reservation.getRating() == null;
    }

    public void requestCancel(User guest, Reservation reservation) {
        LocalDateTime now = store.baselineDateTime();
        if (hasActivePenaltyOn(guest, reservation.getPostalCode())) {
            throw new IllegalArgumentException("페널티를 보유하고 있어 예약취소가 불가합니다.");
        }
        if (!canCancel(reservation)) {
            throw new IllegalArgumentException("취소할 수 없는 예약입니다.");
        }
        int recentCount = recentCancellationCount(guest);
        if (recentCount >= 3) {
            io.println("1주일 내 취소 요청을 3회 초과 하였기 때문에 2주간 예약불가 페널티를 부여합니다.");
            penaltyService.grantGlobal(guest, now.toLocalDate().plusDays(14));
            return;
        }
        long daysUntilReservation = ChronoUnit.DAYS.between(now.toLocalDate(), reservation.getCheckInDate());
        reservation.setCancelRequest(now.toLocalDate(), now.toLocalTime());
        if (daysUntilReservation <= 3) {
            reservation.setStatus(ReservationStatus.CANCEL_PENDING);
            io.println("예약일이 현재로부터 3일 이내이기 때문에 취소 승인 대기로 설정되었습니다.");
        } else {
            reservation.setStatus(ReservationStatus.CANCELLED);
            io.println("예약이 취소되었습니다.");
        }
        store.saveReservations();
    }

    public boolean checkIn(User guest, Reservation reservation) {
        LocalDateTime now = store.baselineDateTime();
        if (!reservation.getGuestIdKey().equals(guest.getIdKey())
                || !canCheckIn(reservation)) {
            return false;
        }
        if (!now.isBefore(reservation.getCheckInDateTime().plusHours(1))) {
            penaltyService.grantHotel(guest, reservation.getPostalCode(), now.toLocalDate().plusDays(7));
        }
        reservation.setStatus(ReservationStatus.CHECKED_IN);
        store.saveReservations();
        return true;
    }

    public boolean checkOut(User guest, Reservation reservation) {
        LocalDateTime now = store.baselineDateTime();
        if (!reservation.getGuestIdKey().equals(guest.getIdKey())
                || !canCheckOut(reservation)) {
            return false;
        }
        if (!now.isBefore(reservation.getCheckOutDateTime().plusHours(1))) {
            penaltyService.grantHotel(guest, reservation.getPostalCode(), now.toLocalDate().plusDays(7));
        }
        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        store.saveReservations();
        return true;
    }

    public boolean writeReview(User guest, Reservation reservation, double rating) {
        if (!reservation.getGuestIdKey().equals(guest.getIdKey())
                || !canWriteReview(reservation)) {
            return false;
        }
        reservation.setRating(rating);
        store.saveReservations();
        return true;
    }

    public boolean restoreExpiredCancelPending(LocalDateTime now) {
        boolean changed = false;
        for (Reservation reservation : store.reservations()) {
            if (shouldRestoreCancelPending(reservation, now)) {
                reservation.setStatus(ReservationStatus.RESERVED);
                reservation.clearCancelRequest();
                changed = true;
            }
        }
        if (changed) {
            store.saveReservations();
        }
        return changed;
    }

    public boolean shouldRestoreCancelPending(Reservation reservation, LocalDateTime now) {
        return reservation.getStatus() == ReservationStatus.CANCEL_PENDING
                && !now.isBefore(reservation.getCheckInDateTime().minusHours(24));
    }

    public LocalDate baselineDate() {
        return store.baselineDate();
    }

    public LocalDateTime baselineDateTime() {
        return store.baselineDateTime();
    }

    public List<Penalty> activePenaltiesOf(User user) {
        return penaltyService.activePenaltiesOf(user, store.baselineDate());
    }

    public boolean hasActiveGlobalPenalty(User user) {
        return penaltyService.hasActiveGlobalPenalty(user, store.baselineDate());
    }

    public boolean hasActivePenaltyOn(User user, String postalCode) {
        return penaltyService.hasActivePenaltyOn(user, postalCode, store.baselineDate());
    }

    private void validateReservable(User guest, String postalCode, Room room, int guestCount,
                                    LocalDateTime start, LocalDateTime end, Reservation exclude) {
        LocalDate today = store.baselineDate();
        if (hasActivePenaltyOn(guest, postalCode)) {
            throw new IllegalArgumentException("페널티를 보유하고 있어 예약이 불가합니다.");
        }
        if (!start.toLocalDate().isAfter(today)) {
            throw new IllegalArgumentException("당일 또는 과거 예약은 불가능합니다.");
        }
        if (!start.isAfter(store.baselineDateTime())) {
            throw new IllegalArgumentException("예약 시작 시각은 현재 기준 시각보다 이후여야 합니다.");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("체크아웃 날짜시간은 체크인 날짜시간보다 늦어야 합니다.");
        }
        if (start.toLocalTime().isBefore(room.getCheckInTime())
                || end.toLocalTime().isAfter(room.getCheckOutTime())) {
            throw new IllegalArgumentException("체크인은 객실 기본 체크인 시간보다 같거나 늦어야 하고, "
                    + "체크아웃은 객실 기본 체크아웃 시간보다 같거나 빨라야 합니다.");
        }
        if (guestCount > room.getCapacity()) {
            throw new IllegalArgumentException("예약 인원이 객실 정원을 초과합니다.");
        }
        if (isHotelSuspensionOverlapping(postalCode, start, end)) {
            throw new IllegalArgumentException("선택한 기간은 업소 영업정지 기간에 포함됩니다.");
        }
        if (isTimeOverlapping(postalCode, room.getRoomNumber(), start, end, exclude)) {
            throw new IllegalArgumentException("이미 점유된 시간과 겹쳐 예약할 수 없습니다.");
        }
    }
}
