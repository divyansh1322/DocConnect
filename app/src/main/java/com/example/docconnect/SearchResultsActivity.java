package com.example.docconnect;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SearchResultsActivity extends AppCompatActivity {

    private RecyclerView rvDoctors;
    private EditText etSearch;
    private ProgressBar progressBar;
    private LinearLayout layoutNoData, layoutFrequent, layoutInitialState, chipContainer;
    private TextView tvResultsTitle;
    private ImageView btnBack;

    private SearchDoctorAdapter adapter;
    private final List<SearchDoctorModel> masterDoctorList = new ArrayList<>();

    private DatabaseReference dbRef;
    private FusedLocationProviderClient locationClient;
    private final CancellationTokenSource locationCts = new CancellationTokenSource();
    private double userLat = 0.0, userLng = 0.0;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        initUI();
        setupChips();

        dbRef = FirebaseDatabase.getInstance().getReference("doctors");
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        setupRecyclerView();
        checkLocationPermission();

        btnBack.setOnClickListener(v -> finish());
    }

    private void initUI() {
        rvDoctors = findViewById(R.id.rvDoctors);
        etSearch = findViewById(R.id.etSearch);
        progressBar = findViewById(R.id.progressBar);
        layoutNoData = findViewById(R.id.layoutNoData);
        layoutFrequent = findViewById(R.id.layoutFrequent);
        layoutInitialState = findViewById(R.id.layoutInitialState);
        chipContainer = findViewById(R.id.chipContainer);
        tvResultsTitle = findViewById(R.id.tvResultsTitle);
        btnBack = findViewById(R.id.btnBack);

        // Hide dynamic components initially
        rvDoctors.setVisibility(View.GONE);
        tvResultsTitle.setVisibility(View.GONE);
        layoutNoData.setVisibility(View.GONE);
    }

    private void setupChips() {
        String[] items = {"Dentist", "Neurologist", "Orthopedic", "Pediatrician", "Cardiologist", "Fever", "Cough"};
        chipContainer.removeAllViews();
        for (String item : items) {
            Chip chip = new Chip(this);
            chip.setText(item);
            chip.setChipBackgroundColorResource(R.color.doc_surface);
            chip.setTextColor(ContextCompat.getColor(this, R.color.doc_primary));
            chip.setChipStrokeWidth(0f);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(0, 0, 16, 0);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                etSearch.setText(item);
                etSearch.setSelection(item.length());
                performSearchFilter(item);
            });
            chipContainer.addView(chip);
        }
    }

    private void setupRecyclerView() {
        rvDoctors.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SearchDoctorAdapter(this, new ArrayList<>());
        rvDoctors.setAdapter(adapter);
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        } else {
            fetchLocationThenData();
        }
    }

    private void fetchLocationThenData() {
        String query = getIntent().getStringExtra("SEARCH_QUERY");
        boolean skipLoading = getIntent().getBooleanExtra("SKIP_LOADING", false);

        // CASE 1: Incoming Search Request (e.g. from Home Recycler/Categories)
        if (query != null && !query.isEmpty()) {
            etSearch.setText(query); // Fill the keyword first
            progressBar.setVisibility(View.VISIBLE); // Use progress bar while fetching
            layoutFrequent.setVisibility(View.GONE); // Hide cubes
            layoutInitialState.setVisibility(View.GONE); // Hide find doctor view
        }
        // CASE 2: User clicked the Search Bar (Instant Intent)
        else if (skipLoading) {
            progressBar.setVisibility(View.GONE);
            showInitialSearchState(); // Show cubes immediately
        }
        // DEFAULT: Just show loading
        else {
            progressBar.setVisibility(View.VISIBLE);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            loadDoctorsFromFirebase();
            return;
        }

        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, locationCts.getToken())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        userLat = task.getResult().getLatitude();
                        userLng = task.getResult().getLongitude();
                    }
                    loadDoctorsFromFirebase();
                });
    }

    private void loadDoctorsFromFirebase() {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                masterDoctorList.clear();
                for (DataSnapshot docSnap : snapshot.getChildren()) {
                    SearchDoctorModel doctor = docSnap.getValue(SearchDoctorModel.class);
                    if (doctor != null && doctor.getStatus() != null &&
                            doctor.getStatus().matches("(?i)verified|approved")) {

                        doctor.setId(docSnap.getKey());

                        // --- RATING CALCULATION ---
                        DataSnapshot ratingsNode = docSnap.child("reviews_ratings");
                        if (ratingsNode.exists()) {
                            long count = ratingsNode.getChildrenCount();
                            double sum = 0.0;
                            for (DataSnapshot r : ratingsNode.getChildren()) {
                                Double val = r.child("rating").getValue(Double.class);
                                if (val != null) sum += val;
                            }
                            doctor.setRatings(String.format(Locale.getDefault(), "%.1f", sum / count));
                            doctor.setReviewCount(String.valueOf(count));
                        } else {
                            doctor.setRatings("0.0");
                            doctor.setReviewCount("0");
                        }

                        // --- DISTANCE CALCULATION ---
                        if (userLat != 0.0 && doctor.getClinicLat() != 0) {
                            float[] distRes = new float[1];
                            Location.distanceBetween(userLat, userLng, doctor.getClinicLat(), doctor.getClinicLng(), distRes);
                            doctor.setDistance(distRes[0] / 1000f);
                        }
                        masterDoctorList.add(doctor);
                    }
                }

                Collections.sort(masterDoctorList, (d1, d2) -> Float.compare(d1.getDistance(), d2.getDistance()));

                progressBar.setVisibility(View.GONE);
                handleInitialSearchIntent();
                setupSearchInputListener();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void handleInitialSearchIntent() {
        String query = getIntent().getStringExtra("SEARCH_QUERY");
        if (query != null && !query.isEmpty()) {
            performSearchFilter(query.trim());
        } else {
            showInitialSearchState();
        }
    }

    private void showInitialSearchState() {
        layoutFrequent.setVisibility(View.VISIBLE);
        layoutInitialState.setVisibility(View.VISIBLE);
        rvDoctors.setVisibility(View.GONE);
        tvResultsTitle.setVisibility(View.GONE);
        layoutNoData.setVisibility(View.GONE);
    }

    private void setupSearchInputListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> performSearchFilter(s.toString().trim());
                searchHandler.postDelayed(searchRunnable, 300);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearchFilter(String query) {
        if (query.isEmpty()) {
            showInitialSearchState();
            return;
        }

        layoutFrequent.setVisibility(View.GONE);
        layoutInitialState.setVisibility(View.GONE);

        List<SearchDoctorModel> filtered = new ArrayList<>();
        for (SearchDoctorModel d : masterDoctorList) {
            if (d.getFullName().toLowerCase().contains(query.toLowerCase()) ||
                    d.getSpeciality().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(d);
            }
        }
        updateUI(filtered);
    }

    private void updateUI(List<SearchDoctorModel> list) {
        if (list.isEmpty()) {
            rvDoctors.setVisibility(View.GONE);
            tvResultsTitle.setVisibility(View.GONE);
            layoutNoData.setVisibility(View.VISIBLE);
        } else {
            layoutNoData.setVisibility(View.GONE);
            tvResultsTitle.setVisibility(View.VISIBLE);
            rvDoctors.setVisibility(View.VISIBLE);
            adapter.filterList(list);
        }
    }

    @Override
    protected void onDestroy() {
        locationCts.cancel();
        searchHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}