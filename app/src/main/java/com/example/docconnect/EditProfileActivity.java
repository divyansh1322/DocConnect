package com.example.docconnect;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * EditProfileActivity: Optimized A2Z Implementation.
 * FIXES: UI stutter by postponing transitions until data is mapped.
 * FIXES: Hardware renderer lag by delaying Firebase calls.
 */
public class EditProfileActivity extends AppCompatActivity {

    private ShapeableImageView imgProfile;
    private ImageButton btnBack;
    private MaterialCardView btnChangePhoto;
    private TextView btnSave, tvChangePhotoLabel, tvDob, tvGender;
    private TextInputEditText etFullName, etEmail, etPhone;
    private MaterialCardView layoutDob, layoutGender;
    private FrameLayout progressOverlay;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private String currentUserId;
    private Uri selectedImageUri = null;

    private static final String CLOUD_NAME = "dps6a4fvu";
    private static final String UPLOAD_PRESET = "ds132213";

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this)
                            .load(uri)
                            .apply(RequestOptions.circleCropTransform())
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(imgProfile);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // 1. Tell the Activity to wait before showing itself
        supportPostponeEnterTransition();

        if (!initFirebase()) return;
        initCloudinary();
        initViews();
        setupListeners();

        // 2. A2Z PERFORMANCE FIX:
        // Delay data loading by 300ms to let the transition finish.
        // This stops the "TransitionChain Mismatch" error.
        getWindow().getDecorView().postDelayed(this::loadUserData, 300);
    }

    private boolean initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            finish();
            return false;
        }
        currentUserId = mAuth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
        return true;
    }

    private void initCloudinary() {
        try {
            MediaManager.get();
        } catch (IllegalStateException e) {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", CLOUD_NAME);
            config.put("secure", true);
            MediaManager.init(this, config);
        }
    }

    private void initViews() {
        imgProfile = findViewById(R.id.imgProfile);
        btnBack = findViewById(R.id.btnBack);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnSave = findViewById(R.id.btnSave);
        tvChangePhotoLabel = findViewById(R.id.tvChangePhotoLabel);
        tvDob = findViewById(R.id.tvDob);
        tvGender = findViewById(R.id.tvGender);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        layoutDob = findViewById(R.id.layoutDob);
        layoutGender = findViewById(R.id.layoutGender);
        progressOverlay = findViewById(R.id.progressOverlay);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        View.OnClickListener pickImage = v -> imagePicker.launch("image/*");
        btnChangePhoto.setOnClickListener(pickImage);
        tvChangePhotoLabel.setOnClickListener(pickImage);
        layoutDob.setOnClickListener(v -> showDatePicker());
        layoutGender.setOnClickListener(v -> showGenderPicker());
        btnSave.setOnClickListener(v -> {
            if (validateInputs()) saveProfile();
        });
    }

    private void loadUserData() {
        // No need to show loading here if we postponed transition,
        // as the screen is invisible until supportStartPostponedEnterTransition()
        userRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists() || isFinishing()) {
                supportStartPostponedEnterTransition();
                return;
            }

            // DATA MAPPING
            etFullName.setText(getSafe(snapshot, "fullName"));
            etEmail.setText(getSafe(snapshot, "email"));
            etPhone.setText(getSafe(snapshot, "phone"));
            tvDob.setText(getSafe(snapshot, "dob"));
            tvGender.setText(getSafe(snapshot, "gender"));

            String photoUrl = snapshot.child("profilePhotoUrl").getValue(String.class);
            if (photoUrl != null && !photoUrl.isEmpty()) {
                Glide.with(this)
                        .load(photoUrl)
                        .apply(RequestOptions.circleCropTransform())
                        .into(imgProfile);
            }

            // 3. Now that data is mapped, show the activity
            supportStartPostponedEnterTransition();

        }).addOnFailureListener(e -> {
            supportStartPostponedEnterTransition();
        });
    }

    // Helper to prevent "null" text appearing in inputs
    private String getSafe(com.google.firebase.database.DataSnapshot ss, String key) {
        Object v = ss.child(key).getValue();
        return (v == null || v.toString().equals("null")) ? "" : v.toString();
    }

    private void saveProfile() {
        showLoading(true);
        if (selectedImageUri != null) {
            uploadImage(selectedImageUri);
        } else {
            userRef.get().addOnSuccessListener(snapshot -> {
                updateProfile(snapshot.child("profilePhotoUrl").getValue(String.class));
            });
        }
    }

    private void uploadImage(Uri uri) {
        MediaManager.get().upload(uri)
                .unsigned(UPLOAD_PRESET)
                .option("folder", "user_profiles")
                .callback(new UploadCallback() {
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        runOnUiThread(() -> updateProfile(imageUrl));
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(EditProfileActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long b, long t) {}
                    @Override public void onReschedule(String requestId, ErrorInfo e) {}
                }).dispatch();
    }

    private void updateProfile(String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", etFullName.getText().toString().trim());
        updates.put("email", etEmail.getText().toString().trim());
        updates.put("phone", etPhone.getText().toString().trim());
        updates.put("dob", tvDob.getText().toString().trim());
        updates.put("gender", tvGender.getText().toString().trim());
        if (imageUrl != null) updates.put("profilePhotoUrl", imageUrl);

        userRef.updateChildren(updates).addOnSuccessListener(unused -> {
            showLoading(false);
            Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            showLoading(false);
            Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show();
        });
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            tvDob.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showGenderPicker() {
        String[] genders = {"Male", "Female", "Other"};
        new AlertDialog.Builder(this).setTitle("Select Gender").setItems(genders, (d, w) -> tvGender.setText(genders[w])).show();
    }

    private boolean validateInputs() {
        if (etFullName.getText() == null || etFullName.getText().toString().trim().isEmpty()) {
            etFullName.setError("Name required");
            return false;
        }
        return true;
    }

    private void showLoading(boolean show) {
        if (progressOverlay != null) progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        if (btnSave != null) btnSave.setEnabled(!show);
    }
}