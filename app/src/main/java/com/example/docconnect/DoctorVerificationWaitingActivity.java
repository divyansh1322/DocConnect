package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

/**
 * This Activity is shown to doctors after they submit their verification documents.
 * It informs them that their account is under review and restricts access to the main app.
 */
public class DoctorVerificationWaitingActivity extends AppCompatActivity {

    private MaterialButton btnBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_verification_waiting);

        btnBackToLogin = findViewById(R.id.btnBackToLogin);

        // Handle the transition back to the entry point of the app
        btnBackToLogin.setOnClickListener(v -> {
            // 1. Sign out the user locally.
            // This is crucial because if they aren't signed out, the Splash screen
            // might auto-login them back to this "Waiting" screen.
            FirebaseAuth.getInstance().signOut();

            // 2. Prepare to navigate back to the Login screen
            Intent intent = new Intent(DoctorVerificationWaitingActivity.this, DoctorLoginActivity.class);

            // 3. FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK:
            // This clears the entire "Back Stack". It ensures that if the user clicks
            // the hardware back button from the Login screen, they don't land back here.
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);

            // Close this activity
            finish();
        });
    }

    /**
     * Overriding the system back button.
     * We sign the user out here as well to maintain security and state consistency.
     */
    @Override
    public void onBackPressed() {
        // Sign out before allowing the user to exit the screen via hardware button
        FirebaseAuth.getInstance().signOut();
        super.onBackPressed();
    }
}