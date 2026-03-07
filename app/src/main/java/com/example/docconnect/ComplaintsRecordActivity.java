package com.example.docconnect;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class ComplaintsRecordActivity extends AppCompatActivity {
    private RecyclerView rvComplaints;
    private LinearLayout layoutEmpty;
    private AdminComplaintAdapter adapter;
    private List<AdminComplaintModel> complaintList;
    private DatabaseReference dbRef;
    private ValueEventListener currentListener;
    private MaterialButtonToggleGroup toggleGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaints_record);

        rvComplaints = findViewById(R.id.rvComplaints);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        toggleGroup = findViewById(R.id.toggleGroup);

        rvComplaints.setLayoutManager(new LinearLayoutManager(this));
        complaintList = new ArrayList<>();
        adapter = new AdminComplaintAdapter(this, complaintList);
        rvComplaints.setAdapter(adapter);

        // DEFAULT: Load User Complaints
        loadComplaints("users", "report_issue");

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnUserComplaints) {
                    loadComplaints("users", "report_issue");
                } else {
                    loadComplaints("doctors", "support_tickets");
                }
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void loadComplaints(String node, String key) {
        if (dbRef != null && currentListener != null) dbRef.removeEventListener(currentListener);

        dbRef = FirebaseDatabase.getInstance().getReference(node);
        currentListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                complaintList.clear();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    if (userSnap.hasChild(key)) {
                        for (DataSnapshot ticket : userSnap.child(key).getChildren()) {
                            AdminComplaintModel m = ticket.getValue(AdminComplaintModel.class);
                            if (m != null) {
                                m.setTicketId(ticket.getKey());
                                m.setUserId(userSnap.getKey());
                                m.setUserType(node); // Tag as "users" or "doctors"
                                m.setNodeKey(key);   // Tag as "report_issue" or "support_tickets"
                                complaintList.add(m);
                            }
                        }
                    }
                }
                updateUI();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        dbRef.addValueEventListener(currentListener);
    }

    private void updateUI() {
        layoutEmpty.setVisibility(complaintList.isEmpty() ? View.VISIBLE : View.GONE);
        rvComplaints.setVisibility(complaintList.isEmpty() ? View.GONE : View.VISIBLE);
        adapter.notifyDataSetChanged();
    }
}