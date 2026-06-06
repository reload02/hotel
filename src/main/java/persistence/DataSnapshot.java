package persistence;

import domain.Hotel;
import domain.Penalty;
import domain.Reservation;
import domain.Room;
import domain.Suspension;
import domain.SystemState;
import domain.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DataSnapshot {
    private final List<User> users = new ArrayList<>();
    private final List<Hotel> hotels = new ArrayList<>();
    private final List<Room> rooms = new ArrayList<>();
    private final List<Reservation> reservations = new ArrayList<>();
    private final List<Penalty> penalties = new ArrayList<>();
    private final List<Suspension> suspensions = new ArrayList<>();
    private final SystemState systemState = new SystemState();
    private LocalDateTime baselineDateTime;

    public List<User> getUsers() {
        return users;
    }

    public List<Hotel> getHotels() {
        return hotels;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public List<Penalty> getPenalties() {
        return penalties;
    }

    public List<Suspension> getSuspensions() {
        return suspensions;
    }

    public SystemState getSystemState() {
        return systemState;
    }

    public LocalDateTime getBaselineDateTime() {
        return baselineDateTime;
    }

    public void setBaselineDateTime(LocalDateTime baselineDateTime) {
        this.baselineDateTime = baselineDateTime;
    }

    public LocalDate getBaselineDate() {
        return baselineDateTime.toLocalDate();
    }
}
