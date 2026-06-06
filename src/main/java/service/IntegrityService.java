package service;

import common.FatalDataException;
import domain.Hotel;
import domain.Penalty;
import domain.Reservation;
import domain.ReservationStatus;
import domain.Role;
import domain.Room;
import domain.Suspension;
import domain.User;
import persistence.FlatFileStore;
import ui.ConsoleIO;

import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class IntegrityService {
    private final FlatFileStore store;
    private final ConsoleIO io;
    private final GuestService guestService;
    private final PenaltyService penaltyService;
    private final SuspensionService suspensionService;
    private final MonthlySystemService monthlySystemService;

    public IntegrityService(FlatFileStore store,
                            ConsoleIO io,
                            GuestService guestService,
                            PenaltyService penaltyService,
                            SuspensionService suspensionService,
                            MonthlySystemService monthlySystemService) {
        this.store = store;
        this.io = io;
        this.guestService = guestService;
        this.penaltyService = penaltyService;
        this.suspensionService = suspensionService;
        this.monthlySystemService = monthlySystemService;
    }

    public void initialize() {
        ensureDataFiles();
        store.loadAll();
        validateSemantics();
        runAutomaticMaintenance();
        validateSemantics();
        store.saveAll();
    }

    public void runAutomaticMaintenance() {
        LocalDateTime now = store.baselineDateTime();
        penaltyService.purgeExpired(now.toLocalDate());
        suspensionService.purgeExpired(now);
        guestService.restoreExpiredCancelPending(now);
        monthlySystemService.runIfNeeded(now);
    }

    private void ensureDataFiles() {
        if (!Files.exists(store.getDataDir())) {
            io.println("경고: data 폴더가 없어 새로 생성합니다.");
        }
        store.ensureDirectoryAndFilesExist();
        store.ensureReadableWritable();
    }

    private void validateSemantics() {
        validateUsers();
        validateHotels();
        validateRooms();
        validateReservations();
        validatePenalties();
        validateSuspensions();
    }

    private void validateUsers() {
        Set<String> ids = new HashSet<>();
        for (User user : store.users()) {
            if (!ids.add(user.getIdKey())) {
                throw new FatalDataException("user.txt 의미 오류: 중복 ID " + user.getId());
            }
        }
    }

    private void validateHotels() {
        Set<String> postalCodes = new HashSet<>();
        for (Hotel hotel : store.hotels()) {
            User host = findUserById(hotel.getHostId());
            if (host == null) {
                throw new FatalDataException("hotel.txt 의미 오류: 존재하지 않는 host ID " + hotel.getHostId());
            }
            if (host.getRole() != Role.HOST) {
                throw new FatalDataException("hotel.txt 의미 오류: host 권한이 아닌 ID " + hotel.getHostId());
            }
            if (!postalCodes.add(hotel.getPostalCode())) {
                throw new FatalDataException("hotel.txt 의미 오류: 중복 우편번호 " + hotel.getPostalCode());
            }
        }
    }

    private void validateRooms() {
        Set<String> roomKeys = new HashSet<>();
        for (Room room : store.rooms()) {
            if (findHotelByPostalCode(room.getPostalCode()) == null) {
                throw new FatalDataException("room.txt 의미 오류: 없는 업소 우편번호 " + room.getPostalCode());
            }
            String key = room.getPostalCode() + "#" + room.getRoomNumber();
            if (!roomKeys.add(key)) {
                throw new FatalDataException("room.txt 의미 오류: 동일 업소 내 중복 방 번호 " + room.getRoomNumber());
            }
        }
    }

    private void validateReservations() {
        for (int i = 0; i < store.reservations().size(); i++) {
            Reservation reservation = store.reservations().get(i);
            User guest = findUserById(reservation.getGuestId());
            if (guest == null) {
                throw new FatalDataException("reservation.txt 의미 오류: 존재하지 않는 guest ID " + reservation.getGuestId());
            }
            if (guest.getRole() != Role.GUEST) {
                throw new FatalDataException("reservation.txt 의미 오류: guest 권한이 아닌 ID " + reservation.getGuestId());
            }
            if (!reservation.getCheckOutDateTime().isAfter(reservation.getCheckInDateTime())) {
                throw new FatalDataException("reservation.txt 의미 오류: 체크아웃이 체크인보다 늦어야 합니다.");
            }
            validateReservationStateFields(reservation);
            for (int j = i + 1; j < store.reservations().size(); j++) {
                Reservation other = store.reservations().get(j);
                if (reservation.getPostalCode().equals(other.getPostalCode())
                        && reservation.getRoomNumber() == other.getRoomNumber()
                        && reservation.isBlockingReservation()
                        && other.isBlockingReservation()
                        && reservation.overlaps(other.getCheckInDateTime(), other.getCheckOutDateTime())) {
                    throw new FatalDataException("reservation.txt 의미 오류: 동일 객실 시간 중복 예약");
                }
            }
        }
    }

    private void validateReservationStateFields(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.RESERVED
                && (reservation.getCancelRequestDate() != null || reservation.getCancelRequestTime() != null)) {
            throw new FatalDataException("reservation.txt 의미 오류: RESERVED 상태인데 취소 요청 시각 존재");
        }
        if (reservation.getStatus() == ReservationStatus.CANCEL_PENDING
                && (reservation.getCancelRequestDate() == null || reservation.getCancelRequestTime() == null)) {
            throw new FatalDataException("reservation.txt 의미 오류: CANCEL_PENDING 상태인데 취소 요청 시각 없음");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED
                && (reservation.getCancelRequestDate() == null || reservation.getCancelRequestTime() == null)) {
            throw new FatalDataException("reservation.txt 의미 오류: CANCELLED 상태인데 취소 요청 시각 없음");
        }
        boolean cancelDateOnly = reservation.getCancelRequestDate() != null && reservation.getCancelRequestTime() == null;
        boolean cancelTimeOnly = reservation.getCancelRequestDate() == null && reservation.getCancelRequestTime() != null;
        if (cancelDateOnly || cancelTimeOnly) {
            throw new FatalDataException("reservation.txt 의미 오류: 취소 요청 날짜와 시간이 함께 있어야 합니다.");
        }
        if (reservation.getStatus() != ReservationStatus.CHECKED_OUT && reservation.getRating() != null) {
            throw new FatalDataException("reservation.txt 의미 오류: CHECKED_OUT이 아닌 예약에 평점 존재");
        }
    }

    private void validatePenalties() {
        for (Penalty penalty : store.penalties()) {
            User guest = findUserById(penalty.getGuestId());
            if (guest == null || guest.getRole() != Role.GUEST) {
                throw new FatalDataException("penalty.txt 의미 오류: 패널티 대상은 존재하는 고객이어야 합니다.");
            }
            if (!penalty.isGlobal() && findHotelByPostalCode(penalty.getPostalCode()) == null) {
                throw new FatalDataException("penalty.txt 의미 오류: 없는 업소 우편번호 " + penalty.getPostalCode());
            }
        }
    }

    private void validateSuspensions() {
        for (Suspension suspension : store.suspensions()) {
            if (findHotelByPostalCode(suspension.getPostalCode()) == null) {
                throw new FatalDataException("suspension.txt 의미 오류: 없는 업소 우편번호 " + suspension.getPostalCode());
            }
            if (!suspension.getStartAt().isBefore(suspension.getEndAt())) {
                throw new FatalDataException("suspension.txt 의미 오류: 영업정지 시작은 종료보다 이전이어야 합니다.");
            }
        }
    }

    public User findUserById(String id) {
        for (User user : store.users()) {
            if (user.getIdKey().equals(id.toLowerCase())) {
                return user;
            }
        }
        return null;
    }

    public Hotel findHotelByPostalCode(String postalCode) {
        for (Hotel hotel : store.hotels()) {
            if (hotel.getPostalCode().equals(postalCode)) {
                return hotel;
            }
        }
        return null;
    }
}
