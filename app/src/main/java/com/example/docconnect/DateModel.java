package com.example.docconnect;

/**
 * Model class representing a single day in the custom horizontal calendar.
 * This class maps date information to the UI and helps track user selection state.
 */
public class DateModel {

    // Unique identifier formatted as "yyyy-MM-dd", used for Firebase database paths
    private String dateKey;

    // Short name of the day (e.g., "Mon", "Tue"), used for the top text in the calendar item
    private String dayName;

    // The specific day of the month (e.g., "25"), used for the main number in the calendar item
    private String dateNumber;

    // Tracks if this specific date is currently clicked/highlighted by the user
    private boolean selected;

    /**
     * Required empty constructor for Firebase Realtime Database.
     */
    public DateModel() {}

    /**
     * Constructor for creating a new DateModel instance.
     * @param dateKey The database-friendly date string.
     * @param dayName The display name for the day of the week.
     * @param dateNumber The display number for the day.
     */
    public DateModel(String dateKey, String dayName, String dateNumber) {
        this.dateKey = dateKey;
        this.dayName = dayName;
        this.dateNumber = dateNumber;
    }

    // --- Getters and Setters ---

    /**
     * @return The date formatted as yyyy-MM-dd.
     */
    public String getDateKey() { return dateKey; }

    /**
     * @return The short day name (e.g., Mon).
     */
    public String getDayName() { return dayName; }

    /**
     * @return The day of the month (e.g., 25).
     */
    public String getDateNumber() { return dateNumber; }

    /**
     * @return true if the date is currently selected in the UI.
     */
    public boolean isSelected() { return selected; }

    /**
     * @param selected Sets the visual selection state of the date.
     */
    public void setSelected(boolean selected) { this.selected = selected; }
}