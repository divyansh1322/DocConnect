package com.example.docconnect;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Model class representing the specific details provided by a patient during the booking process.
 * Unlike the main User profile, this captures the 'Purpose' of a specific consultation.
 */
@IgnoreExtraProperties
public class PatientDetails {
    // Demographics and Contact Info
    public String fullName;
    public String email;
    public String phone;
    public String gender;
    public String age;

    // The clinical reason for the appointment (e.g., "Regular Checkup", "Flu symptoms")
    public String purpose;

    /**
     * MANDATORY: Empty constructor.
     * Required by Firebase Realtime Database to deserialize data from a DataSnapshot
     * into a Java object.
     */
    public PatientDetails() {
    }

    /**
     * Overloaded constructor used when a patient fills out the booking form
     * before pushing the data to the "appointments" or "DoctorSchedule" node.
     */
    public PatientDetails(String fullName, String email, String phone, String gender, String age, String purpose) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
        this.age = age;
        this.purpose = purpose;
    }
}