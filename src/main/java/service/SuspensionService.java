package service;

import common.FatalDataException;
import domain.Hotel;
import domain.Reservation;
import domain.ReservationStatus;
import domain.Suspension;
import persistence.FlatFileStore;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SuspensionService {
    public static final String LOW_RATING_REASON = "LOW_RATING_MONTHLY";

    private final FlatFileStore store;

    public SuspensionService(FlatFileStore store) {
        this.store = store;
    }

    public List<Suspension> suspensionsOf(String postalCode) {
        List<Suspension> results = new ArrayList<>();
        for (Suspension suspension : store.suspensions()) {
            if (suspension.getPostalCode().equals(postalCode)) {
                results.add(suspension);
            }
        }
        return results;
    }

    public boolean isActiveAt(String postalCode, LocalDateTime now) {
        for (Suspension suspension : store.suspensions()) {
            if (suspension.getPostalCode().equals(postalCode) && suspension.isActiveAt(now)) {
                return true;
            }
        }
        return false;
    }

    public boolean overlaps(String postalCode, LocalDateTime from, LocalDateTime to) {
        for (Suspension suspension : store.suspensions()) {
            if (suspension.getPostalCode().equals(postalCode) && suspension.overlaps(from, to)) {
                return true;
            }
        }
        return false;
    }

    public Suspension grantAutomatic(String postalCode, LocalDateTime startAt, LocalDateTime endAt, String reason) {
        if (!startAt.isBefore(endAt)) {
            throw new FatalDataException("영업정지 시작 시각은 종료 시각보다 이전이어야 합니다.");
        }
        if (findHotel(postalCode) == null) {
            throw new FatalDataException("존재하지 않는 호텔 우편번호입니다.");
        }
        Suspension suspension = new Suspension(postalCode, startAt, endAt, reason);
        store.suspensions().add(suspension);
        cancelOverlappingReservations(postalCode, startAt, endAt);
        store.saveSuspensions();
        store.saveReservations();
        return suspension;
    }

    public boolean purgeExpired(LocalDateTime now) {
        boolean changed = false;
        Iterator<Suspension> iterator = store.suspensions().iterator();
        while (iterator.hasNext()) {
            Suspension suspension = iterator.next();
            if (!now.isBefore(suspension.getEndAt())) {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            store.saveSuspensions();
        }
        return changed;
    }

    public void cancelOverlappingReservations(String postalCode, LocalDateTime startAt, LocalDateTime endAt) {
        for (Reservation reservation : store.reservations()) {
            if (!reservation.getPostalCode().equals(postalCode)) {
                continue;
            }
            if (reservation.getStatus() != ReservationStatus.RESERVED
                    && reservation.getStatus() != ReservationStatus.CANCEL_PENDING) {
                continue;
            }
            if (!reservation.overlaps(startAt, endAt)) {
                continue;
            }
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservation.setCancelRequest(startAt.toLocalDate(), LocalTime.MIDNIGHT);
            reservation.setRating(null);
        }
    }

    public boolean isAutomaticSuspensionCancellation(Reservation reservation) {
        LocalDateTime cancelAt = reservation.getCancelRequestDateTime();
        if (cancelAt == null || reservation.getStatus() != ReservationStatus.CANCELLED) {
            return false;
        }
        for (Suspension suspension : store.suspensions()) {
            if (!suspension.getPostalCode().equals(reservation.getPostalCode())) {
                continue;
            }
            if (!LOW_RATING_REASON.equals(suspension.getReason())) {
                continue;
            }
            if (suspension.getStartAt().toLocalDate().equals(reservation.getCancelRequestDate())
                    && reservation.getCancelRequestTime().equals(LocalTime.MIDNIGHT)
                    && reservation.overlaps(suspension.getStartAt(), suspension.getEndAt())) {
                return true;
            }
        }
        return false;
    }

    private Hotel findHotel(String postalCode) {
        for (Hotel hotel : store.hotels()) {
            if (hotel.getPostalCode().equals(postalCode)) {
                return hotel;
            }
        }
        return null;
    }
}
