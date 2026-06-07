package ui;

import common.FatalDataException;
import domain.Hotel;
import domain.Reservation;
import domain.ReservationStatus;
import domain.Role;
import domain.Room;
import domain.Session;
import domain.User;
import persistence.FlatFileStore;
import service.AuthService;
import service.GuestService;
import service.HostService;
import service.IntegrityService;
import validation.SpecParsers;
import validation.SpecValidators;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class AppController {
    private final ConsoleIO io;
    private final FlatFileStore store;
    private final IntegrityService integrityService;
    private final AuthService authService;
    private final GuestService guestService;
    private final HostService hostService;
    private final Session session = new Session();

    public AppController(ConsoleIO io,
                         FlatFileStore store,
                         IntegrityService integrityService,
                         AuthService authService,
                         GuestService guestService,
                         HostService hostService) {
        this.io = io;
        this.store = store;
        this.integrityService = integrityService;
        this.authService = authService;
        this.guestService = guestService;
        this.hostService = hostService;
    }

    public void run() {
        integrityService.initialize();
        while (showInitialMenu()) {
            // main loop
        }
    }

    private boolean showInitialMenu() {
        io.println("");
        io.println("현재 기준 시간 : " + SpecParsers.formatDateTime(store.baselineDateTime()));
        io.println("1. 로그인");
        io.println("2. 회원가입");
        String input = io.prompt(">> ");
        if ("1".equals(input)) {
            loginFlow();
            return true;
        }
        if ("2".equals(input)) {
            signUpFlow();
            return true;
        }
        if (";;".equals(input)) {
            io.println("프로그램을 종료 합니다.");
            return false;
        }
        io.println("메뉴 번호는 1 또는 2를 입력해야 합니다.");
        return true;
    }

    private void loginFlow() {
        User user = null;
        while (user == null) {
            String id = io.prompt("아이디 : ");
            if (!SpecValidators.isValidId(id)) {
                io.println("잘못된 아이디입니다. 다시 입력해주세요.");
                continue;
            }
            user = authService.findUserById(id);
            if (user == null) {
                io.println("잘못된 아이디입니다. 다시 입력해주세요.");
            }
        }

        while (true) {
            String password = io.prompt("비밀번호 : ");
            if (!SpecValidators.isValidPassword(password)) {
                io.println("잘못된 비밀번호입니다. 다시 입력해주세요.");
                continue;
            }
            User matched = authService.authenticate(user.getId(), password);
            if (matched == null) {
                io.println("잘못된 비밀번호입니다. 다시 입력해주세요.");
                continue;
            }
            session.login(matched);
            if (matched.getRole() == Role.GUEST) {
                guestMenuLoop();
            } else if (matched.getRole() == Role.HOST) {
                hostMenuLoop();
            } else {
                adminMenuLoop();
            }
            return;
        }
    }

    private void signUpFlow() {
        String name;
        while (true) {
            name = io.prompt("이름 : ");
            if (SpecValidators.isValidUserName(name)) {
                break;
            }
            io.println("잘못된 이름입니다. 다시 입력해주세요.");
        }

        String id;
        while (true) {
            id = io.prompt("아이디 : ");
            if (!SpecValidators.isValidId(id)) {
                io.println("잘못된 아이디입니다. 다시 입력해주세요.");
                continue;
            }
            if (authService.existsUserId(id)) {
                io.println("이미 존재하는 아이디입니다. 다시 입력해주세요.");
                continue;
            }
            break;
        }

        String password;
        while (true) {
            password = io.prompt("비밀번호 : ");
            if (SpecValidators.isValidPassword(password)) {
                break;
            }
            io.println("비밀번호는 8 글자 이상 20 글자 이하이며 로마자 대문자, 소문자, 숫자, "
                    + "특수문자가 각각 최소 1개 이상 반드시 포함되어야 합니다.");
        }

        Role role;
        while (true) {
            String roleText = io.prompt("자신의 역할을 입력하세요 (Host / Guest / Admin) : ");
            try {
                role = SpecParsers.parseRole(roleText, "권한");
                break;
            } catch (FatalDataException e) {
                io.println("잘못된 역할 이름 입니다.");
            }
        }

        authService.register(name, id, password, role);
    }

    private void guestMenuLoop() {
        while (session.isLoggedIn()) {
            io.println("");
            io.println("고객 메뉴");
            io.println("1. 예약");
            boolean hasReservations = !guestService.reservationsOf(session.getCurrentUser()).isEmpty();
            if (hasReservations) {
                io.println("2. 기록조회");
                io.println("3. 로그아웃");
            } else {
                io.println("2. 로그아웃");
            }
            String input = io.prompt(">> ");
            if ("1".equals(input)) {
                reserveFlow();
            } else if (hasReservations && "2".equals(input)) {
                guestReservationHistoryLoop();
            } else if ((!hasReservations && "2".equals(input))
                    || (hasReservations && "3".equals(input))) {
                logout();
            } else {
                io.println("잘못된 입력입니다. 다시 입력해주세요.");
            }
        }
    }

    private void reserveFlow() {
        User guest = session.getCurrentUser();
        if (guestService.hasActiveGlobalPenalty(guest)) {
            io.println("페널티를 보유하고 있어 예약이 불가합니다.");
            guestBackToMenu();
            return;
        }

        Hotel selectedHotel = chooseHotelBySearch();
        if (selectedHotel == null) {
            guestBackToMenu();
            return;
        }
        if (guestService.hasActivePenaltyOn(guest, selectedHotel.getPostalCode())) {
            io.println("해당 업소에 대한 페널티를 보유하고 있어 예약이 불가합니다.");
            guestBackToMenu();
            return;
        }

        Room selectedRoom = chooseRoom(selectedHotel);
        if (selectedRoom == null) {
            guestBackToMenu();
            return;
        }

        Integer guestCount = chooseGuestCount(selectedRoom);
        if (guestCount == null) {
            guestBackToMenu();
            return;
        }

        StayInput stayInput = promptStayPeriod(selectedHotel, selectedRoom, null);
        if (stayInput == null) {
            guestBackToMenu();
            return;
        }

        try {
            guestService.createReservation(guest, selectedHotel, selectedRoom, guestCount,
                    stayInput.checkInDate, stayInput.checkInTime, stayInput.checkOutTime);
            io.println("예약이 완료되었습니다.");
            io.println(Formatters.bookingSummary(selectedHotel, selectedRoom, guestCount,
                    stayInput.checkInDate, stayInput.checkInTime, stayInput.checkOutDate, stayInput.checkOutTime)
                    + " 위 정보로 예약 완료되었습니다!");
        } catch (IllegalArgumentException e) {
            io.println(e.getMessage());
        }
        guestBackToMenu();
    }

    private Hotel chooseHotelBySearch() {
        List<Hotel> hotels = guestService.listOpenHotels(store.baselineDateTime());
        if (hotels.isEmpty()) {
            io.println("예약 가능한 업소가 없습니다.");
            return null;
        }
        io.println("예약 가능한 업소");
        for (int i = 0; i < hotels.size(); i++) {
            io.println((i + 1) + ". " + Formatters.hotelLine(hotels.get(i)));
        }

        while (true) {
            String input = io.prompt("업소 이름을 입력하세요\n>> ");
            if (";".equals(input)) {
                return null;
            }
            if (!SpecValidators.isValidHotelName(input)) {
                io.println("존재하지 않는 업소 이름입니다. 다시 입력해주세요.");
                continue;
            }
            List<Hotel> results = guestService.searchOpenHotels(input, store.baselineDateTime());
            if (results.isEmpty()) {
                io.println("존재하지 않는 업소 이름입니다. 다시 입력해주세요.");
                continue;
            }
            io.println("검색된 업소");
            for (int i = 0; i < results.size(); i++) {
                io.println((i + 1) + ". " + Formatters.hotelLine(results.get(i)));
            }
            Integer index = readIndex("예약을 원하는 번호를 입력하세요\n>> ", results.size(), true);
            if (index == null) {
                return null;
            }
            return results.get(index - 1);
        }
    }

    private Room chooseRoom(Hotel hotel) {
        List<Room> rooms = guestService.roomsByHotel(hotel.getPostalCode());
        if (rooms.isEmpty()) {
            io.println("등록된 객실이 없습니다.");
            return null;
        }
        io.println("예약 가능한 방 목록");
        for (Room room : rooms) {
            io.println(Formatters.roomLine(room));
        }
        while (true) {
            String input = io.prompt("원하는 방 번호를 입력하세요\n>> ");
            if (";".equals(input)) {
                return null;
            }
            try {
                int roomNumber = SpecParsers.parseRoomNumber(input, "방 번호");
                Room room = guestService.findRoom(hotel.getPostalCode(), roomNumber);
                if (room == null) {
                    io.println("잘못된 입력입니다.");
                    continue;
                }
                return room;
            } catch (FatalDataException e) {
                io.println(isSyntaxError(e)
                        ? "방 번호 형식이 올바르지 않습니다."
                        : "방 번호는 1 이상 9,999 이하이어야 합니다.");
            }
        }
    }

    private Integer chooseGuestCount(Room room) {
        while (true) {
            String input = io.prompt("숙박인원을 입력해주세요.\n>> ");
            if (";".equals(input)) {
                return null;
            }
            try {
                int value = SpecParsers.parseCapacity(input, "투숙 인원");
                if (value > room.getCapacity()) {
                    io.println("잘못된 입력입니다. 다시 입력해주세요.");
                    continue;
                }
                return value;
            } catch (FatalDataException e) {
                io.println(isSyntaxError(e)
                        ? "숙박인원 형식이 올바르지 않습니다."
                        : "숙박인원은 1 이상 999 이하이어야 합니다.");
            }
        }
    }

    private StayInput promptStayPeriod(Hotel hotel, Room room, Reservation exclude) {
        while (true) {
            io.println("예약 날짜 메뉴");
            io.println("1. 날짜 확인");
            io.println("2. 날짜 확정");
            String menuInput = io.prompt(">> ");
            if (";".equals(menuInput)) {
                return null;
            }
            if ("1".equals(menuInput)) {
                YearMonth yearMonth = promptYearMonth(
                        "예약 가능 여부를 확인하고 싶은 날짜를 입력해주세요. (연/월)\n>> ");
                if (yearMonth != null) {
                    CalendarPrinter.print(io, yearMonth.getYear(), yearMonth.getMonthValue(),
                            date -> guestService.isDateBlocked(
                                    hotel.getPostalCode(), room.getRoomNumber(), date,
                                    guestService.baselineDate(), exclude));
                }
                continue;
            }
            if (!"2".equals(menuInput)) {
                io.println("잘못된 입력입니다. 다시 입력해주세요.");
                continue;
            }

            LocalDate checkInDate = promptReservableDate(hotel, room, exclude);
            if (checkInDate == null) {
                continue;
            }
            io.println("기본 체크인 시간: " + SpecParsers.formatTime(room.getCheckInTime())
                    + ", 기본 체크아웃 시간: " + SpecParsers.formatTime(room.getCheckOutTime()));
            LocalTime checkInTime = promptReservationCheckInTime(room);
            if (checkInTime == null) {
                return null;
            }
            LocalTime checkOutTime = promptReservationCheckOutTime(room);
            if (checkOutTime == null) {
                return null;
            }
            return new StayInput(checkInDate, checkInTime, checkInDate.plusDays(1), checkOutTime);
        }
    }

    private LocalDate promptReservableDate(Hotel hotel, Room room, Reservation exclude) {
        while (true) {
            LocalDate date = promptDate(
                    "예약 가능 여부를 확인하고 싶은 날짜를 입력해주세요. (연/월/일) "
                            + "(메인 메뉴로 돌아가려면 ; 입력)\n>> ");
            if (date == null) {
                return null;
            }
            if (!date.isAfter(guestService.baselineDate())) {
                io.println("잘못된 입력입니다. 다시 입력해주세요.");
                continue;
            }
            if (guestService.isDateBlocked(hotel.getPostalCode(), room.getRoomNumber(), date,
                    guestService.baselineDate(), exclude)) {
                io.println("이미 예약되어 있는 날짜입니다. 다른 날짜를 입력해주세요.");
                continue;
            }
            return date;
        }
    }

    private YearMonth promptYearMonth(String prompt) {
        while (true) {
            String input = io.prompt(prompt);
            if (";".equals(input)) {
                return null;
            }
            try {
                SpecParsers.YearMonthValue value = SpecParsers.parseYearMonth(input, "연/월");
                return YearMonth.of(value.year(), value.month());
            } catch (FatalDataException e) {
                io.println(isSyntaxError(e)
                        ? "연/월 형식이 올바르지 않습니다."
                        : "유효하지 않은 연/월입니다.");
            }
        }
    }

    private void printGuestReservations() {
        List<Reservation> reservations = guestService.reservationsOf(session.getCurrentUser());
        io.println("예약 목록");
        if (reservations.isEmpty()) {
            io.println("(예약 없음)");
            return;
        }
        for (int i = 0; i < reservations.size(); i++) {
            io.println((i + 1) + ". " + Formatters.reservationLong(reservations.get(i)));
        }
    }

    private void guestReservationHistoryLoop() {
        while (session.isLoggedIn()) {
            io.println("");
            printGuestReservations();
            io.println("1. 입실");
            io.println("2. 퇴실");
            io.println("3. 예약 변경");
            io.println("4. 평점 작성");
            io.println("5. 예약 취소");
            String input = io.prompt(">> ");
            if ("1".equals(input)) {
                checkInFlow();
            } else if ("2".equals(input)) {
                checkOutFlow();
            } else if ("3".equals(input)) {
                boolean shouldReturnToMain = changeReservationFlow();
                if (shouldReturnToMain) {
                    return;
                }
            } else if ("4".equals(input)) {
                reviewFlow();
            } else if ("5".equals(input)) {
                boolean shouldReturnToMain = cancelReservationFlow();
                if (shouldReturnToMain) {
                    return;
                }
            } else if (";".equals(input)) {
                return;
            } else {
                io.println("잘못된 입력입니다. 다시 입력해주세요.");
            }
        }
    }

    private boolean changeReservationFlow() {
        User guest = session.getCurrentUser();
        Reservation reservation = chooseGuestReservation(
                "변경할 예약 번호를 입력하세요.\n>> ", guestService::canChange);
        if (reservation == null) {
            return false;
        }
        if (guestService.hasActivePenaltyOn(guest, reservation.getPostalCode())) {
            io.println("해당 업소에 대한 페널티를 보유하고 있어 예약변경이 불가합니다.");
            return false;
        }
        Hotel hotel = guestService.findHotelByPostalCode(reservation.getPostalCode());
        if (hotel == null) {
            io.println("변경할 예약의 업소를 찾을 수 없습니다.");
            return false;
        }
        io.println("변경할 업소: " + Formatters.hotelLine(hotel));
        Room room = chooseRoom(hotel);
        if (room == null) {
            return false;
        }
        Integer guestCount = chooseGuestCount(room);
        if (guestCount == null) {
            return false;
        }
        StayInput stayInput = promptStayPeriod(hotel, room, reservation);
        if (stayInput == null) {
            return false;
        }
        try {
            guestService.replaceReservation(guest, reservation, hotel, room, guestCount,
                    stayInput.checkInDate, stayInput.checkInTime, stayInput.checkOutTime);
            io.println("예약이 완료되었습니다.");
            io.println(Formatters.bookingSummary(hotel, room, guestCount,
                    stayInput.checkInDate, stayInput.checkInTime, stayInput.checkOutDate, stayInput.checkOutTime)
                    + " 위 정보로 예약 완료되었습니다!");
            io.println("기존의 예약이 취소되었습니다.");
            return true;
        } catch (IllegalArgumentException e) {
            io.println(e.getMessage());
            return false;
        }
    }

    private void checkInFlow() {
        Reservation reservation = chooseGuestReservation(
                "예약 번호를 입력해주세요 : ", guestService::canCheckIn);
        if (reservation == null) {
            return;
        }
        if (guestService.checkIn(session.getCurrentUser(), reservation)) {
            io.println("처리가 완료되었습니다.");
        } else {
            io.println("잘못된 입력입니다.");
        }
    }

    private void checkOutFlow() {
        Reservation reservation = chooseGuestReservation(
                "예약 번호를 입력해주세요 : ", guestService::canCheckOut);
        if (reservation == null) {
            return;
        }
        if (guestService.checkOut(session.getCurrentUser(), reservation)) {
            io.println("처리가 완료되었습니다.");
        } else {
            io.println("잘못된 입력입니다.");
        }
    }

    private void reviewFlow() {
        Reservation reservation = chooseGuestReservation(
                "예약 번호 : ", guestService::canWriteReview);
        if (reservation == null) {
            return;
        }
        while (true) {
            String input = io.prompt("평점 (0~10): ");
            if (";".equals(input)) {
                return;
            }
            try {
                double rating = SpecParsers.parseRating(input, "평점");
                if (guestService.writeReview(session.getCurrentUser(), reservation, rating)) {
                    io.println("처리가 완료되었습니다.");
                } else {
                    io.println("잘못된 입력입니다.");
                }
                return;
            } catch (FatalDataException e) {
                io.println(isSyntaxError(e)
                        ? "평점 형식이 올바르지 않습니다. 소수점 이하 첫째 자리까지만 입력해주세요."
                        : "평점은 0.0 이상 10.0 이하이어야 합니다.");
            }
        }
    }

    private boolean cancelReservationFlow() {
        User guest = session.getCurrentUser();
        List<Reservation> reservations = guestService.reservationsOf(guest);
        if (reservations.isEmpty()) {
            io.println("취소할 예약이 없습니다.");
            return false;
        }
        Reservation reservation = chooseGuestReservation(
                "취소할 예약 순서 번호를 입력하세요.\n>> ", reservations, guestService::canCancel);
        if (reservation == null) {
            return false;
        }
        if (guestService.hasActivePenaltyOn(guest, reservation.getPostalCode())) {
            io.println("해당 업소에 대한 페널티를 보유하고 있어 예약취소가 불가합니다.");
            return false;
        }
        guestService.requestCancel(guest, reservation);
        return true;
    }

    private Reservation chooseGuestReservation(String prompt, Predicate<Reservation> isValid) {
        List<Reservation> reservations = guestService.reservationsOf(session.getCurrentUser());
        return chooseGuestReservation(prompt, reservations, isValid);
    }

    private Reservation chooseGuestReservation(String prompt, List<Reservation> reservations,
                                                Predicate<Reservation> isValid) {
        if (reservations.isEmpty()) {
            io.println("대상 예약이 없습니다.");
            return null;
        }
        while (true) {
            Integer index = readIndex(prompt, reservations.size(), true);
            if (index == null) {
                return null;
            }
            Reservation reservation = reservations.get(index - 1);
            if (isValid.test(reservation)) {
                return reservation;
            }
            io.println("잘못된 입력입니다. 다시 입력해주세요.");
        }
    }

    private void hostMenuLoop() {
        while (session.isLoggedIn()) {
            io.println("");
            io.println("업주 메뉴");
            io.println("1. 업소 등록");
            boolean hasHotels = !hostService.hotelsByHost(session.getCurrentUser()).isEmpty();
            if (hasHotels) {
                io.println("2. 업소 정보 수정");
                io.println("3. 예약자 관리");
                io.println("4. 로그아웃");
            } else {
                io.println("2. 로그아웃");
            }
            String input = io.prompt(">> ");
            if ("1".equals(input)) {
                registerHotelFlow();
            } else if (hasHotels && "2".equals(input)) {
                editHotelFlow();
            } else if (hasHotels && "3".equals(input)) {
                manageReservationsFlow();
            } else if ((!hasHotels && "2".equals(input))
                    || (hasHotels && "4".equals(input))) {
                logout();
            } else {
                io.println("잘못된 입력입니다. 다시 입력해주세요.");
            }
        }
    }

    private void registerHotelFlow() {
        String name;
        while (true) {
            name = io.prompt("등록할 업소 이름을 입력하세요 : ");
            if (SpecValidators.isValidHotelName(name)) {
                break;
            }
            io.println("유효하지 않은 업소 이름입니다.");
        }

        String postalCode;
        while (true) {
            postalCode = io.prompt("등록할 업소의 우편번호를 입력하세요 : ");
            if (!SpecValidators.isValidPostalCode(postalCode)) {
                io.println("우편번호 형식이 올바르지 않습니다.");
                continue;
            }
            if (hostService.findHotelByPostalCode(postalCode) != null) {
                io.println("이미 등록된 우편번호입니다.");
                continue;
            }
            break;
        }

        List<Room> rooms = new ArrayList<>();
        while (true) {
            int roomNumber = promptRoomNumberForRegistration(rooms);
            int capacity = promptCapacity();
            LocalTime checkIn = promptRequiredTime("체크인 시간을 입력하세요 : ");
            LocalTime checkOut = promptRequiredTime("체크아웃 시간을 입력하세요 : ");
            rooms.add(new Room(postalCode, roomNumber, checkIn, checkOut, capacity));

            String more = promptYesNo("추가 방을 등록하시겠습니까? (y/n) : ");
            if ("n".equals(more)) {
                hostService.registerHotel(session.getCurrentUser(), name, postalCode, rooms);
                io.println("업소 등록이 완료되었습니다.");
                return;
            }
        }
    }

    private int promptRoomNumberForRegistration(List<Room> existingRooms) {
        while (true) {
            String text = io.prompt("방 번호를 입력하세요 : ");
            try {
                int roomNumber = SpecParsers.parseRoomNumber(text, "방 번호");
                boolean duplicate = false;
                for (Room room : existingRooms) {
                    if (room.getRoomNumber() == roomNumber) {
                        duplicate = true;
                        break;
                    }
                }
                if (duplicate) {
                    io.println("이미 입력한 방 번호입니다.");
                    continue;
                }
                return roomNumber;
            } catch (FatalDataException e) {
                io.println(isSyntaxError(e)
                        ? "방 번호 형식이 올바르지 않습니다."
                        : "방 번호는 1 이상 9,999 이하이어야 합니다.");
            }
        }
    }

    private int promptCapacity() {
        while (true) {
            String text = io.prompt("인원 수를 입력하세요 : ");
            try {
                return SpecParsers.parseCapacity(text, "인원 수");
            } catch (FatalDataException e) {
                io.println(isSyntaxError(e)
                        ? "인원 수 형식이 올바르지 않습니다."
                        : "인원 수는 1 이상 999 이하이어야 합니다.");
            }
        }
    }

    private LocalTime promptRequiredTime(String prompt) {
        while (true) {
            try {
                return SpecParsers.parseTime(io.prompt(prompt), "시간");
            } catch (FatalDataException e) {
                io.println(isSyntaxError(e)
                        ? "시간 형식이 올바르지 않습니다."
                        : "유효하지 않은 시간입니다.");
            }
        }
    }

    private void editHotelFlow() {
        Hotel hotel = chooseOwnedHotel();
        if (hotel == null) {
            hostBackToMenu();
            return;
        }
        io.println("수정할 업소의 정보 선택");
        io.println("1. 업소 이름 수정");
        io.println("2. 우편번호 수정");
        io.println("3. 방 정보 수정");
        String input = io.prompt("수정할 항목의 번호를 선택하세요 : ");
        if ("1".equals(input)) {
            editHotelName(hotel);
        } else if ("2".equals(input)) {
            editHotelPostalCode(hotel);
        } else if ("3".equals(input)) {
            editRoomInfo(hotel);
        } else {
            io.println("잘못된 입력입니다.");
        }
        hostBackToMenu();
    }

    private Hotel chooseOwnedHotel() {
        List<Hotel> hotels = hostService.hotelsByHost(session.getCurrentUser());
        if (hotels.isEmpty()) {
            io.println("등록된 업소가 없습니다.");
            return null;
        }
        io.println("내 업소 목록");
        for (int i = 0; i < hotels.size(); i++) {
            io.println((i + 1) + ". " + hotels.get(i).getName() + ", " + hotels.get(i).getPostalCode());
        }
        Integer index = readIndex("수정할 업소 번호를 입력하세요 : ", hotels.size(), true);
        if (index == null) {
            return null;
        }
        return hotels.get(index - 1);
    }

    private void editHotelName(Hotel hotel) {
        while (true) {
            String newName = io.prompt("새로운 업소 이름을 입력하세요 : ");
            if (!SpecValidators.isValidHotelName(newName)) {
                io.println("유효하지 않은 업소 이름입니다.");
                continue;
            }
            hostService.renameHotel(hotel, newName);
            io.println("수정이 완료되었습니다.");
            return;
        }
    }

    private void editHotelPostalCode(Hotel hotel) {
        while (true) {
            String newPostalCode = io.prompt("새로운 우편번호를 입력하세요 : ");
            if (!SpecValidators.isValidPostalCode(newPostalCode)) {
                io.println("유효하지 않은 우편번호입니다.");
                continue;
            }
            try {
                hostService.changeHotelPostalCode(hotel, newPostalCode);
                io.println("수정이 완료되었습니다.");
                return;
            } catch (FatalDataException e) {
                io.println("이미 존재하는 우편번호입니다.");
            }
        }
    }

    private void editRoomInfo(Hotel hotel) {
        List<Room> rooms = hostService.roomsOfHotel(hotel.getPostalCode());
        if (rooms.isEmpty()) {
            io.println("등록된 방이 없습니다.");
            return;
        }
        io.println("내 방 목록");
        for (int i = 0; i < rooms.size(); i++) {
            io.println((i + 1) + ". " + Formatters.ownedRoomLine(rooms.get(i)));
        }
        Integer index = readIndex("수정할 방 번호를 입력하세요 : ", rooms.size(), true);
        if (index == null) {
            return;
        }
        Room room = rooms.get(index - 1);
        io.println("방 수정 정보 선택");
        io.println("1. 방 번호");
        io.println("2. 인원 수");
        io.println("3. 체크인 시간");
        io.println("4. 체크아웃 시간");
        String input = io.prompt("수정할 항목의 번호를 선택하세요 : ");
        try {
            if ("1".equals(input)) {
                hostService.updateRoomNumber(room, promptSingleRoomNumber());
            } else if ("2".equals(input)) {
                hostService.updateRoomCapacity(room, promptCapacity());
            } else if ("3".equals(input)) {
                hostService.updateRoomCheckIn(room, promptRequiredTime("새 체크인 시간을 입력하세요 : "));
            } else if ("4".equals(input)) {
                hostService.updateRoomCheckOut(room, promptRequiredTime("새 체크아웃 시간을 입력하세요 : "));
            } else {
                io.println("잘못된 입력입니다.");
                return;
            }
            io.println("수정이 완료되었습니다.");
        } catch (FatalDataException e) {
            io.println(e.getMessage());
        }
    }

    private int promptSingleRoomNumber() {
        while (true) {
            String text = io.prompt("새 방 번호를 입력하세요 : ");
            try {
                return SpecParsers.parseRoomNumber(text, "방 번호");
            } catch (FatalDataException e) {
                io.println(isSyntaxError(e)
                        ? "방 번호 형식이 올바르지 않습니다."
                        : "방 번호는 1 이상 9,999 이하이어야 합니다.");
            }
        }
    }

    private void manageReservationsFlow() {
        List<Reservation> reservations = hostService.reservationsForHost(session.getCurrentUser());
        if (reservations.isEmpty()) {
            io.println("예약 내역이 없습니다.");
            hostBackToMenu();
            return;
        }
        while (true) {
            io.println("예약 목록");
            io.println("취소를 승인하려면 해당 예약내역의 번호를 입력하세요.");
            for (int i = 0; i < reservations.size(); i++) {
                io.println((i + 1) + ". " + Formatters.reservationMana(reservations.get(i)));
            }
            Integer index = readIndex(">> ", reservations.size(), true);
            if (index == null) {
                hostBackToMenu();
                return;
            }
            Reservation reservation = reservations.get(index - 1);
            if (reservation.getStatus() != ReservationStatus.CANCEL_PENDING) {
                io.println("취소 요청 대기 상태의 예약만 처리할 수 있습니다.");
                continue;
            }
            io.println("취소 승인 및 예약 조회");
            io.println(Formatters.reservationMana(reservation));
            String yn = promptYesNo("해당 예약을 취소 승인하시겠습니까? (y/n) : ");
            boolean processed = hostService.processCancellation(reservation, "y".equals(yn));
            if (!processed) {
                io.println("취소 요청 대기 상태의 예약만 처리할 수 있습니다.");
            } else if ("y".equals(yn)) {
                io.println("취소 승인이 완료되었습니다.");
            } else {
                io.println("취소 승인을 거절했습니다.");
            }
            hostBackToMenu();
            return;
        }
    }

    private void adminMenuLoop() {
        while (session.isLoggedIn()) {
            io.println("");
            io.println("관리자 메뉴");
            io.println("1. 시간 관리");
            io.println("2. 로그아웃");
            String input = io.prompt(">> ");
            if ("1".equals(input)) {
                changeBaselineFlow();
            } else if ("2".equals(input)) {
                logout();
            } else {
                io.println("잘못된 입력입니다. 다시 입력해주세요.");
            }
        }
    }

    private void changeBaselineFlow() {
        while (true) {
            String input = io.prompt("새 기준 시간 (YYYY-MM-DD HH:MM): ");
            if (";".equals(input)) {
                return;
            }
            try {
                LocalDateTime newBaseline = SpecParsers.parseFlexibleDateTime(input, "새 기준 시간");
                if (!newBaseline.isAfter(store.baselineDateTime())) {
                    io.println("잘못된 기준 시간입니다. 현재 기준 시간보다 미래의 값만 입력할 수 있습니다.");
                    continue;
                }
                store.setBaselineDateTime(newBaseline);
                store.saveTime();
                integrityService.runAutomaticMaintenance();
                store.saveAll();
                io.println("기준 시간이 변경되었습니다.");
                return;
            } catch (FatalDataException e) {
                io.println("잘못된 기준 시간입니다. 현재 기준 시간보다 미래의 값만 입력할 수 있습니다.");
            }
        }
    }

    private LocalDate promptDate(String prompt) {
        while (true) {
            String input = io.prompt(prompt);
            if (";".equals(input)) {
                return null;
            }
            try {
                return SpecParsers.parseDate(input, "날짜");
            } catch (FatalDataException e) {
                io.println(isSyntaxError(e)
                        ? "날짜 형식이 올바르지 않습니다."
                        : "유효하지 않은 날짜입니다.");
            }
        }
    }

    private LocalTime promptReservationCheckInTime(Room room) {
        while (true) {
            String input = io.prompt("체크인 시간을 입력해주세요:\n>> ");
            if (";".equals(input)) {
                return null;
            }
            try {
                LocalTime time = SpecParsers.parseTime(input, "체크인 시간");
                if (time.isBefore(room.getCheckInTime())) {
                    io.println("체크인 시간은 객실 기본 체크인 시간보다 같거나 늦어야 합니다.");
                    continue;
                }
                return time;
            } catch (FatalDataException e) {
                io.println(isSyntaxError(e)
                        ? "체크인 시간 형식이 올바르지 않습니다."
                        : "유효하지 않은 체크인 시간입니다.");
            }
        }
    }

    private LocalTime promptReservationCheckOutTime(Room room) {
        while (true) {
            String input = io.prompt("체크아웃 시간을 입력해주세요:\n>> ");
            if (";".equals(input)) {
                return null;
            }
            try {
                LocalTime time = SpecParsers.parseTime(input, "체크아웃 시간");
                if (time.isAfter(room.getCheckOutTime())) {
                    io.println("체크아웃 시간은 객실 기본 체크아웃 시간보다 같거나 빨라야 합니다.");
                    continue;
                }
                return time;
            } catch (FatalDataException e) {
                io.println(isSyntaxError(e)
                        ? "체크아웃 시간 형식이 올바르지 않습니다."
                        : "유효하지 않은 체크아웃 시간입니다.");
            }
        }
    }

    private boolean isSyntaxError(FatalDataException exception) {
        return exception.getMessage() != null && exception.getMessage().endsWith("문법 오류");
    }

    private Integer readIndex(String prompt, int size, boolean allowBack) {
        while (true) {
            String input = io.prompt(prompt);
            if (allowBack && ";".equals(input)) {
                return null;
            }
            if (!input.matches("^[0-9]+$")) {
                io.println("잘못된 입력입니다. 다시 입력해주세요.");
                continue;
            }
            int index = Integer.parseInt(input);
            if (index < 1 || index > size) {
                io.println("잘못된 입력입니다. 다시 입력해주세요.");
                continue;
            }
            return index;
        }
    }

    private String promptYesNo(String prompt) {
        while (true) {
            String input = io.prompt(prompt).toLowerCase();
            if ("y".equals(input) || "n".equals(input)) {
                return input;
            }
            io.println("잘못된 입력입니다. 다시 입력해주세요.");
        }
    }

    private void guestBackToMenu() {
        io.println("고객 메인 메뉴로 이동합니다.");
    }

    private void hostBackToMenu() {
        io.println("사장 메인 메뉴로 이동합니다.");
    }

    private void logout() {
        io.println("로그아웃 합니다.");
        session.logout();
    }

    private static final class StayInput {
        private final LocalDate checkInDate;
        private final LocalTime checkInTime;
        private final LocalDate checkOutDate;
        private final LocalTime checkOutTime;

        private StayInput(LocalDate checkInDate, LocalTime checkInTime,
                          LocalDate checkOutDate, LocalTime checkOutTime) {
            this.checkInDate = checkInDate;
            this.checkInTime = checkInTime;
            this.checkOutDate = checkOutDate;
            this.checkOutTime = checkOutTime;
        }
    }
}
