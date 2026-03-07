package com.example.docconnect;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays a curated list of top doctor categories or highly-rated specialists.
 * Uses a 2-column Grid layout to present specialists in a clean, card-based interface.
 */
public class TopDoctorsActivity extends AppCompatActivity {

    // View Components for the list
    private RecyclerView recyclerViewTopDoctors;
    private TopDoctorsAdapter adapter;
    private List<TopDoctorsModel> doctorList;

    // Standard Header UI Components
    private ImageView btnBack;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_doctors);

        // 1. Initialize Views: Linking XML IDs to Java objects
        recyclerViewTopDoctors = findViewById(R.id.recyclerViewTopDoctors);
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);

        // 2. Setup RecyclerView (2-column Grid)
        // GridLayoutManager makes the items appear side-by-side rather than a vertical list
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerViewTopDoctors.setLayoutManager(gridLayoutManager);

        // 3. Initialize Data: Mock data population
        loadData();

        // 4. Set Adapter: Bridge between the data list and the UI components
        adapter = new TopDoctorsAdapter(this, doctorList);
        recyclerViewTopDoctors.setAdapter(adapter);

        // 5. Back Button Logic: Returns user to the previous screen
        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Hardcoded data loader.
     * In a production app, this would typically involve a Firebase query or an API call.
     */
    private void loadData() {
        doctorList = new ArrayList<>();

        // Populate the list with doctor specialty categories, counts, and images
        // Note: Images are referenced via local drawable resource IDs
        doctorList.add(new TopDoctorsModel("Skin Specialist", "665 Doctors", R.drawable.doctor4));
        doctorList.add(new TopDoctorsModel("Gynecologist", "452 Doctors", R.drawable.doctor5));
        doctorList.add(new TopDoctorsModel("Urologist", "Not Available", R.drawable.doctor4));
        doctorList.add(new TopDoctorsModel("Neurologist", "823 Doctors", R.drawable.doctor5));
        doctorList.add(new TopDoctorsModel("Dentist", "256 Doctors", R.drawable.doctor4));
        doctorList.add(new TopDoctorsModel("Cardiologist", "120 Doctors", R.drawable.doctor5));
    }
}