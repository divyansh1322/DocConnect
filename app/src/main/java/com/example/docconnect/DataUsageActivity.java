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

public class DataUsageActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TermsAdapter adapter;
    private List<TermModel> dataList;
    private ProgressBar progressBar;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_usage);

        // Initialize UI Components
        progressBar = findViewById(R.id.progressBar);
        ImageButton btnBack = findViewById(R.id.btnBack);
        recyclerView = findViewById(R.id.recyclerViewDataUsage);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        dataList = new ArrayList<>();
        adapter = new TermsAdapter(dataList, recyclerView);
        recyclerView.setAdapter(adapter);

        // Navigation
        btnBack.setOnClickListener(v -> finish());

        // Firebase Path
        mDatabase = FirebaseDatabase.getInstance().getReference("data_usage");

        loadData();
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                dataList.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        // FIX: Use Object and String.valueOf to prevent casting crashes
                        Object titleValue = data.child("title").getValue();
                        Object contentValue = data.child("content").getValue();

                        String title = (titleValue != null) ? String.valueOf(titleValue) : "";
                        String content = (contentValue != null) ? String.valueOf(contentValue) : "";

                        if (!title.isEmpty() || !content.isEmpty()) {
                            dataList.add(new TermModel(title, content));
                        }
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(DataUsageActivity.this, "No data usage info found.", Toast.LENGTH_SHORT).show();
                }

                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DataUsageActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}