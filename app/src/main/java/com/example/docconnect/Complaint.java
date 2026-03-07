package com.example.docconnect;

/**
 * Model class representing a user support ticket or complaint.
 * Used for storing and retrieving data from Firebase Realtime Database.
 */
public class Complaint {

    // Unique identifier for the complaint entry
    private String id;
    // ID of the user who filed the complaint
    private String userId;
    // Name of the user for display in the admin panel
    private String fullName;
    private String email;
    private String phone;
    // The type of issue (e.g., App Bug, Billing, Doctor Behavior)
    private String category;
    // Detailed explanation of the user's issue
    private String description;
    // The date and time when the complaint was submitted
    private String dateTime;
    // Current state of the ticket (e.g., PENDING, RESOLVED, IN_PROGRESS)
    private String status;
    // Optional: URL to a screenshot provided by the user
    private String imageUrl;
    // Optional: User's profile photo for identity verification
    private String profilePhotoUrl;

    /**
     * Required empty constructor for Firebase Realtime Database.
     * Needed to map database snapshots directly to this Java object.
     */
    public Complaint() {}

    // --- Getters and Setters ---
    // These allow Firebase and other activities to read and update the data fields.

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }

    /**
     * Updates the profile photo URL.
     * @param profilePhotoUrl The web link to the image.
     */
    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}