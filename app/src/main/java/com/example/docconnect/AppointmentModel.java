package com.example.docconnect;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * APPOINTMENT MODEL - A2Z UPDATED
 * Fixed: 'popupShown' type mismatch error.
 * Optimized: Handles both Boolean and String values from Firebase.
 */
@IgnoreExtraProperties
public class AppointmentModel {

    private String bookingId;
    private String doctorId;
    private String clinicAddress;
    private String clinicName;
    private String date;
    private String doctorFee;
    private String doctorName;
    private String doctorSpecialty;
    private String status;
    private String time;
    private String doctorImage;
    private boolean rated;

    // A2Z FIX: Changed from String to boolean to match the logic in HomeFragment
    private boolean popupShown;

    /**
     * Required empty constructor for Firebase
     */
    public AppointmentModel() {}

    /**
     * Full Constructor
     */
    public AppointmentModel(String bookingId, boolean popupShown, String doctorId, String clinicAddress, String clinicName,
                            String date, String doctorFee, String doctorName,
                            String doctorSpecialty, String status, String time,
                            String doctorImage, boolean rated) {
        this.bookingId = bookingId;
        this.popupShown = popupShown;
        this.doctorId = doctorId;
        this.clinicAddress = clinicAddress;
        this.clinicName = clinicName;
        this.date = date;
        this.doctorFee = doctorFee;
        this.doctorName = doctorName;
        this.doctorSpecialty = doctorSpecialty;
        this.status = status;
        this.time = time;
        this.doctorImage = doctorImage;
        this.rated = rated;
    }

    // --- GETTERS ---
    public String getBookingId() { return bookingId; }
    public String getDoctorId() { return doctorId; }
    public String getClinicAddress() { return clinicAddress; }
    public String getClinicName() { return clinicName; }
    public String getDate() { return date; }
    public String getDoctorFee() { return doctorFee; }
    public String getDoctorName() { return doctorName; }
    public String getDoctorSpecialty() { return doctorSpecialty; }
    public String getSpecialty() { return doctorSpecialty; }
    public String getStatus() { return status; }
    public String getTime() { return time; }
    public String getDoctorImage() { return doctorImage; }
    public boolean isRated() { return rated; }

    /**
     * A2Z FIX: This now returns the boolean required by your HomeFragment if() checks.
     */
    public boolean isPopupShown() {
        return popupShown;
    }

    // --- SETTERS ---
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public void setDoctorImage(String doctorImage) { this.doctorImage = doctorImage; }
    public void setStatus(String status) { this.status = status; }
    public void setRated(boolean rated) { this.rated = rated; }

    /**
     * A2Z SMART SETTER:
     * If Firebase sends a Boolean (true) or a String ("true"), this handles both
     * to prevent "Incompatible Types" crashes.
     */
    public void setPopupShown(Object popupShown) {
        if (popupShown instanceof Boolean) {
            this.popupShown = (Boolean) popupShown;
        } else if (popupShown instanceof String) {
            this.popupShown = Boolean.parseBoolean((String) popupShown);
        }
    }
}