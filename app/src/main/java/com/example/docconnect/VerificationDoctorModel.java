package com.example.docconnect;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced Model for Doctor Verification.
 * Uses @IgnoreExtraProperties to prevent crashes if the database schema evolves.
 */
@IgnoreExtraProperties
public class VerificationDoctorModel implements Serializable {

    // --- Status Constants ---
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_VERIFIED = "verified";
    public static final String STATUS_REJECTED = "rejected";

    // Firebase Key (UID) - Excluded from JSON body
    @Exclude
    private String id;

    // Basic Info
    public String fullName;
    public String gender;
    public String dob;
    public String speciality;

    // Clinic Info
    public String clinicName;
    public String clinicAddress;

    // Credentials
    public String regNo;
    public String council;
    public String profileImageUrl;
    public String licenseImageUrl;

    // System Fields
    public String role = "doctor";
    public String status = STATUS_PENDING;

    // Timestamp handling (Server-side)
    private Object timestamp;

    /**
     * Required empty constructor for Firebase DataSnapshot.getValue()
     */
    public VerificationDoctorModel() {
        // Initialize timestamp with ServerValue for new objects
        this.timestamp = ServerValue.TIMESTAMP;
    }

    // --- ID Handling ---
    @Exclude
    public String getId() { return id; }

    @Exclude
    public void setId(String id) { this.id = id; }

    // --- Timestamp Logic ---
    public Object getTimestamp() {
        return timestamp;
    }

    @Exclude
    public long getTimestampLong() {
        if (timestamp instanceof Long) {
            return (long) timestamp;
        }
        return 0L;
    }

    // --- Getters and Setters (Standard Boilerplate) ---

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getSpeciality() { return speciality; }
    public void setSpeciality(String speciality) { this.speciality = speciality; }

    public String getClinicName() { return clinicName; }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }

    public String getClinicAddress() { return clinicAddress; }
    public void setClinicAddress(String clinicAddress) { this.clinicAddress = clinicAddress; }

    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) { this.regNo = regNo; }

    public String getCouncil() { return council; }
    public void setCouncil(String council) { this.council = council; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getLicenseImageUrl() { return licenseImageUrl; }
    public void setLicenseImageUrl(String licenseImageUrl) { this.licenseImageUrl = licenseImageUrl; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}