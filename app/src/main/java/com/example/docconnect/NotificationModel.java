package com.example.docconnect;

public class NotificationModel {
    private String title;
    private String message;
    private String timestamp;
    private String imageUrl;
    private String type;
    private boolean read;    // CRITICAL: Added for the "Mark as Read" logic

    // Required empty constructor for Firebase
    public NotificationModel() {}

    public NotificationModel(String title, String message, String timestamp, String imageUrl, String type, boolean read) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.imageUrl = imageUrl;
        this.type = type;
        this.read = read;
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    // Logic Helper: This is what the Activity calls
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}