package com.example.docconnect;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * ArticleActivity: Professional Implementation
 * Displays curated health content with optimized scrolling and intent-safe navigation.
 */
public class ArticleActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArticleAdapter adapter;
    private List<ArticleModel> articleList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        // --- 1. UI NAVIGATION ---
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // --- 2. RECYCLERVIEW CONFIGURATION ---
        recyclerView = findViewById(R.id.recyclerAllArticles);

        // Professional Optimization: Improves performance when item sizes don't change
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // --- 3. DATA SOURCE (CURATED CONTENT) ---
        loadArticleData();

        // --- 4. ADAPTER INTEGRATION ---
        // Passing 'this' for Context ensures the adapter can trigger Implicit Intents safely
        adapter = new ArticleAdapter(articleList, this);
        recyclerView.setAdapter(adapter);
    }

    /**
     * Initializes the static content list.
     * Refactored into a separate method to allow for future Firebase integration.
     */
    private void loadArticleData() {
        articleList = new ArrayList<>();

        articleList.add(new ArticleModel(
                "5 Ways to Boost Immunity",
                "Wellness",
                "14 Jan 2026",
                R.drawable.img_immunity,
                "https://www.healthline.com/nutrition/how-to-boost-immune-system"
        ));

        articleList.add(new ArticleModel(
                "Heart Health & Diet Tips",
                "Cardiology",
                "12 Jan 2026",
                R.drawable.img_heart_health,
                "https://www.heart.org/en/healthy-living/healthy-eating"
        ));

        articleList.add(new ArticleModel(
                "Understanding Mental Anxiety",
                "Mental Health",
                "10 Jan 2026",
                R.drawable.img_mental_exercise,
                "https://www.mayoclinic.org/diseases-conditions/anxiety/symptoms-causes/syc-20350961"
        ));

        articleList.add(new ArticleModel(
                "Best Exercises for Back Pain",
                "Physiotherapy",
                "08 Jan 2026",
                R.drawable.img_back_exercise,
                "https://www.webmd.com/back-pain/ss/slideshow-exercises-for-lower-back-pain"
        ));

        articleList.add(new ArticleModel(
                "Sugar Free Diet Plans",
                "Nutrition",
                "05 Jan 2026",
                R.drawable.img_sugar_free_diet,
                "https://www.medicalnewstoday.com/articles/320498"
        ));

        articleList.add(new ArticleModel(
                "Importance of Eye Checkups",
                "Eye Care",
                "02 Jan 2026",
                R.drawable.img_eye_checkups,
                "https://www.aao.org/eye-health/tips-prevention/eye-exams-101"
        ));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Professional apps refresh the list or check for network
        // connectivity here if data was dynamic.
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Handled via finish() in btnBack, but maintained for system gesture safety
    }
}