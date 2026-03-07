package com.example.docconnect;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Model class representing an upcoming appointment for the doctor.
 * The @IgnoreExtraProperties annotation prevents the app from crashing if Firebase
 * contains properties not defined in this class.
 */
@IgnoreExtraProperties
public class DoctorUpcomingModel {
    // Basic patient and appointment metadata
    private String bookingId;
    private String patientName;
    private String patientImage;
    private String patientId;
    private String time;
    private String date;
    private String status; // e.g., "UPCOMING", "COMPLETED", "CANCELLED"
    private String patientAge;
    private String patientGender;

    /**
     * Required empty constructor for Firebase Realtime Database.
     */
    public DoctorUpcomingModel() {}

    // --- FIREBASE KEY MAPPING ---

    /**
     * Returns the Firebase Push ID. Used in Fragments to identify specific appointments.
     */
    public String getKey() { return bookingId; }

    /**
     * Manually sets the bookingId. Useful when retrieving the key via DataSnapshot.getKey().
     */
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    /**
     * Safety setter to ensure 'key' and 'bookingId' refer to the same database reference.
     */
    public void setKey(String key) { this.bookingId = key; }

    // --- GETTERS (with Null Safety) ---

    /**
     * @return The patient's name, defaulting to "No Name" if null.
     */
    public String getPatientName() { return patientName != null ? patientName : "No Name"; }

    public String getPatientAge() { return patientAge != null ? patientAge : "0"; }

    public String getPatientGender() { return patientGender != null ? patientGender : "N/A"; }

    public String getPatientImage() { return patientImage; }

    /**
     * @return Formatted time string (e.g., "10:30 AM").
     */
    public String getTime() { return time != null ? time : "--:--"; }

    public String getStatus() { return status != null ? status : "Upcoming"; }

    public String getBookingId() { return bookingId; }

    public String getDate() { return date; }

    public String getPatientId() { return patientId; }

    // --- SETTERS ---
    // Standard setters used by Firebase to populate the object

    public void setPatientName(String patientName) { this.patientName = patientName; }
    public void setPatientAge(String patientAge) { this.patientAge = patientAge; }
    public void setPatientGender(String patientGender) { this.patientGender = patientGender; }
    public void setPatientImage(String patientImage) { this.patientImage = patientImage; }
    public void setTime(String time) { this.time = time; }
    public void setDate(String date) { this.date = date; }
    public void setStatus(String status) { this.status = status; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
}