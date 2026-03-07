package com.example.docconnect;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class PrivacyActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TermsAdapter adapter;
    private List<TermModel> privacyList;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy);

        // Initialize Views using the new IDs from the XML
        progressBar = findViewById(R.id.progressBarPrivacy);
        btnBack = findViewById(R.id.btnBackPrivacy);
        recyclerView = findViewById(R.id.recyclerViewPrivacy);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        privacyList = new ArrayList<>();
        adapter = new TermsAdapter(privacyList, recyclerView);
        recyclerView.setAdapter(adapter);

        // Back Button click logic
        btnBack.setOnClickListener(v -> onBackPressed());

        // Path set to Firebase node "privacy_policy"
        mDatabase = FirebaseDatabase.getInstance().getReference("privacy_policy");

        loadPrivacyData();
    }

    private void loadPrivacyData() {
        progressBar.setVisibility(View.VISIBLE);

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                privacyList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        String title = data.child("title").getValue(String.class);
                        String content = data.child("content").getValue(String.class);
                        privacyList.add(new TermModel(title, content));
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(PrivacyActivity.this, "No policy data found", Toast.LENGTH_SHORT).show();
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PrivacyActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}