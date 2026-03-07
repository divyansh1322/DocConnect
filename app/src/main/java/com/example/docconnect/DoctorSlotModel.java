package com.example.docconnect;

import com.google.firebase.database.Exclude; // Import this!
import java.io.Serializable;

public class DoctorSlotModel implements Serializable {

    private String dateLabel;
    private String timeLabel;
    private long startMillis;
    private long endMillis;
    private String dateKey;
    private String status;

    // Required empty constructor for Firebase
    public DoctorSlotModel() {}

    public DoctorSlotModel(String dateLabel, String timeLabel,
                           long startMillis, long endMillis, String dateKey) {
        this.dateLabel = dateLabel;
        this.timeLabel = timeLabel;
        this.startMillis = startMillis;
        this.endMillis = endMillis;
        this.dateKey = dateKey;
        this.status = "AVAILABLE";
    }

    // --- Standard Getters/Setters ---
    public String getDateLabel() { return dateLabel; }
    public void setDateLabel(String dateLabel) { this.dateLabel = dateLabel; }

    public String getTimeLabel() { return timeLabel; }
    public void setTimeLabel(String timeLabel) { this.timeLabel = timeLabel; }

    public long getStartMillis() { return startMillis; }
    public void setStartMillis(long startMillis) { this.startMillis = startMillis; }

    public long getEndMillis() { return endMillis; }
    public void setEndMillis(long endMillis) { this.endMillis = endMillis; }

    public String getDateKey() { return dateKey; }
    public void setDateKey(String dateKey) { this.dateKey = dateKey; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // --- Helper Methods (Excluded from Firebase) ---

    @Exclude // Prevents Firebase from creating a "timeDisplay" field
    public String getTimeDisplay() {
        return timeLabel;
    }

    @Exclude // Prevents Firebase from creating an "expired" field
    public boolean isExpired() {
        return System.currentTimeMillis() > endMillis;
    }
}