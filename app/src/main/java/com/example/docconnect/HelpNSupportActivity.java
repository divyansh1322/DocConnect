package com.example.docconnect;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * HelpNSupportActivity
 * Logic: Streamlined hub for support using Direct Dial and Ticket reporting.
 */
public class HelpNSupportActivity extends AppCompatActivity {

    // UI Components
    private ImageButton btnBack;
    private LinearLayout btnCallSupport, btnFaq, btnRaiseTicket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_n_support);

        initViews();
        setupListeners();
    }

    private void initViews() {
        // Changed to ImageButton to match the XML's actual View type
        btnBack = findViewById(R.id.btnBack);

        // Match the IDs exactly from the refined XML
        btnCallSupport = findViewById(R.id.btnCallSupport);
        btnFaq = findViewById(R.id.btnFaq);
        btnRaiseTicket = findViewById(R.id.btnRaiseTicket);
    }

    private void setupListeners() {
        // Back Navigation
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 1. Call Support Logic - Using your specific number
        if (btnCallSupport != null) {
            btnCallSupport.setOnClickListener(v -> {
                String supportNumber = "+918556059172";
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + supportNumber));
                startActivity(intent);
            });
        }

        // 2. Open FAQs Activity
        if (btnFaq != null) {
            btnFaq.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, FAQsActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    showToast("FAQ section is currently unavailable.");
                }
            });
        }

        // 3. Raise Ticket / Report Issue Logic
        if (btnRaiseTicket != null) {
            btnRaiseTicket.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, ReportIssueActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    showToast("Unable to open report form.");
                }
            });
        }
    }

    /**
     * Optional WhatsApp Support Logic
     * If you decide to add a WhatsApp button later, this is ready to go.
     */
    private void openWhatsAppSupport() {
        String number = "918556059172"; // Country code + number without '+'
        String message = "Hello, I need assistance with DocConnect.";
        String url = "https://api.whatsapp.com/send?phone=" + number + "&text=" + Uri.encode(message);

        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            i.setPackage("com.whatsapp");
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            // Fallback: Open in Browser if WhatsApp is not installed
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(i);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}