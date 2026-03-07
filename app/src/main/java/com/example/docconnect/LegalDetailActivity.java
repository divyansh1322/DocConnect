package com.example.docconnect;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * A reusable container activity designed to display legal documentation and external web pages.
 * It uses a WebView to load content dynamically based on the URL passed via Intents.
 */
public class LegalDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.legal_detail_activity);

        // 1. Toolbar Configuration
        // Providing a consistent navigation header for the legal pages.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable the standard "Up" navigation (Back Arrow)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Handle Toolbar navigation to return to the Legal/About menu
        toolbar.setNavigationOnClickListener(v -> finish());

        // 2. Intent Data Extraction
        // Fetching the dynamic title and URL passed from LegalAboutAppActivity
        String title = getIntent().getStringExtra("EXTRA_TITLE");
        String url = getIntent().getStringExtra("EXTRA_URL");

        // Dynamically update the app bar title to match the content (e.g., "Privacy Policy")
        if (title != null) {
            getSupportActionBar().setTitle(title);
        }

        // 3. WebView Implementation
        // Encapsulating web content within the native app frame
        WebView webView = findViewById(R.id.webView);

        // WebSettings: Configuring the WebView environment
        WebSettings webSettings = webView.getSettings();
        // Enables JavaScript execution which is often required for modern responsive pages
        webSettings.setJavaScriptEnabled(true);

        /**
         * WebViewClient Optimization:
         * This prevents the OS from launching a default browser (Chrome/Safari)
         * when a link inside the legal document is clicked, keeping the user in-app.
         */
        webView.setWebViewClient(new WebViewClient());

        // 4. Content Loading Logic
        if (url != null) {
            // Load the external remote URL
            webView.loadUrl(url);
        } else {
            /**
             * Fallback Strategy:
             * If the URL is missing for some reason, we generate an
             * 'on-the-fly' HTML string to inform the user.
             */
            String htmlData = "<html><body><h2>" + title + "</h2><p>Information not available.</p></body></html>";
            webView.loadData(htmlData, "text/html", "UTF-8");
        }
    }
}