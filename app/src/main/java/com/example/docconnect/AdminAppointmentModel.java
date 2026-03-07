package com.example.docconnect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Objects;

/**
 * Data model representing an appointment from the administrator's perspective.
 * Updated to include all fields found in the Firebase database to prevent ClassMapper warnings.
 */
public class AdminAppointmentModel {

    // --- Existing Fields ---
    private String id;
    private String userId;
    private String doctorName;
    private String patientName;
    private String doctorSpecialty;
    private String status;
    private String date;
    private String time;

    // --- Newly Added Fields (to fix Logcat errors) ---
    private String clinicAddress;
    private String patientId;
    private String patientGender;
    private String bookingId;
    private String doctorFee;
    private String patientImage;
    private String doctorId;
    private String consultationMedium;
    private String patientAge;
    private String doctorImage;
    private Object timestamp; // Firebase timestamps are often Long or Map
    private String clinicName;

    /**
     * Required default constructor for Firebase.
     */
    public AdminAppointmentModel() {}

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDoctorName() { return doctorName != null ? doctorName : ""; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getPatientName() { return patientName != null ? patientName : ""; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorSpecialty() { return doctorSpecialty != null ? doctorSpecialty : ""; }
    public void setDoctorSpecialty(String doctorSpecialty) { this.doctorSpecialty = doctorSpecialty; }

    public String getStatus() { return status != null ? status : "PENDING"; }
    public void setStatus(String status) { this.status = status; }

    public String getDate() { return date != null ? date : ""; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time != null ? time : ""; }
    public void setTime(String time) { this.time = time; }

    // Setters/Getters for the new fields
    public String getClinicAddress() { return clinicAddress; }
    public void setClinicAddress(String clinicAddress) { this.clinicAddress = clinicAddress; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientGender() { return patientGender; }
    public void setPatientGender(String patientGender) { this.patientGender = patientGender; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getDoctorFee() { return doctorFee; }
    public void setDoctorFee(String doctorFee) { this.doctorFee = doctorFee; }

    public String getPatientImage() { return patientImage; }
    public void setPatientImage(String patientImage) { this.patientImage = patientImage; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getConsultationMedium() { return consultationMedium; }
    public void setConsultationMedium(String consultationMedium) { this.consultationMedium = consultationMedium; }

    public String getPatientAge() { return patientAge; }
    public void setPatientAge(String patientAge) { this.patientAge = patientAge; }

    public String getDoctorImage() { return doctorImage; }
    public void setDoctorImage(String doctorImage) { this.doctorImage = doctorImage; }

    public Object getTimestamp() { return timestamp; }
    public void setTimestamp(Object timestamp) { this.timestamp = timestamp; }

    public String getClinicName() { return clinicName; }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }

    // --- DiffUtil & Logic ---

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdminAppointmentModel that = (AdminAppointmentModel) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(bookingId, that.bookingId) && // Use unique IDs for comparison
                Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bookingId, status);
    }

    @NonNull
    @Override
    public String toString() {
        return "AdminAppointmentModel{" +
                "bookingId='" + bookingId + '\'' +
                ", patient='" + patientName + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}