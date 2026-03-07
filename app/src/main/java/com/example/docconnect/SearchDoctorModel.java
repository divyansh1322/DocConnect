package com.example.docconnect;

import java.util.Locale;

/**
 * Model class for Doctor Search.
 * Includes explicit fields for calculated Ratings and Review Counts.
 */
public class SearchDoctorModel {
    private String id;
    private String fullName;
    private String speciality;
    private String clinicName;
    private String clinicAddress;
    private String profileImageUrl;
    private String status;
    private Object ratings; // Can be a Map from Firebase or a String after calculation
    private String reviewCount = "0"; // Explicitly set via Activity
    private Object experience;
    private Object totalPatients;
    private Object consultationFees;
    private String bio;
    private double clinicLat;
    private double clinicLng;
    private float distance;

    // Required empty constructor for Firebase
    public SearchDoctorModel() {}

    // --- Calculated Data Accessors ---

    public String getAverageRating() {
        if (ratings == null) return "0.0";
        // If we already set it as a String in the Activity
        if (ratings instanceof String) return (String) ratings;
        return "0.0";
    }

    public String getReviewCount() {
        return (reviewCount == null) ? "0" : reviewCount;
    }

    public void setReviewCount(String reviewCount) {
        this.reviewCount = reviewCount;
    }

    // --- Standard Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getSpeciality() { return speciality; }
    public void setSpeciality(String speciality) { this.speciality = speciality; }
    public String getClinicName() { return clinicName; }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }
    public String getClinicAddress() { return clinicAddress; }
    public void setClinicAddress(String clinicAddress) { this.clinicAddress = clinicAddress; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Object getRatings() { return ratings; }
    public void setRatings(Object ratings) { this.ratings = ratings; }
    public String getExperience() { return String.valueOf(experience != null ? experience : "0"); }
    public void setExperience(Object experience) { this.experience = experience; }
    public String getTotalPatients() { return String.valueOf(totalPatients != null ? totalPatients : "0"); }
    public void setTotalPatients(Object totalPatients) { this.totalPatients = totalPatients; }
    public String getConsultationFees() { return String.valueOf(consultationFees != null ? consultationFees : "0"); }
    public void setConsultationFees(Object consultationFees) { this.consultationFees = consultationFees; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public double getClinicLat() { return clinicLat; }
    public void setClinicLat(double clinicLat) { this.clinicLat = clinicLat; }
    public double getClinicLng() { return clinicLng; }
    public void setClinicLng(double clinicLng) { this.clinicLng = clinicLng; }
    public float getDistance() { return distance; }
    public void setDistance(float distance) { this.distance = distance; }
}