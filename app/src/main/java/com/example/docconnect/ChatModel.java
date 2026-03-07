package com.example.docconnect;

import com.google.firebase.database.IgnoreExtraProperties;
import java.io.Serializable;

/**
 * ChatModel: The Unified Data Blueprint for DocConnect 2026.
 * * FIXED: Added missing fields identified in Logcat to prevent ClassMapper warnings.
 * Added @IgnoreExtraProperties to safeguard against future schema changes.
 */
@IgnoreExtraProperties
public class ChatModel implements Serializable {

    // --- EXISTING FIREBASE MAPPING FIELDS ---
    private String patientId;
    private String doctorId;
    private String bookingId;
    private String status;
    private String doctorName;
    private String doctorImage;
    private String doctorSpecialty;
    private String doctorFee;
    private String consultationMedium;
    private String patientName;
    private String patientAge;
    private String patientGender;
    private String patientImage;
    private String date;
    private String time;
    private String clinicName;
    private String clinicAddress;
    private Object timestamp;

    // --- NEWLY ADDED FIELDS (To resolve Logcat Warnings) ---
    private String rescheduledFrom; // Tracks original booking ID if rescheduled
    private String newBookingId;    // Tracks the new ID if this one was replaced
    private boolean isRated;        // Changed from 'rated' to 'isRated' to match standard Firebase boolean mapping
    private boolean isAlertShown;   // Tracks if a notification alert was shown to user

    /**
     * Required Empty Constructor.
     * Essential for Firebase's DataSnapshot.getValue(ChatModel.class)
     */
    public ChatModel() {}

    /**
     * Full Constructor.
     */
    public ChatModel(String patientId, String doctorName, String doctorImage, String doctorId, String doctorFee, String bookingId, String status,
                     String consultationMedium, String patientName, String patientAge,
                     String patientGender, String date, String time, String patientImage, String doctorSpecialty ,String clinicAddress, String clinicName) {
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.doctorImage = doctorImage;
        this.doctorFee = doctorFee;
        this.doctorSpecialty = doctorSpecialty;
        this.bookingId = bookingId;
        this.clinicAddress = clinicAddress;
        this.clinicName = getClinicName();
        this.status = status;
        this.consultationMedium = consultationMedium;
        this.patientName = patientName;
        this.patientAge = patientAge;
        this.patientGender = patientGender;
        this.date = date;
        this.time = time;
        this.patientImage = patientImage;
    }

    // --- GETTERS ---
    public String getPatientId() { return patientId; }
    public String getDoctorId() { return doctorId; }
    public String getDoctorName() { return doctorName; }
    public String getDoctorFee() { return doctorFee; }
    public String getDoctorImage() { return doctorImage; }
    public String getDoctorSpecialty() { return doctorSpecialty; }
    public String getBookingId() { return bookingId; }
    public String getStatus() { return status; }
    public String getConsultationMedium() { return consultationMedium; }
    public String getPatientName() { return patientName; }
    public String getPatientAge() { return patientAge; }
    public String getPatientGender() { return patientGender; }
    public String getPatientImage() { return patientImage; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getClinicName() { return clinicName; }
    public String getClinicAddress() { return clinicAddress; }
    public Object getTimestamp() { return timestamp; }

    // Getters for newly added fields
    public String getRescheduledFrom() { return rescheduledFrom; }
    public String getNewBookingId() { return newBookingId; }
    public boolean isRated() { return isRated; }
    public boolean isAlertShown() { return isAlertShown; }

    // --- SETTERS ---
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public void setDoctorImage(String doctorImage) { this.doctorImage = doctorImage; }
    public void setDoctorFee(String doctorFee) { this.doctorFee = doctorFee; }
    public void setDoctorSpecialty(String doctorSpecialty) { this.doctorSpecialty = doctorSpecialty; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public void setStatus(String status) { this.status = status; }
    public void setConsultationMedium(String consultationMedium) { this.consultationMedium = consultationMedium; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public void setPatientAge(String patientAge) { this.patientAge = patientAge; }
    public void setPatientGender(String patientGender) { this.patientGender = patientGender; }
    public void setPatientImage(String patientImage) { this.patientImage = patientImage; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }
    public void setClinicAddress(String clinicAddress) { this.clinicAddress = clinicAddress; }
    public void setTimestamp(Object timestamp) { this.timestamp = timestamp; }

    // Setters for newly added fields
    public void setRescheduledFrom(String rescheduledFrom) { this.rescheduledFrom = rescheduledFrom; }
    public void setNewBookingId(String newBookingId) { this.newBookingId = newBookingId; }
    public void setRated(boolean rated) { isRated = rated; }
    public void setAlertShown(boolean alertShown) { isAlertShown = alertShown; }

    /**
     * Message Static Class for the Chat logic.
     */
    public static class Message implements Serializable {
        private String message;
        private String senderId;
        private String patientId;
        private String doctorId;
        private long timestamp;
        private String type;

        public Message() {}

        public Message(String message, String senderId, String patientId, String doctorId, long timestamp, String type) {
            this.message = message;
            this.senderId = senderId;
            this.patientId = patientId;
            this.doctorId = doctorId;
            this.timestamp = timestamp;
            this.type = type;
        }

        public String getMessage() { return message; }
        public String getText() { return message; } // Helper for older codebases
        public String getSenderId() { return senderId; }
        public String getPatientId() { return patientId; }
        public String getDoctorId() { return doctorId; }
        public long getTimestamp() { return timestamp; }
        public String getType() { return type; }

        public void setMessage(String message) { this.message = message; }
        public void setSenderId(String senderId) { this.senderId = senderId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public void setType(String type) { this.type = type; }
    }
}