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

public class TermsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TermsAdapter adapter;
    private List<TermModel> termList;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_term);

        // Initialize Views
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);
        recyclerView = findViewById(R.id.recyclerViewTerms);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        termList = new ArrayList<>();
        adapter = new TermsAdapter(termList, recyclerView);
        recyclerView.setAdapter(adapter);

        // Back Button Logic
        btnBack.setOnClickListener(v -> {
            // Closes this activity and goes back to the previous one
            onBackPressed();
        });

        // Firebase Reference
        mDatabase = FirebaseDatabase.getInstance().getReference().child("terms_and_conditions");

        loadDataFromFirebase();
    }

    private void loadDataFromFirebase() {
        progressBar.setVisibility(View.VISIBLE);

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                termList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        String title = data.child("title").getValue(String.class);
                        String content = data.child("content").getValue(String.class);
                        termList.add(new TermModel(title, content));
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(TermsActivity.this, "No data found", Toast.LENGTH_SHORT).show();
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TermsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}