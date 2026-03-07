package com.example.docconnect;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * DoctorSupportActivity: Professional Implementation
 * Features: Internal/External Intent management, FAQ system, and crash-resilient communication.
 */
public class DoctorSupportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_support);

        // --- 1. Header & Navigation ---
        initHeader();

        // --- 2. Functional Support ---
        setupFunctionalSupport();

        // --- 3. Technical Support ---
        setupTechnicalSupport();

        // --- 4. FAQ Section ---
        setupFaqSection();

        // --- 5. Legal Section ---
        setupLegalSection();

        // --- 6. Email Support ---
        setupEmailFab();
    }

    private void initHeader() {
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupFunctionalSupport() {
        MaterialCardView cardCalendar = findViewById(R.id.card_calendar);
        if (cardCalendar != null) {
            cardCalendar.setOnClickListener(v -> {
                Intent intent = new Intent(this, SlotsAvailabilityActivity.class);
                startActivity(intent);
            });
        }

        MaterialCardView cardRecords = findViewById(R.id.card_records);
        if (cardRecords != null) {
            cardRecords.setOnClickListener(v -> {
                Intent intent = new Intent(this, PastConsultationsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupTechnicalSupport() {
        MaterialCardView cardBug = findViewById(R.id.card_bug_report);
        if (cardBug != null) {
            cardBug.setOnClickListener(v -> {
                Intent intent = new Intent(this, DoctorReportBugActivity.class);
                startActivity(intent);
            });
        }
    }

    /**
     * FAQ Section Implementation
     */

    private void setupFaqSection() {
        setFaqClickListener(R.id.card_faq_address, "Change Clinic Address",
                "To change your address, go to Profile -> Clinic Details -> Edit. \n\nNote: Major location changes may require re-verification.");

        setFaqClickListener(R.id.card_faq_reviews, "View Reviews",
                "You can see patient feedback in the 'Dashboard' tab under the 'Ratings' section.");

        setFaqClickListener(R.id.card_faq_license, "License Verification",
                "Upload your medical registration certificate in Profile -> Documents. Our team verifies this within 24-48 hours.");

        setFaqClickListener(R.id.card_faq_password, "Reset Password",
                "Log out of the app, then click 'Forgot Password?' on the login screen. A reset link will be sent to your email.");

        setFaqClickListener(R.id.card_faq_block, "Block Patient",
                "Yes. Go to your Appointment History, select the patient, tap the three dots and select 'Block Patient'.");
    }

    private void setFaqClickListener(int viewId, String title, String message) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(v -> showFaqDialog(title, message));
        }
    }

    private void showFaqDialog(String title, String message) {
        if (isFinishing()) return;

        new MaterialAlertDialogBuilder(this, R.style.RoundedAlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Got it", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Legal Section using Safe External Intents
     */

    private void setupLegalSection() {
        MaterialCardView cardLegal = findViewById(R.id.card_legal);
        if (cardLegal == null) return;

        cardLegal.setOnClickListener(v -> {
            String url = "https://youtube.google.com/app";
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                // Professional Guard: Ensure there is an app to handle this URL
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    startActivity(Intent.createChooser(intent, "Open with"));
                }
            } catch (Exception e) {
                Toast.makeText(this, "No browser found to open link.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Email FAB with strict Intent filters
     */
    private void setupEmailFab() {
        FloatingActionButton fab = findViewById(R.id.fab_support_email);
        if (fab == null) return;

        fab.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:")); // Only email apps should handle this

            String[] recipients = {"docconnect2213@gmail.com"};
            emailIntent.putExtra(Intent.EXTRA_EMAIL, recipients);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Doctor Support Request");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello DocConnect Team,\n\n[Issue Description]: ");

            try {
                // Professional Chooser for better UX
                startActivity(Intent.createChooser(emailIntent, "Contact Support via Email"));
            } catch (Exception e) {
                Toast.makeText(this, "No email client installed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Professional Cleanup here if any listeners or dialogs were persistent
    }
}