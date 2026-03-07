package com.example.docconnect;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * AddFamilyMemberActivity: A2Z Professional Implementation using HashMaps.
 * BENEFITS: Faster network updates and atomic field control.
 * FIXES: Transition Mismatch by syncing with HardwareRenderer via postDelayed.
 */
public class AddFamilyMemberActivity extends AppCompatActivity {

    private static final String CLOUD_NAME = "dps6a4fvu";
    private static final String UPLOAD_PRESET = "ds132213";
    private static final String CLOUDINARY_FOLDER = "family_images";

    private ImageView imgProfile;
    private AutoCompleteTextView dropdownRelationship, dropdownGender;
    private TextInputEditText etFullName, etDob, etMobile;
    private MaterialButton btnSave;
    private CardView btnEditImage;
    private ProgressDialog pd;

    private Uri selectedImageUri = null;
    private String existingImageUrl = "";
    private String memberId = null;

    private DatabaseReference mDatabase;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_family_members);

        // 1. SERVICES INIT
        initCloudinary();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupDropdowns();
        setupListeners();

        // 2. A2Z PERFORMANCE SYNC
        // We wait 300ms for Activity transition to finish to prevent UI "Hanging".
        getWindow().getDecorView().postDelayed(() -> {
            if (!isFinishing()) {
                checkIntentData();
            }
        }, 300);
    }

    private void checkIntentData() {
        if (getIntent().hasExtra("MEMBER_ID")) {
            memberId = getIntent().getStringExtra("MEMBER_ID");
            loadMemberData();
            btnSave.setText("Update Member");
        }
    }

    private void loadMemberData() {
        String uid = auth.getUid();
        if (uid == null || memberId == null) return;

        mDatabase.child("users").child(uid).child("add_family_members").child(memberId)
                .get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists() && !isFinishing()) {
                        // Using Map to read data (A2Z Recommendation)
                        Map<String, Object> data = (Map<String, Object>) snapshot.getValue();
                        if (data != null) {
                            etFullName.setText((String) data.get("fullName"));
                            etDob.setText((String) data.get("dob"));
                            etMobile.setText((String) data.get("mobile"));
                            dropdownRelationship.setText((String) data.get("relationship"), false);
                            dropdownGender.setText((String) data.get("gender"), false);
                            existingImageUrl = (String) data.get("imageUrl");

                            if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
                                Glide.with(this).load(existingImageUrl).circleCrop().into(imgProfile);
                            }
                        }
                    }
                });
    }

    private void validateAndSave() {
        String name = etFullName.getText().toString().trim();
        String mob = etMobile.getText().toString().trim();

        if (name.isEmpty()) { etFullName.setError("Required"); return; }
        if (mob.length() < 10) { etMobile.setError("Invalid Mobile"); return; }

        showProgress("Saving to Cloud...");

        if (selectedImageUri != null) {
            uploadToCloudinary();
        } else {
            saveWithHashMap(existingImageUrl);
        }
    }

    /**
     * saveWithHashMap: The A2Z logic for Atomic Updates.
     * Logic: We only push a Map to Firebase, which is more efficient than pushing an object.
     */
    private void saveWithHashMap(String imageUrl) {
        String uid = auth.getUid();
        if (uid == null) { dismissProgress(); return; }

        if (memberId == null) {
            memberId = mDatabase.child("users").child(uid).child("add_family_members").push().getKey();
        }

        // --- HASHMAP DATA CONSTRUCTION ---
        Map<String, Object> memberData = new HashMap<>();
        memberData.put("id", memberId);
        memberData.put("fullName", etFullName.getText().toString().trim());
        memberData.put("relationship", dropdownRelationship.getText().toString());
        memberData.put("gender", dropdownGender.getText().toString());
        memberData.put("dob", etDob.getText().toString().trim());
        memberData.put("mobile", etMobile.getText().toString().trim());
        memberData.put("imageUrl", imageUrl);
        memberData.put("updatedAt", System.currentTimeMillis());

        // Performing the Atomic Update
        mDatabase.child("users").child(uid).child("add_family_members").child(memberId)
                .updateChildren(memberData)
                .addOnSuccessListener(aVoid -> {
                    dismissProgress();
                    Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    dismissProgress();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadToCloudinary() {
        MediaManager.get().upload(selectedImageUri)
                .unsigned(UPLOAD_PRESET)
                .option("folder", CLOUDINARY_FOLDER)
                .callback(new UploadCallback() {
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String newUrl = (String) resultData.get("secure_url");
                        runOnUiThread(() -> saveWithHashMap(newUrl));
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> {
                            dismissProgress();
                            Toast.makeText(AddFamilyMemberActivity.this, "Upload Fail", Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long b, long t) {}
                    @Override public void onReschedule(String requestId, ErrorInfo e) {}
                }).dispatch();
    }

    // --- VIEW & UI HELPERS ---

    private void initViews() {
        imgProfile = findViewById(R.id.img_avatar_view);
        btnEditImage = findViewById(R.id.cv_edit_icon);
        dropdownRelationship = findViewById(R.id.actv_relationship);
        dropdownGender = findViewById(R.id.actv_gender);
        etFullName = (TextInputEditText) ((TextInputLayout) findViewById(R.id.til_name)).getEditText();
        etDob = (TextInputEditText) ((TextInputLayout) findViewById(R.id.til_dob)).getEditText();
        etMobile = (TextInputEditText) ((TextInputLayout) findViewById(R.id.til_mobile)).getEditText();
        btnSave = findViewById(R.id.btn_save);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void setupDropdowns() {
        String[] rels = {"Father", "Mother", "Spouse", "Son", "Daughter", "Sibling", "Other"};
        dropdownRelationship.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, rels));
        String[] gens = {"Male", "Female", "Other"};
        dropdownGender.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, gens));
    }

    private void setupListeners() {
        etDob.setFocusable(false);
        etDob.setOnClickListener(v -> showDatePicker());
        btnEditImage.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).circleCrop().into(imgProfile);
                }
            }
    );

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            etDob.setText(day + "/" + (month + 1) + "/" + year);
        }, 2000, 0, 1).show();
    }

    private void initCloudinary() {
        try { MediaManager.get(); } catch (Exception e) {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", CLOUD_NAME); config.put("secure", true);
            MediaManager.init(this, config);
        }
    }

    private void showProgress(String msg) {
        pd = new ProgressDialog(this);
        pd.setMessage(msg);
        pd.setCancelable(false);
        pd.show();
    }

    private void dismissProgress() {
        if (pd != null && pd.isShowing()) pd.dismiss();
    }

    @Override
    protected void onDestroy() {
        dismissProgress();
        super.onDestroy();
    }
}