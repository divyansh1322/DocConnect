package com.example.docconnect;

public class TermModel {
    private String title;
    private String content;
    private boolean isExpanded; // Tracks the state of the accordion

    public TermModel(String title, String content) {
        this.title = title;
        this.content = content;
        this.isExpanded = false; // Closed by default
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
}