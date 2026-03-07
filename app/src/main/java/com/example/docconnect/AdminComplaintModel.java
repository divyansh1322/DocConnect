package com.example.docconnect;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Enhanced Model class for Admin Complaints.
 * Capable of handling both User and Doctor complaint nodes.
 */
@IgnoreExtraProperties
public class AdminComplaintModel {

    // Unique Identifiers
    private String ticketId;
    private String userId;

    // Path Tracking (Crucial for AdminUDComplaintActivity)
    private String userType; // Stores "users" or "doctors"
    private String nodeKey;  // Stores "report_issue" or "support_tickets"

    // Content & Media
    private String fullName;
    private String profileImageUrl;
    private String evidenceUrl;
    private String category;
    private String appointmentId;
    private String description;
    private String date;
    private String status;

    // Required empty constructor for Firebase
    public AdminComplaintModel() {}

    // --- Identification Getters/Setters ---
    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    // --- Dynamic Path Handling ---
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getNodeKey() { return nodeKey; }
    public void setNodeKey(String nodeKey) { this.nodeKey = nodeKey; }

    // --- Display Info ---
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }

    // --- Ticket Details ---
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}