package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText emailResetEt;
    private MaterialButton resetBtn;
    private TextView openGmailTv;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Views
        emailResetEt = findViewById(R.id.emailResetEt);
        resetBtn = findViewById(R.id.resetBtn);
        openGmailTv = findViewById(R.id.openGmailTv);
        progressBar = findViewById(R.id.resetProgressBar);

        // Back Button Logic
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        // Manual Reset Button Click
        resetBtn.setOnClickListener(v -> sendResetEmail());

        // Manual Gmail Text Click (Backup)
        openGmailTv.setOnClickListener(v -> openGmailApp());
    }

    private void sendResetEmail() {
        String email = emailResetEt.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailResetEt.setError("Required");
            return;
        }

        // UI Feedback: Show loading state
        progressBar.setVisibility(View.VISIBLE);
        resetBtn.setVisibility(View.INVISIBLE);

        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            resetBtn.setVisibility(View.VISIBLE);

            if (task.isSuccessful()) {
                android.util.Log.d("FORGOT_PW", "Reset email sent to: " + email);
                Toast.makeText(this, "Recovery link sent!", Toast.LENGTH_LONG).show();

                // Show the backup link
                openGmailTv.setVisibility(View.VISIBLE);

                // --- AUTOMATIC REDIRECT ---
                openGmailApp();
            } else {
                String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                android.util.Log.e("FORGOT_PW", "Firebase error: " + error);
                Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openGmailApp() {
        try {
            // Specifically target the Gmail App package
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.google.android.gm");

            if (intent != null) {
                // If Gmail exists, launch it
                startActivity(intent);
            } else {
                // If Gmail is not installed, open the general email selector
                Intent emailIntent = new Intent(Intent.ACTION_MAIN);
                emailIntent.addCategory(Intent.CATEGORY_APP_EMAIL);
                emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(Intent.createChooser(emailIntent, "Open Email App"));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not open email app automatically.", Toast.LENGTH_SHORT).show();
        }
    }
}