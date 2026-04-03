package com.aurasoft.booky.model;

public class SeatModel {

    private String seatName;
    private int status;
    private String selectedGender = "";

    public SeatModel(String seatName, int status) {
        this.seatName = seatName;
        this.status = status;
        this.selectedGender = "";
    }

    public String getSeatName() {
        return seatName;
    }

    public void setSeatName(String seatName) {
        this.seatName = seatName;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getSelectedGender() {
        return selectedGender;
    }

    public void setSelectedGender(String selectedGender) {
        this.selectedGender = selectedGender;
    }
}
