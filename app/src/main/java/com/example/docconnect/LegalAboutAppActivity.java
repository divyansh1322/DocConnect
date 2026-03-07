package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import androidx.appcompat.app.AppCompatActivity;

public class LegalAboutAppActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legalaboutapp); // Assuming this is your XML name

        // Back Button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // 1. Terms & Conditions Intent
        RelativeLayout btnTerms = findViewById(R.id.btn_terms);
        btnTerms.setOnClickListener(v -> {
            startActivity(new Intent(this, TermsActivity.class));
        });

        // 2. Privacy Policy Intent
        RelativeLayout btnPrivacy = findViewById(R.id.btn_privacy);
        btnPrivacy.setOnClickListener(v -> {
            startActivity(new Intent(this, PrivacyActivity.class));
        });

        //  3. Data Usage Intent
        RelativeLayout btnData = findViewById(R.id.btn_data);
        btnData.setOnClickListener(v -> {
            startActivity(new Intent(this, DataUsageActivity.class));
        });

        // 4. About Us Intent
        RelativeLayout btnAbout = findViewById(R.id.btn_about);
        btnAbout.setOnClickListener(v -> {
            startActivity(new Intent(this, AboutUsActivity.class));
        });
    }
}