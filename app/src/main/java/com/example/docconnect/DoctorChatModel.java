package com.example.docconnect;

import com.google.firebase.database.IgnoreExtraProperties;
import java.io.Serializable;

/**
 * DoctorChatModel: The Unified Data Blueprint for Doctor-side Operations.
 * Standardized for DocConnect 2026.
 * Includes all missing fields identified in Logcat to prevent ClassMapper warnings.
 */
@IgnoreExtraProperties
public class DoctorChatModel implements Serializable {

    // --- EXISTING APPOINTMENT DATA FIELDS ---
    private String patientId;
    private String doctorId;
    private String bookingId;
    private String status;
    private String consultationMedium;
    private String patientName;
    private String patientAge;
    private String patientGender;
    private String patientImage;
    private String date;
    private String time;

    // --- NEWLY ADDED FIELDS (To resolve Logcat Warnings) ---
    private String doctorName;
    private String doctorImage;
    private String doctorSpecialty;
    private String doctorFee;
    private String clinicName;
    private String clinicAddress;
    private Object timestamp; // Handles both Long and Firebase ServerValue.TIMESTAMP

    public DoctorChatModel() {
        // Required for Firebase DataSnapshot.getValue(DoctorChatModel.class)
    }

    public DoctorChatModel(String patientId, String doctorId, String bookingId, String status,
                           String consultationMedium, String patientName, String patientAge,
                           String patientGender, String date, String time, String patientImage) {
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.bookingId = bookingId;
        this.status = status;
        this.consultationMedium = consultationMedium;
        this.patientName = patientName;
        this.patientAge = patientAge;
        this.patientGender = patientGender;
        this.date = date;
        this.time = time;
        this.patientImage = patientImage;
    }

    // --- GETTERS (All Attributes) ---
    public String getPatientId() { return patientId; }
    public String getDoctorId() { return doctorId; }
    public String getBookingId() { return bookingId; }
    public String getStatus() { return status; }
    public String getConsultationMedium() { return consultationMedium; }
    public String getPatientName() { return patientName; }
    public String getPatientAge() { return patientAge; }
    public String getPatientGender() { return patientGender; }
    public String getPatientImage() { return patientImage; }
    public String getDate() { return date; }
    public String getTime() { return time; }

    // Getters for missing fields
    public String getDoctorName() { return doctorName; }
    public String getDoctorImage() { return doctorImage; }
    public String getDoctorSpecialty() { return doctorSpecialty; }
    public String getDoctorFee() { return doctorFee; }
    public String getClinicName() { return clinicName; }
    public String getClinicAddress() { return clinicAddress; }
    public Object getTimestamp() { return timestamp; }

    // --- SETTERS (All Attributes) ---
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public void setStatus(String status) { this.status = status; }
    public void setConsultationMedium(String consultationMedium) { this.consultationMedium = consultationMedium; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public void setPatientAge(String patientAge) { this.patientAge = patientAge; }
    public void setPatientGender(String patientGender) { this.patientGender = patientGender; }
    public void setPatientImage(String patientImage) { this.patientImage = patientImage; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }

    // Setters for missing fields
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public void setDoctorImage(String doctorImage) { this.doctorImage = doctorImage; }
    public void setDoctorSpecialty(String doctorSpecialty) { this.doctorSpecialty = doctorSpecialty; }
    public void setDoctorFee(String doctorFee) { this.doctorFee = doctorFee; }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }
    public void setClinicAddress(String clinicAddress) { this.clinicAddress = clinicAddress; }
    public void setTimestamp(Object timestamp) { this.timestamp = timestamp; }

    /**
     * NESTED CHAT MESSAGE CLASS
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
        public String getText() { return message; }
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