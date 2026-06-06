package persistence;

import common.FatalDataException;
import domain.Hotel;
import domain.Penalty;
import domain.Reservation;
import domain.ReservationStatus;
import domain.Role;
import domain.Room;
import domain.Suspension;
import domain.SystemState;
import domain.User;
import validation.SpecParsers;
import validation.SpecValidators;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class FlatFileStore {
    public static final String USER_FILE = "user.txt";
    public static final String HOTEL_FILE = "hotel.txt";
    public static final String ROOM_FILE = "room.txt";
    public static final String RESERVATION_FILE = "reservation.txt";
    public static final String TIME_FILE = "time.txt";
    public static final String SYSTEM_FILE = "system.txt";
    public static final String PENALTY_FILE = "penalty.txt";
    public static final String SUSPENSION_FILE = "suspension.txt";

    private final Path dataDir;
    private final DataSnapshot snapshot = new DataSnapshot();

    public FlatFileStore(Path dataDir) {
        this.dataDir = dataDir;
    }

    public Path getDataDir() {
        return dataDir;
    }

    public List<User> users() {
        return snapshot.getUsers();
    }

    public List<Hotel> hotels() {
        return snapshot.getHotels();
    }

    public List<Room> rooms() {
        return snapshot.getRooms();
    }

    public List<Reservation> reservations() {
        return snapshot.getReservations();
    }

    public List<Penalty> penalties() {
        return snapshot.getPenalties();
    }

    public List<Suspension> suspensions() {
        return snapshot.getSuspensions();
    }

    public SystemState systemState() {
        return snapshot.getSystemState();
    }

    public LocalDateTime baselineDateTime() {
        return snapshot.getBaselineDateTime();
    }

    public LocalDate baselineDate() {
        return snapshot.getBaselineDate();
    }

    public void setBaselineDateTime(LocalDateTime baselineDateTime) {
        snapshot.setBaselineDateTime(baselineDateTime);
    }

    public Path userFile() {
        return dataDir.resolve(USER_FILE);
    }

    public Path hotelFile() {
        return dataDir.resolve(HOTEL_FILE);
    }

    public Path roomFile() {
        return dataDir.resolve(ROOM_FILE);
    }

    public Path reservationFile() {
        return dataDir.resolve(RESERVATION_FILE);
    }

    public Path timeFile() {
        return dataDir.resolve(TIME_FILE);
    }

    public Path systemFile() {
        return dataDir.resolve(SYSTEM_FILE);
    }

    public Path penaltyFile() {
        return dataDir.resolve(PENALTY_FILE);
    }

    public Path suspensionFile() {
        return dataDir.resolve(SUSPENSION_FILE);
    }

    public void ensureDirectoryAndFilesExist() {
        try {
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
            createIfMissing(userFile());
            createIfMissing(hotelFile());
            createIfMissing(roomFile());
            createIfMissing(reservationFile());
            createIfMissing(timeFile());
            createIfMissing(systemFile());
            createIfMissing(penaltyFile());
            createIfMissing(suspensionFile());
        } catch (IOException e) {
            throw new FatalDataException("data 폴더 또는 데이터 파일 생성 실패", e);
        }
    }

    public void ensureReadableWritable() {
        ensureFileAccessible(userFile());
        ensureFileAccessible(hotelFile());
        ensureFileAccessible(roomFile());
        ensureFileAccessible(reservationFile());
        ensureFileAccessible(timeFile());
        ensureFileAccessible(systemFile());
        ensureFileAccessible(penaltyFile());
        ensureFileAccessible(suspensionFile());
    }

    public void loadUsers() {
        users().clear();
        List<String> lines = readAllLines(userFile());
        int lineNo = 0;
        for (String line : lines) {
            lineNo++;
            String cleaned = trimRecordLine(line);
            if (cleaned.isEmpty()) {
                continue;
            }
            String[] tokens = cleaned.split("\t", -1);
            if (tokens.length != 4) {
                throw new FatalDataException("user.txt " + lineNo + "행 형식 오류");
            }
            String id = tokens[0];
            String password = tokens[1];
            String name = tokens[2];
            String roleText = tokens[3];
            SpecValidators.requireValidId(id, "user.txt " + lineNo + "행 ID");
            SpecValidators.requireValidPassword(password, "user.txt " + lineNo + "행 비밀번호");
            SpecValidators.requireValidUserName(name, "user.txt " + lineNo + "행 이름");
            Role role = SpecParsers.parseRole(roleText, "user.txt " + lineNo + "행 권한");
            users().add(new User(id, password, name, role));
        }
    }

    public void loadHotels() {
        hotels().clear();
        List<String> lines = readAllLines(hotelFile());
        int lineNo = 0;
        for (String line : lines) {
            lineNo++;
            String cleaned = trimRecordLine(line);
            if (cleaned.isEmpty()) {
                continue;
            }
            String[] tokens = cleaned.split("\t", -1);
            if (tokens.length != 3) {
                throw new FatalDataException("hotel.txt " + lineNo + "행 형식 오류");
            }
            String name = tokens[0];
            String postalCode = tokens[1];
            String hostId = tokens[2];
            SpecValidators.requireValidHotelName(name, "hotel.txt " + lineNo + "행 업소 이름");
            SpecValidators.requireValidPostalCode(postalCode, "hotel.txt " + lineNo + "행 우편번호");
            SpecValidators.requireValidId(hostId, "hotel.txt " + lineNo + "행 ID");
            hotels().add(new Hotel(name, postalCode, hostId));
        }
    }

    public void loadRooms() {
        rooms().clear();
        List<String> lines = readAllLines(roomFile());
        int lineNo = 0;
        for (String line : lines) {
            lineNo++;
            String cleaned = trimRecordLine(line);
            if (cleaned.isEmpty()) {
                continue;
            }
            String[] tokens = cleaned.split("\t", -1);
            if (tokens.length != 5) {
                throw new FatalDataException("room.txt " + lineNo + "행 형식 오류");
            }
            String postalCode = tokens[0];
            int roomNumber = SpecParsers.parseRoomNumber(tokens[1], "room.txt " + lineNo + "행 방 번호");
            LocalTime checkIn = SpecParsers.parseTime(tokens[2], "room.txt " + lineNo + "행 체크인 시간");
            LocalTime checkOut = SpecParsers.parseTime(tokens[3], "room.txt " + lineNo + "행 체크아웃 시간");
            int capacity = SpecParsers.parseCapacity(tokens[4], "room.txt " + lineNo + "행 수용 인원");
            SpecValidators.requireValidPostalCode(postalCode, "room.txt " + lineNo + "행 우편번호");
            rooms().add(new Room(postalCode, roomNumber, checkIn, checkOut, capacity));
        }
    }

    public void loadReservations() {
        reservations().clear();
        List<String> lines = readAllLines(reservationFile());
        int lineNo = 0;
        for (String line : lines) {
            lineNo++;
            String cleaned = trimRecordLine(line);
            if (cleaned.isEmpty()) {
                continue;
            }
            String[] tokens = cleaned.split("\t", -1);
            if (tokens.length != 14) {
                throw new FatalDataException("reservation.txt " + lineNo + "행 형식 오류");
            }
            String guestId = tokens[0];
            String hotelName = tokens[1];
            String postalCode = tokens[2];
            int roomNumber = SpecParsers.parseRoomNumber(tokens[3], "reservation.txt " + lineNo + "행 방 번호");
            int guestCount = SpecParsers.parseCapacity(tokens[4], "reservation.txt " + lineNo + "행 투숙 인원");
            LocalDate checkInDate = SpecParsers.parseDate(tokens[5], "reservation.txt " + lineNo + "행 체크인 날짜");
            LocalTime checkInTime = SpecParsers.parseTime(tokens[6], "reservation.txt " + lineNo + "행 체크인 시간");
            LocalDate checkOutDate = SpecParsers.parseDate(tokens[7], "reservation.txt " + lineNo + "행 체크아웃 날짜");
            LocalTime checkOutTime = SpecParsers.parseTime(tokens[8], "reservation.txt " + lineNo + "행 체크아웃 시간");
            ReservationStatus status = SpecParsers.parseReservationStatus(tokens[9], "reservation.txt " + lineNo + "행 예약 상태");
            LocalDateTime createdAt = SpecParsers.parseDateTime(tokens[10], "reservation.txt " + lineNo + "행 생성 시각");
            LocalDate cancelRequestDate = tokens[11].isEmpty() ? null
                    : SpecParsers.parseDate(tokens[11], "reservation.txt " + lineNo + "행 취소 요청일");
            LocalTime cancelRequestTime = tokens[12].isEmpty() ? null
                    : SpecParsers.parseTime(tokens[12], "reservation.txt " + lineNo + "행 취소 요청시각");
            Double rating = tokens[13].isEmpty() ? null
                    : SpecParsers.parseRating(tokens[13], "reservation.txt " + lineNo + "행 평점");
            SpecValidators.requireValidId(guestId, "reservation.txt " + lineNo + "행 ID");
            SpecValidators.requireValidHotelName(hotelName, "reservation.txt " + lineNo + "행 업소 이름");
            SpecValidators.requireValidPostalCode(postalCode, "reservation.txt " + lineNo + "행 우편번호");
            reservations().add(new Reservation(
                    guestId, hotelName, postalCode, roomNumber, guestCount,
                    checkInDate, checkInTime, checkOutDate, checkOutTime,
                    status, createdAt, cancelRequestDate, cancelRequestTime, rating
            ));
        }
    }

    public void loadPenalties() {
        penalties().clear();
        List<String> lines = readAllLines(penaltyFile());
        int lineNo = 0;
        for (String line : lines) {
            lineNo++;
            String cleaned = trimRecordLine(line);
            if (cleaned.isEmpty()) {
                continue;
            }
            String[] tokens = cleaned.split("\t", -1);
            if (tokens.length != 3) {
                throw new FatalDataException("penalty.txt " + lineNo + "행 형식 오류");
            }
            SpecValidators.requireValidId(tokens[0], "penalty.txt " + lineNo + "행 ID");
            if (!Penalty.GLOBAL_POSTAL_CODE.equals(tokens[1])) {
                SpecValidators.requireValidPostalCode(tokens[1], "penalty.txt " + lineNo + "행 우편번호");
            }
            penalties().add(new Penalty(tokens[0], tokens[1],
                    SpecParsers.parseDate(tokens[2], "penalty.txt " + lineNo + "행 종료일")));
        }
    }

    public void loadSuspensions() {
        suspensions().clear();
        List<String> lines = readAllLines(suspensionFile());
        int lineNo = 0;
        for (String line : lines) {
            lineNo++;
            String cleaned = trimRecordLine(line);
            if (cleaned.isEmpty()) {
                continue;
            }
            String[] tokens = cleaned.split("\t", -1);
            if (tokens.length != 4) {
                throw new FatalDataException("suspension.txt " + lineNo + "행 형식 오류");
            }
            SpecValidators.requireValidPostalCode(tokens[0], "suspension.txt " + lineNo + "행 우편번호");
            suspensions().add(new Suspension(tokens[0],
                    SpecParsers.parseDateTime(tokens[1], "suspension.txt " + lineNo + "행 시작 시각"),
                    SpecParsers.parseDateTime(tokens[2], "suspension.txt " + lineNo + "행 종료 시각"),
                    tokens[3]));
        }
    }

    public void loadSystem() {
        systemState().setLastCheckedMonth(null);
        String raw = readSingleRaw(systemFile());
        if (raw.isEmpty()) {
            return;
        }
        systemState().setLastCheckedMonth(SpecParsers.parseSystemYearMonth(raw, "system.txt"));
    }

    public void loadBaselineDateTime() {
        String raw = readSingleRaw(timeFile());
        if (raw.isEmpty()) {
            setBaselineDateTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")).withSecond(0).withNano(0));
            return;
        }
        try {
            setBaselineDateTime(SpecParsers.parseFlexibleDateTime(raw, "time.txt"));
        } catch (FatalDataException e) {
            setBaselineDateTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")).withSecond(0).withNano(0));
        }
    }

    public void loadAll() {
        loadBaselineDateTime();
        loadUsers();
        loadHotels();
        loadRooms();
        loadReservations();
        loadPenalties();
        loadSuspensions();
        loadSystem();
    }

    public void saveUsers() {
        List<String> lines = new ArrayList<>();
        for (User user : users()) {
            lines.add(user.getId() + "\t" + user.getPassword() + "\t" + user.getName() + "\t" + user.getRole().code());
        }
        writeLines(userFile(), lines);
    }

    public void saveHotels() {
        List<String> lines = new ArrayList<>();
        for (Hotel hotel : hotels()) {
            lines.add(hotel.getName() + "\t" + hotel.getPostalCode() + "\t" + hotel.getHostId());
        }
        writeLines(hotelFile(), lines);
    }

    public void saveRooms() {
        List<String> lines = new ArrayList<>();
        for (Room room : rooms()) {
            lines.add(room.getPostalCode() + "\t" + room.getRoomNumber() + "\t"
                    + SpecParsers.formatTime(room.getCheckInTime()) + "\t"
                    + SpecParsers.formatTime(room.getCheckOutTime()) + "\t"
                    + room.getCapacity());
        }
        writeLines(roomFile(), lines);
    }

    public void saveReservations() {
        List<String> lines = new ArrayList<>();
        for (Reservation reservation : reservations()) {
            lines.add(reservation.getGuestId() + "\t"
                    + reservation.getHotelName() + "\t"
                    + reservation.getPostalCode() + "\t"
                    + reservation.getRoomNumber() + "\t"
                    + reservation.getGuestCount() + "\t"
                    + SpecParsers.formatDate(reservation.getCheckInDate()) + "\t"
                    + SpecParsers.formatTime(reservation.getCheckInTime()) + "\t"
                    + SpecParsers.formatDate(reservation.getCheckOutDate()) + "\t"
                    + SpecParsers.formatTime(reservation.getCheckOutTime()) + "\t"
                    + reservation.getStatus().code() + "\t"
                    + SpecParsers.formatDateTime(reservation.getCreatedAt()) + "\t"
                    + (reservation.getCancelRequestDate() == null ? "" : SpecParsers.formatDate(reservation.getCancelRequestDate())) + "\t"
                    + (reservation.getCancelRequestTime() == null ? "" : SpecParsers.formatTime(reservation.getCancelRequestTime())) + "\t"
                    + SpecParsers.formatRating(reservation.getRating()));
        }
        writeLines(reservationFile(), lines);
    }

    public void savePenalties() {
        List<String> lines = new ArrayList<>();
        for (Penalty penalty : penalties()) {
            lines.add(penalty.getGuestId() + "\t" + penalty.getPostalCode() + "\t"
                    + SpecParsers.formatDate(penalty.getEndDate()));
        }
        writeLines(penaltyFile(), lines);
    }

    public void saveSuspensions() {
        List<String> lines = new ArrayList<>();
        for (Suspension suspension : suspensions()) {
            lines.add(suspension.getPostalCode() + "\t"
                    + SpecParsers.formatDateTime(suspension.getStartAt()) + "\t"
                    + SpecParsers.formatDateTime(suspension.getEndAt()) + "\t"
                    + suspension.getReason());
        }
        writeLines(suspensionFile(), lines);
    }

    public void saveSystem() {
        if (systemState().getLastCheckedMonth() == null) {
            writeLines(systemFile(), List.of());
            return;
        }
        writeLines(systemFile(), List.of(SpecParsers.formatSystemYearMonth(systemState().getLastCheckedMonth())));
    }

    public void saveTime() {
        writeLines(timeFile(), List.of(SpecParsers.formatDateTime(baselineDateTime())));
    }

    public void saveAll() {
        saveUsers();
        saveHotels();
        saveRooms();
        saveReservations();
        saveTime();
        saveSystem();
        savePenalties();
        saveSuspensions();
    }

    private void createIfMissing(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
    }

    private void ensureFileAccessible(Path path) {
        if (!Files.isReadable(path) || !Files.isWritable(path)) {
            throw new FatalDataException(path.getFileName() + " 읽기/쓰기 권한이 없습니다.");
        }
    }

    private List<String> readAllLines(Path path) {
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FatalDataException(path.getFileName() + " 읽기 실패", e);
        }
    }

    private String readSingleRaw(Path path) {
        List<String> lines = readAllLines(path);
        if (lines.isEmpty()) {
            return "";
        }
        List<String> merged = new ArrayList<>();
        for (String line : lines) {
            merged.add(SpecParsers.trimHorizontalWhitespace(line));
        }
        return String.join("", merged).trim();
    }

    private void writeLines(Path path, List<String> lines) {
        try {
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FatalDataException(path.getFileName() + " 저장 실패", e);
        }
    }

    private String trimRecordLine(String line) {
        int start = 0;
        int end = line.length();
        while (start < end && isTrimmableRecordEdge(line.charAt(start))) {
            start++;
        }
        while (end > start && isTrimmableRecordEdge(line.charAt(end - 1))) {
            end--;
        }
        return line.substring(start, end);
    }

    private boolean isTrimmableRecordEdge(char ch) {
        return Character.isWhitespace(ch) && ch != '\t';
    }
}
