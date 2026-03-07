package com.example.docconnect;

/**
 * Data model representing a patient review for a doctor.
 * This class is designed for Firebase Realtime Database mapping,
 * supporting both general ratings and specific medical service metrics.
 */
public class DoctorReviewsModel {

    // Unique identifiers and patient info
    private String reviewId;
    private String patientId;
    private String patientName; // Displays the name of the patient who wrote the review
    private String date;        // Formatted date string (e.g., "Feb 14, 2026")

    // Granular rating metrics for advanced analytics
    private float rating;         // The overall star rating
    private float communication;  // Rating for how well the doctor explained things
    private float punActuality;   // Rating for the doctor's punctuality
    private float bedsideManner;  // Rating for the doctor's professionalism/empathy

    // Detailed text feedback
    private String feedback;

    /**
     * Required empty constructor for Firebase Realtime Database.
     * Firebase uses this to create an instance before populating fields.
     */
    public DoctorReviewsModel() {
        // Required for Firebase
    }

    /**
     * Overloaded constructor to manually create a review object.
     */
    public DoctorReviewsModel(String reviewId, String patientId, String patientName, String date,
                              float rating, float communication, float punActuality,
                              float bedsideManner, String feedback) {
        this.reviewId = reviewId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.date = date;
        this.rating = rating;
        this.communication = communication;
        this.punActuality = punActuality;
        this.bedsideManner = bedsideManner;
        this.feedback = feedback;
    }

    // --- Getters ---
    // These are used by the RecyclerView Adapter and Firebase to access data

    public String getReviewId() { return reviewId; }
    public String getPatientId() { return patientId; }
    public String getPatientName() { return patientName; }
    public String getDate() { return date; }
    public float getRating() { return rating; }
    public float getCommunication() { return communication; }
    public float getPunActuality() { return punActuality; }
    public float getBedsideManner() { return bedsideManner; }
    public String getFeedback() { return feedback; }
}