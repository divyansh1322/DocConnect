package com.example.docconnect;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * DoctorClinicLocationActivity: A2Z Complete Implementation.
 * FIXES: "TransitionChain Mismatch" by using delayed startup logic.
 * FIXES: "MIUI NullPointer" via lifecycle-aware UI checks.
 * FEATURES: GPS Auto-Location, Reverse Geocoding, and Cloudinary Image Management.
 */
public class DoctorClinicLocationActivity extends AppCompatActivity implements ClinicImageAdapter.OnImageClickListener {

    private static final String TAG = "ClinicLocation";

    // --- UI ELEMENTS ---
    private ImageView btnBack;
    private TextView tvClinicName;
    private TextInputEditText etAddress;
    private TextInputLayout tilAddress;
    private MaterialCardView btnAddPhoto;
    private MaterialButton btnSave;
    private RecyclerView rvClinicImages;

    // --- ADAPTERS & LISTS ---
    private ClinicImageAdapter adapter;
    private List<ClinicImageModel> imageList;
    private ProgressDialog progressDialog;

    // --- FIREBASE REFERENCES ---
    private FirebaseAuth mAuth;
    private DatabaseReference clinicRef;
    private DatabaseReference imagesRef;

    // --- CONSTANTS ---
    private FusedLocationProviderClient fusedLocationClient;
    private static final int IMAGE_REQ = 101;
    private static final int LOC_REQ = 102;
    private static final String CLOUDINARY_PRESET = "ds132213";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_clinic_location);

        // 1. SESSION GUARD & FIREBASE INIT
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            clinicRef = FirebaseDatabase.getInstance().getReference("doctors").child(uid);
            imagesRef = clinicRef.child("clinicImages");
        } else {
            Toast.makeText(this, "Session Expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        supportPostponeEnterTransition();
        // 2. VIEW INITIALIZATION
        initViews();
        setupRecyclerView();

        // 3. SAFE STARTUP (A2Z FIX)
        // Delaying heavy database/UI logic by 300ms prevents the "Transition Record Mismatch"
        // error in system_server by letting the "OPEN" animation finish first.
        getWindow().getDecorView().postDelayed(() -> {
            if (!isFinishing()) {
                loadClinicDetails();
                loadClinicImages();
            }
        }, 300);

        // 4. LISTENERS
        btnBack.setOnClickListener(v -> finish());
        btnAddPhoto.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> saveClinicData());
        tilAddress.setEndIconOnClickListener(v -> checkPermissionAndGetLocation());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvClinicName = findViewById(R.id.tvClinicName);
        etAddress = findViewById(R.id.etAddress);
        tilAddress = findViewById(R.id.tilAddress);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        btnSave = findViewById(R.id.btnSave);
        rvClinicImages = findViewById(R.id.rvClinicImages);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
    }

    private void setupRecyclerView() {
        imageList = new ArrayList<>();
        adapter = new ClinicImageAdapter(imageList, this);
        rvClinicImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvClinicImages.setAdapter(adapter);
    }

    // --- LOCATION LOGIC ---

    private void checkPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOC_REQ);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (isFinishing()) return;
        progressDialog.setMessage("Fetching GPS location...");
        progressDialog.show();

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            reverseGeocode(location.getLatitude(), location.getLongitude());
                        } else {
                            dismissProgress();
                            Toast.makeText(this, "GPS Signal Weak. Turn on Location.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        dismissProgress();
                        Toast.makeText(this, "Location Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (SecurityException e) {
            dismissProgress();
        }
    }

    /**
     * Converts Coordinates into a human-readable address.
     */
    private void reverseGeocode(double lat, double lng) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty() && !isFinishing()) {
                    String fullAddress = addresses.get(0).getAddressLine(0);
                    runOnUiThread(() -> {
                        etAddress.setText(fullAddress);
                        dismissProgress();
                    });
                }
            } catch (IOException e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    Toast.makeText(this, "Geocoding Failed", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // --- DATA MANAGEMENT ---

    private void saveClinicData() {
        String addressText = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        if (addressText.isEmpty()) {
            etAddress.setError("Address is required");
            return;
        }

        showProgress("Saving clinic details...");

        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            double lat = 0.0, lng = 0.0;
            boolean found = false;
            try {
                List<Address> addresses = geocoder.getFromLocationName(addressText, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    lat = addresses.get(0).getLatitude();
                    lng = addresses.get(0).getLongitude();
                    found = true;
                }
            } catch (IOException ignored) {}

            final double fLat = lat; final double fLng = lng; final boolean fFound = found;
            runOnUiThread(() -> {
                if (isFinishing()) return;

                Map<String, Object> map = new HashMap<>();
                map.put("clinicAddress", addressText);
                if (fFound) {
                    map.put("clinicLat", fLat);
                    map.put("clinicLng", fLng);
                }

                clinicRef.updateChildren(map).addOnCompleteListener(task -> {
                    dismissProgress();
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Clinic Saved!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            });
        }).start();
    }

    private void loadClinicDetails() {
        if (clinicRef == null) return;
        clinicRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isFinishing() && snapshot.exists()) {
                    tvClinicName.setText(String.valueOf(snapshot.child("clinicName").getValue()));
                    etAddress.setText(String.valueOf(snapshot.child("clinicAddress").getValue()));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadClinicImages() {
        if (imagesRef == null) return;
        imagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;
                imageList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    imageList.add(new ClinicImageModel(ds.getKey(), ds.getValue(String.class)));
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- IMAGE PICKING & CLOUDINARY ---

    private void openGallery() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        startActivityForResult(Intent.createChooser(i, "Select Image"), IMAGE_REQ);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && requestCode == IMAGE_REQ) {
            uploadToCloudinary(data.getData());
        }
    }

    private void uploadToCloudinary(Uri uri) {
        showProgress("Uploading photo...");

        MediaManager.get().upload(uri).unsigned(CLOUDINARY_PRESET).option("folder", "clinic_photos")
                .callback(new UploadCallback() {
                    @Override public void onSuccess(String requestId, Map resultData) {
                        if (isFinishing()) return;
                        String url = (String) resultData.get("secure_url");
                        imagesRef.push().setValue(url).addOnCompleteListener(t -> dismissProgress());
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        dismissProgress();
                        Toast.makeText(DoctorClinicLocationActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long b, long t) {}
                    @Override public void onReschedule(String requestId, ErrorInfo e) {}
                }).dispatch();
    }

    // --- INTERFACE IMPLEMENTATIONS ---

    @Override public void onDeleteClick(ClinicImageModel model) {
        if (imagesRef != null) {
            imagesRef.child(model.getKey()).removeValue();
        }
    }

    @Override public void onImageClick(String url) {
        if (isFinishing()) return;
        Dialog d = new Dialog(this);
        ImageView iv = new ImageView(this);
        Glide.with(this).load(url).into(iv);
        d.setContentView(iv);
        Window window = d.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        d.show();
    }

    // --- LIFECYCLE & UTILITIES ---

    private void showProgress(String message) {
        if (!isFinishing() && progressDialog != null) {
            progressDialog.setMessage(message);
            progressDialog.show();
        }
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        // PREVENTS: WindowLeaked Exception during MIUI system property checks
        dismissProgress();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOC_REQ && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }
}