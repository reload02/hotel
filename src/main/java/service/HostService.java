package service;

import common.FatalDataException;
import domain.Hotel;
import domain.Penalty;
import domain.Reservation;
import domain.ReservationStatus;
import domain.Room;
import domain.Suspension;
import domain.User;
import persistence.FlatFileStore;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HostService {
    private final FlatFileStore store;
    private final GuestService guestService;

    public HostService(FlatFileStore store, GuestService guestService) {
        this.store = store;
        this.guestService = guestService;
    }

    public List<Hotel> hotelsByHost(User host) {
        List<Hotel> results = new ArrayList<>();
        for (Hotel hotel : store.hotels()) {
            if (hotel.getHostIdKey().equals(host.getIdKey())) {
                results.add(hotel);
            }
        }
        results.sort(Comparator.comparing(Hotel::getName).thenComparing(Hotel::getPostalCode));
        return results;
    }

    public Hotel registerHotel(User host, String name, String postalCode, List<Room> rooms) {
        if (findHotelByPostalCode(postalCode) != null) {
            throw new FatalDataException("이미 등록된 우편번호입니다.");
        }
        Hotel hotel = new Hotel(name, postalCode, host.getId());
        store.hotels().add(hotel);
        for (Room room : rooms) {
            room.setPostalCode(postalCode);
            store.rooms().add(room);
        }
        store.saveHotels();
        store.saveRooms();
        return hotel;
    }

    public Hotel findHotelByPostalCode(String postalCode) {
        for (Hotel hotel : store.hotels()) {
            if (hotel.getPostalCode().equals(postalCode)) {
                return hotel;
            }
        }
        return null;
    }

    public List<Room> roomsOfHotel(String postalCode) {
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

    public boolean isRoomNumberTaken(String postalCode, int roomNumber, Room exclude) {
        for (Room room : store.rooms()) {
            if (room == exclude) {
                continue;
            }
            if (room.getPostalCode().equals(postalCode) && room.getRoomNumber() == roomNumber) {
                return true;
            }
        }
        return false;
    }

    public void renameHotel(Hotel hotel, String newName) {
        hotel.setName(newName);
        store.saveHotels();
    }

    public void changeHotelPostalCode(Hotel hotel, String newPostalCode) {
        if (!hotel.getPostalCode().equals(newPostalCode) && findHotelByPostalCode(newPostalCode) != null) {
            throw new FatalDataException("이미 등록된 우편번호입니다.");
        }
        String oldPostalCode = hotel.getPostalCode();
        hotel.setPostalCode(newPostalCode);
        for (Room room : store.rooms()) {
            if (room.getPostalCode().equals(oldPostalCode)) {
                room.setPostalCode(newPostalCode);
            }
        }
        for (Reservation reservation : store.reservations()) {
            if (reservation.getPostalCode().equals(oldPostalCode)) {
                reservation.setPostalCode(newPostalCode);
            }
        }
        for (Penalty penalty : store.penalties()) {
            if (penalty.getPostalCode().equals(oldPostalCode)) {
                penalty.setPostalCode(newPostalCode);
            }
        }
        for (Suspension suspension : store.suspensions()) {
            if (suspension.getPostalCode().equals(oldPostalCode)) {
                suspension.setPostalCode(newPostalCode);
            }
        }
        store.saveHotels();
        store.saveRooms();
        store.saveReservations();
        store.savePenalties();
        store.saveSuspensions();
    }

    public void updateRoomNumber(Room room, int newRoomNumber) {
        if (isRoomNumberTaken(room.getPostalCode(), newRoomNumber, room)) {
            throw new FatalDataException("같은 업소의 다른 방과 번호가 중복됩니다.");
        }
        room.setRoomNumber(newRoomNumber);
        store.saveRooms();
    }

    public void updateRoomCapacity(Room room, int newCapacity) {
        room.setCapacity(newCapacity);
        store.saveRooms();
    }

    public void updateRoomCheckIn(Room room, LocalTime newTime) {
        room.setCheckInTime(newTime);
        store.saveRooms();
    }

    public void updateRoomCheckOut(Room room, LocalTime newTime) {
        room.setCheckOutTime(newTime);
        store.saveRooms();
    }

    public List<Reservation> reservationsForHost(User host) {
        List<Hotel> hotels = hotelsByHost(host);
        List<Reservation> results = new ArrayList<>();
        for (Reservation reservation : store.reservations()) {
            if (isOwnedReservationSnapshot(reservation, hotels)) {
                results.add(reservation);
            }
        }
        results.sort(Comparator.comparing(Reservation::getCheckInDateTime)
                .thenComparing(Reservation::getPostalCode)
                .thenComparingInt(Reservation::getRoomNumber));
        return results;
    }

    public boolean processCancellation(Reservation reservation, boolean approve) {
        if (guestService.shouldRestoreCancelPending(reservation, store.baselineDateTime())) {
            reservation.setStatus(ReservationStatus.RESERVED);
            reservation.clearCancelRequest();
            store.saveReservations();
            return false;
        }
        if (reservation.getStatus() != ReservationStatus.CANCEL_PENDING) {
            return false;
        }
        reservation.setStatus(approve ? ReservationStatus.CANCELLED : ReservationStatus.RESERVED);
        if (!approve) {
            reservation.clearCancelRequest();
        }
        store.saveReservations();
        return true;
    }

    public String hotelNameFor(Reservation reservation) {
        Hotel hotel = findHotelByPostalCode(reservation.getPostalCode());
        return hotel == null ? reservation.getHotelName() : hotel.getName();
    }

    private boolean isOwnedReservationSnapshot(Reservation reservation, List<Hotel> hotels) {
        for (Hotel hotel : hotels) {
            if (reservation.getPostalCode().equals(hotel.getPostalCode())
                    || reservation.getHotelName().equals(hotel.getName())) {
                return true;
            }
        }
        return false;
    }
}
