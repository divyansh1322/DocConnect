package com.example.docconnect;

import com.google.firebase.database.IgnoreExtraProperties;
import java.io.Serializable;

/**
 * Model class representing a user for administrative management.
 * Added @IgnoreExtraProperties to prevent ClassMapper warnings found in logs.
 */
@IgnoreExtraProperties
public class UserManageModel implements Serializable {

    private String userId;
    private String fullName;
    private String email;
    private String joinedDate;
    private String status;
    private String profilePhotoUrl;
    private String appointmentDate;

    // Required empty constructor
    public UserManageModel() {}

    // --- GETTERS ---
    public String getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getJoinedDate() { return joinedDate; }
    public String getStatus() { return status; }
    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public String getAppointmentDate() { return appointmentDate; }

    // --- SETTERS ---
    public void setUserId(String userId) { this.userId = userId; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setJoinedDate(String joinedDate) { this.joinedDate = joinedDate; }
    public void setStatus(String status) { this.status = status; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
    public void setAppointmentDate(String appointmentDate) { this.appointmentDate = appointmentDate; }
}