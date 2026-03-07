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

public class AboutUsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TermsAdapter adapter;
    private List<TermModel> aboutList;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        // 1. Initialize Views
        progressBar = findViewById(R.id.progressBarAbout);
        btnBack = findViewById(R.id.btnBackAbout);
        recyclerView = findViewById(R.id.recyclerViewAbout);

        // 2. Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        aboutList = new ArrayList<>();
        adapter = new TermsAdapter(aboutList, recyclerView);
        recyclerView.setAdapter(adapter);

        // 3. Navigation
        btnBack.setOnClickListener(v -> finish());

        // 4. Firebase Path
        mDatabase = FirebaseDatabase.getInstance().getReference().child("about_us");

        loadDataFromFirebase();
    }

    private void loadDataFromFirebase() {
        progressBar.setVisibility(View.VISIBLE);

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                aboutList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        String title = data.child("title").getValue(String.class);
                        String content = data.child("content").getValue(String.class);
                        aboutList.add(new TermModel(title, content));
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(AboutUsActivity.this, "About information not found", Toast.LENGTH_SHORT).show();
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AboutUsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}