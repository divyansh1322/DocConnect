package com.example.docconnect;

public class CategoryModel {

    public static final int TYPE_SPECIALTY = 1;
    public static final int TYPE_SYMPTOM = 2;
    public static final int TYPE_SURGERY = 3;

    private String name;
    private int iconResId;
    private int type;

    public CategoryModel(String name, int iconResId, int type) {
        this.name = name;
        this.iconResId = iconResId;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public int getIconResId() {
        return iconResId;
    }

    public int getType() {
        return type;
    }
}
