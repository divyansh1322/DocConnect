package com.example.docconnect;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

/**
 * Activity designed to display medical licenses or verification documents in full screen.
 * It features pinch-to-zoom capabilities via the PhotoView library to allow
 * detailed inspection of document text and seals.
 */
public class LicenseDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license_detail);

        // PhotoView is a specialized ImageView that supports zooming and panning
        PhotoView photoView = findViewById(R.id.photoView);
        ImageButton btnClose = findViewById(R.id.btnClose);

        // Retrieve the image URL passed from the previous activity (e.g., DoctorProfileActivity)
        String imageUrl = getIntent().getStringExtra("licenseUrl");

        /**
         * Glide Implementation:
         * Loads the remote image into the PhotoView.
         * ic_verify is used as a placeholder to signal the verification context.
         */
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_verify)
                .into(photoView);

        // Standard close logic to return to the doctor's profile
        btnClose.setOnClickListener(v -> finish());
    }
}