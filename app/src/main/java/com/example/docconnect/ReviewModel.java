package com.example.docconnect;

import com.google.firebase.database.IgnoreExtraProperties;
import java.io.Serializable;

/**
 * ReviewModel: Finalized for DocConnect 2026.
 * Includes all identification, performance metrics, and metadata.
 */
@IgnoreExtraProperties
public class ReviewModel implements Serializable {

    // --- IDENTIFIERS ---
    public String id;
    public String patientId;
    public String patientName;
    public String patientImage;

    // --- PERFORMANCE METRICS ---
    public float communication;
    public float punctuality;
    public float bedsideManner;
    public float rating;

    // --- TEXTUAL FEEDBACK ---
    public String feedback;

    // --- METADATA ---
    public long timestamp;
    public String date;

    /**
     * MANDATORY: Empty constructor for Firebase Data Mapping.
     */
    public ReviewModel() { }

    /**
     * Overloaded constructor (Existing logic preserved).
     */
    public ReviewModel(String id, float communication, float punctuality, float bedsideManner, String feedback) {
        this.id = id;
        this.communication = communication;
        this.punctuality = punctuality;
        this.bedsideManner = bedsideManner;
        this.feedback = feedback;
        this.timestamp = System.currentTimeMillis();
    }

    // --- GETTERS & SETTERS (Crucial for Data Mapping) ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName != null ? patientName : "Anonymous"; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientImage() { return patientImage; }
    public void setPatientImage(String patientImage) { this.patientImage = patientImage; }

    public float getCommunication() { return communication; }
    public void setCommunication(float communication) { this.communication = communication; }

    public float getPunctuality() { return punctuality; }
    public void setPunctuality(float punctuality) { this.punctuality = punctuality; }

    public float getBedsideManner() { return bedsideManner; }
    public void setBedsideManner(float bedsideManner) { this.bedsideManner = bedsideManner; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}