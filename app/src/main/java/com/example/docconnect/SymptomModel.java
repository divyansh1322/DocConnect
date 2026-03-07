package com.example.docconnect;

/**
 * Model class representing a medical symptom.
 * Used primarily in the search and category selection screens to help users
 * identify their health concerns through visual icons and titles.
 */
public class SymptomModel {

    // The name of the symptom (e.g., "Fever", "Headache", "Cough")
    private String title;

    // The resource ID for the drawable icon associated with the symptom
    // Note: Stored as int to match R.drawable.filename references
    private int image;

    /**
     * Primary constructor to initialize a symptom item.
     * * @param title The human-readable name of the symptom.
     * @param image The local drawable resource ID (e.g., R.drawable.img_fever).
     */
    public SymptomModel(String title, int image) {
        this.title = title;
        this.image = image;
    }

    /**
     * @return The title string used for display and search filtering.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return The integer resource ID for the icon.
     */
    public int getImage() {
        return image;
    }
}