package service;

import domain.Penalty;
import domain.User;
import persistence.FlatFileStore;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PenaltyService {
    private final FlatFileStore store;

    public PenaltyService(FlatFileStore store) {
        this.store = store;
    }

    public boolean hasActiveGlobalPenalty(User user, LocalDate today) {
        for (Penalty penalty : activePenaltiesOf(user, today)) {
            if (penalty.isGlobal()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasActivePenaltyOn(User user, String postalCode, LocalDate today) {
        for (Penalty penalty : activePenaltiesOf(user, today)) {
            if (penalty.affects(postalCode)) {
                return true;
            }
        }
        return false;
    }

    public List<Penalty> activePenaltiesOf(User user, LocalDate today) {
        List<Penalty> results = new ArrayList<>();
        for (Penalty penalty : store.penalties()) {
            if (penalty.getGuestIdKey().equals(user.getIdKey()) && penalty.isActiveOn(today)) {
                results.add(penalty);
            }
        }
        return results;
    }

    public void grantGlobal(User user, LocalDate endDate) {
        store.penalties().add(new Penalty(user.getId(), Penalty.GLOBAL_POSTAL_CODE, endDate));
        store.savePenalties();
    }

    public void grantHotel(User user, String postalCode, LocalDate endDate) {
        store.penalties().add(new Penalty(user.getId(), postalCode, endDate));
        store.savePenalties();
    }

    public boolean purgeExpired(LocalDate today) {
        boolean changed = false;
        Iterator<Penalty> iterator = store.penalties().iterator();
        while (iterator.hasNext()) {
            Penalty penalty = iterator.next();
            if (today.isAfter(penalty.getEndDate())) {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            store.savePenalties();
        }
        return changed;
    }
}
