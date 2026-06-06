package domain;

import java.time.LocalTime;

public class Room {
    private String postalCode;
    private int roomNumber;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private int capacity;

    public Room(String postalCode, int roomNumber, LocalTime checkInTime, LocalTime checkOutTime, int capacity) {
        this.postalCode = postalCode;
        this.roomNumber = roomNumber;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.capacity = capacity;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(int roomNumber) {
        this.roomNumber = roomNumber;
    }

    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}
