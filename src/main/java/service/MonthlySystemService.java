package service;

import domain.Hotel;
import domain.Reservation;
import domain.ReservationStatus;
import persistence.FlatFileStore;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class MonthlySystemService {
    private static final int SUSPENSION_HOURS = 336;

    private final FlatFileStore store;
    private final SuspensionService suspensionService;

    public MonthlySystemService(FlatFileStore store, SuspensionService suspensionService) {
        this.store = store;
        this.suspensionService = suspensionService;
    }

    public void runIfNeeded(LocalDateTime now) {
        YearMonth targetMonth = YearMonth.from(now.minusMonths(1));
        YearMonth lastChecked = store.systemState().getLastCheckedMonth();
        if (lastChecked != null && !lastChecked.isBefore(targetMonth)) {
            return;
        }

        LocalDateTime startAt = targetMonth.plusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime endAt = startAt.plusHours(SUSPENSION_HOURS);
        for (Hotel hotel : findLowRatedHotels(targetMonth)) {
            suspensionService.grantAutomatic(hotel.getPostalCode(), startAt, endAt, SuspensionService.LOW_RATING_REASON);
        }
        store.systemState().setLastCheckedMonth(targetMonth);
        store.saveSystem();
    }

    public List<Hotel> findLowRatedHotels(YearMonth targetMonth) {
        List<Hotel> results = new ArrayList<>();
        for (Hotel hotel : store.hotels()) {
            double total = 0.0;
            int count = 0;
            for (Reservation reservation : store.reservations()) {
                if (!reservation.getPostalCode().equals(hotel.getPostalCode())) {
                    continue;
                }
                if (reservation.getStatus() != ReservationStatus.CHECKED_OUT || reservation.getRating() == null) {
                    continue;
                }
                if (!YearMonth.from(reservation.getCheckOutDate()).equals(targetMonth)) {
                    continue;
                }
                total += reservation.getRating();
                count++;
            }
            if (count > 0 && Math.floor(total / count) < 2.0) {
                results.add(hotel);
            }
        }
        return results;
    }
}
