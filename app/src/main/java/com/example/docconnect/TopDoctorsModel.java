package com.example.docconnect;

/**
 * Model class representing a high-level doctor category or specialist.
 * This class encapsulates the data required to populate the Top Doctors grid,
 * including visual assets and current status information.
 */
public class TopDoctorsModel {

    // The specific medical field (e.g., "Cardiologist", "Dentist")
    private String speciality;

    // Status text (e.g., "Available", "Not Available", "452 Doctors")
    // Note: The Adapter uses this string to determine UI background colors (Green/Red)
    private String availability;

    // Local drawable resource ID (e.g., R.drawable.doctor4)
    private int imageResId;

    /**
     * Primary constructor to initialize a Top Doctor category.
     *
     * @param speciality   The medical specialty name.
     * @param availability The status text or count of doctors in this category.
     * @param imageResId   The integer resource ID for the category icon/image.
     */
    public TopDoctorsModel(String speciality, String availability, int imageResId) {
        this.speciality = speciality;
        this.availability = availability;
        this.imageResId = imageResId;
    }

    /**
     * @return The specialty title used in the card header.
     */
    public String getSpeciality() {
        return speciality;
    }

    /**
     * @return The availability status used for the badge text and conditional styling.
     */
    public String getAvailability() {
        return availability;
    }

    /**
     * @return The resource ID used by the adapter to set the ImageView content.
     */
    public int getImageResId() {
        return imageResId;
    }
}