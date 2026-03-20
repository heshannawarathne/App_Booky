package com.aurasoft.booky.model;

public class NotificationModel {

    String title, message, userId;

    private long timestamp;

    private boolean isRead;

    public NotificationModel() {
    }

    public NotificationModel(String title, String message, String userId, long timestamp, boolean isRead) {
        this.title = title;
        this.message = message;
        this.userId = userId;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }



    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
