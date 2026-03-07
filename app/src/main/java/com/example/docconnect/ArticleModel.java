package com.example.docconnect;

/**
 * ARTICLE MODEL
 * A simple POJO (Plain Old Java Object) used to represent health articles.
 * This class stores both display metadata (title, image) and functional data (URL).
 */
public class ArticleModel {
    private String title;
    private String category;
    private String date;
    private int imageResId;   // Local drawable resource ID (e.g., R.drawable.image)
    private String articleUrl; // Remote web address for the full article

    // --- CONSTRUCTOR ---
    /**
     * Initializes a new Article object.
     * @param articleUrl The destination link used by the Intent in the Adapter.
     */
    public ArticleModel(String title, String category, String date, int imageResId, String articleUrl) {
        this.title = title;
        this.category = category;
        this.date = date;
        this.imageResId = imageResId;
        this.articleUrl = articleUrl;
    }

    // --- GETTERS ---
    // These methods allow the Adapter to extract data and bind it to the XML views.

    /**
     * @return The web link to be opened in the browser.
     */
    public String getArticleUrl() {
        return articleUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getDate() {
        return date;
    }

    /**
     * @return The integer reference to the drawable resource.
     */
    public int getImageResId() {
        return imageResId;
    }
}