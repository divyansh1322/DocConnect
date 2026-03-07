package com.example.docconnect;

import com.google.firebase.database.PropertyName;
import java.io.Serializable;

/**
 * Updated Model representing an appointment slot.
 * Includes all original attributes plus missing fields found in Logcat
 * (timeDisplay, bookedBy, expired) to fix ClassMapper warnings.
 */
public class SlotsModel implements Serializable {

    // --- Original Attributes ---
    private String timeLabel;
    private String dateLabel;
    private String dateKey;
    private long startMillis;
    private long endMillis;
    private String status;

    // --- Attributes added based on Logcat Warnings ---
    private String timeDisplay; // Logcat showed: "No setter/field for timeDisplay"
    private String bookedBy;    // Logcat showed: "No setter/field for bookedBy"
    private boolean expired;    // Logcat showed: "No setter/field for expired"

    /**
     * Required empty constructor for Firebase Realtime Database.
     */
    public SlotsModel() {}

    // --- GETTERS AND SETTERS ---

    @PropertyName("timeLabel")
    public String getTimeLabel() { return timeLabel; }

    @PropertyName("timeLabel")
    public void setTimeLabel(String timeLabel) { this.timeLabel = timeLabel; }

    @PropertyName("dateLabel")
    public String getDateLabel() { return dateLabel; }

    @PropertyName("dateLabel")
    public void setDateLabel(String dateLabel) { this.dateLabel = dateLabel; }

    @PropertyName("dateKey")
    public String getDateKey() { return dateKey; }

    @PropertyName("dateKey")
    public void setDateKey(String dateKey) { this.dateKey = dateKey; }

    @PropertyName("startMillis")
    public long getStartMillis() { return startMillis; }

    @PropertyName("startMillis")
    public void setStartMillis(long startMillis) { this.startMillis = startMillis; }

    @PropertyName("endMillis")
    public long getEndMillis() { return endMillis; }

    @PropertyName("endMillis")
    public void setEndMillis(long endMillis) { this.endMillis = endMillis; }

    @PropertyName("status")
    public String getStatus() { return status; }

    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }

    // --- NEW GETTERS AND SETTERS (Logic synchronization) ---

    @PropertyName("timeDisplay")
    public String getTimeDisplay() {
        // Fallback logic: if timeDisplay is null, use timeLabel
        return timeDisplay != null ? timeDisplay : timeLabel;
    }

    @PropertyName("timeDisplay")
    public void setTimeDisplay(String timeDisplay) {
        this.timeDisplay = timeDisplay;
    }

    @PropertyName("bookedBy")
    public String getBookedBy() { return bookedBy; }

    @PropertyName("bookedBy")
    public void setBookedBy(String bookedBy) { this.bookedBy = bookedBy; }

    @PropertyName("expired")
    public boolean isExpired() { return expired; }

    @PropertyName("expired")
    public void setExpired(boolean expired) { this.expired = expired; }
}