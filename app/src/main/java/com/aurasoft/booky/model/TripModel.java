package com.aurasoft.booky.model;

public class TripModel {

    private String busName, date, from, to, price, status;

    public TripModel() {
    }

    public TripModel(String busName, String date, String from, String to, String price, String status) {
        this.busName = busName;
        this.date = date;
        this.from = from;
        this.to = to;
        this.price = price;
        this.status = status;
    }

    public String getBusName() {

        if (busName == null || busName.isEmpty()) {
            return "Booky Higway Express";
        }
        return busName;
    }

    public void setBusName(String busName) {
        this.busName = busName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
