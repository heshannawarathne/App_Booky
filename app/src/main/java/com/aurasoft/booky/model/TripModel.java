package com.aurasoft.booky.model;

import com.google.firebase.Timestamp;

import java.util.ArrayList;

public class TripModel {
    private String busNo;
    private String date;
    private String time;
    private String fromLocation;
    private String toLocation;
    private String status;
    private String userId;
    private long totalPrice;
    private ArrayList<String> seats;

    private Timestamp timestamp;

    public TripModel() {
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public String getBusNo() {
        return busNo;
    }

    public void setBusNo(String busNo) {
        this.busNo = busNo;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(long totalPrice) {
        this.totalPrice = totalPrice;
    }

    public ArrayList<String> getSeats() {
        return seats;
    }

    public void setSeats(ArrayList<String> seats) {
        this.seats = seats;
    }

    // 2. මෙන්න මේ Getter එක නැති නිසයි Adapter එකේ error එක ආවේ
    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedDate() {
        return date;
    }

    public String getFormattedTime() {
        return time;
    }

    public String getSeatCount() {
        return (seats != null) ? String.valueOf(seats.size()) : "0";
    }
}