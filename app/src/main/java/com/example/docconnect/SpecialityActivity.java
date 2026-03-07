package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays categorized medical information: Specialties, Symptoms, and Surgeries.
 * Each section uses a RecyclerView to allow users to quickly find doctors based on specific needs.
 */
public class SpecialityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speciality);

        // --- BACK NAVIGATION ---
        // Basic back button functionality to return to the previous screen
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        // --- UI INITIALIZATION ---
        // Three separate RecyclerViews for distinct medical categories
        RecyclerView rvSpecialties = findViewById(R.id.recyclerViewSpecialties);
        RecyclerView rvSymptoms = findViewById(R.id.recyclerViewSymptoms);
        RecyclerView rvSurgeries = findViewById(R.id.recyclerViewSurgeries);

        // --- 1. DATA PREPARATION: SPECIALTIES ---
        // Mapping general doctor categories like Dentists or Neurologists
        List<CategoryModel> specialties = new ArrayList<>();
        specialties.add(new CategoryModel(
                "Orthodontics",
                R.drawable.img_orthodontics,
                CategoryModel.TYPE_SPECIALTY));

        specialties.add(new CategoryModel(
                "Dermatologist",
                R.drawable.img_dermatologist,
                CategoryModel.TYPE_SPECIALTY));

        specialties.add(new CategoryModel(
                "Orthopedic",
                R.drawable.img_orthopedics,
                CategoryModel.TYPE_SPECIALTY));

        specialties.add(new CategoryModel(
                "Physio",
                R.drawable.img_physical,
                CategoryModel.TYPE_SPECIALTY));

        specialties.add(new CategoryModel(
                "Neurologist",
                R.drawable.img_neurologist,
                CategoryModel.TYPE_SPECIALTY));

        // --- 2. DATA PREPARATION: SYMPTOMS ---
        // Mapping common patient complaints to help them find general practitioners
        List<CategoryModel> symptoms = new ArrayList<>();
        symptoms.add(new CategoryModel(
                "Cough",
                R.drawable.img_cough,
                CategoryModel.TYPE_SYMPTOM));

        symptoms.add(new CategoryModel(
                "Fatigue",
                R.drawable.img_fatigue,
                CategoryModel.TYPE_SYMPTOM));

        symptoms.add(new CategoryModel(
                "Headache",
                R.drawable.img_headache,
                CategoryModel.TYPE_SYMPTOM));

        symptoms.add(new CategoryModel(
                "Fever",
                R.drawable.img_fever,
                CategoryModel.TYPE_SYMPTOM));

        symptoms.add(new CategoryModel(
                "Stomach Issue",
                R.drawable.img_stomach,
                CategoryModel.TYPE_SYMPTOM));

        // --- 3. DATA PREPARATION: SURGERIES ---
        // Mapping specific surgical procedures to find specialized surgeons
        List<CategoryModel> surgeries = new ArrayList<>();
        surgeries.add(new CategoryModel(
                "Hernia",
                R.drawable.img_hernia,
                CategoryModel.TYPE_SURGERY));

        surgeries.add(new CategoryModel(
                "Appendicitis",
                R.drawable.img_appendicitis,
                CategoryModel.TYPE_SURGERY));

        surgeries.add(new CategoryModel(
                "Fractuer",
                R.drawable.imgfractuer,
                CategoryModel.TYPE_SURGERY));

        surgeries.add(new CategoryModel(
                "Gallstones",
                R.drawable.img_gallstones,
                CategoryModel.TYPE_SURGERY));

        surgeries.add(new CategoryModel(
                "Heart ",
                R.drawable.img_heart,
                CategoryModel.TYPE_SURGERY));

        // --- ADAPTER CONFIGURATION ---
        // Reusing the same CategoryGridAdapter class for all three lists.
        // The lambda 'this::handleItemClick' acts as a unified click listener.

        CategoryGridAdapter specialtyAdapter =
                new CategoryGridAdapter(specialties, this::handleItemClick);

        CategoryGridAdapter symptomAdapter =
                new CategoryGridAdapter(symptoms, this::handleItemClick);

        CategoryGridAdapter surgeryAdapter =
                new CategoryGridAdapter(surgeries, this::handleItemClick);

        // Bind the adapters to their respective RecyclerViews
        rvSpecialties.setAdapter(specialtyAdapter);
        rvSymptoms.setAdapter(symptomAdapter);
        rvSurgeries.setAdapter(surgeryAdapter);
    }

    /**
     * Centralized click handler for all category items.
     * When a user clicks an item (like "Cough" or "Neurologist"),
     * this method passes the search query to the SearchResultsActivity.
     * * @param item The CategoryModel object that was clicked.
     */
    private void handleItemClick(CategoryModel item) {
        // Intent to move to search results
        Intent intent = new Intent(this, SearchResultsActivity.class);

        // Passing the item name as the search query (e.g., "Fever")
        intent.putExtra("SEARCH_QUERY", item.getName());

        // Passing the metadata (Symptom/Specialty/Surgery) to refine search logic
        intent.putExtra("CATEGORY_TYPE", item.getType());
        intent.putExtra("IS_CATEGORY_SEARCH", true);

        startActivity(intent);
    }
}