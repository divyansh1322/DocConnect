package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {
    private EditText emailEt, passwordEt;
    private MaterialButton signInBtn;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();

        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);
        signInBtn = findViewById(R.id.signInBtn);
        progressBar = findViewById(R.id.progressBar);

        signInBtn.setOnClickListener(v -> {
            String email = emailEt.getText().toString().trim();
            String password = passwordEt.getText().toString().trim();

            if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
                loginUser(email, password);
            } else {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginUser(String email, String password) {
        setLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // Auth matched! Now determine if UID belongs to Admin or User node
                    checkIdentity(authResult.getUser().getUid());
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Login Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkIdentity(String uid) {
        // STEP 1: Look in Admins Node
        rootRef.child("admins").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // FOUND IN ADMIN NODE
                    navigateTo(AdminDashboardActivity.class, "admins");

                } else {
                    // NOT IN ADMIN NODE -> STEP 2: Look in Users Node
                    checkUserNode(uid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // If Rules block reading the Admin node, it means this isn't an admin.
                checkUserNode(uid);
            }
        });
    }

    private void checkUserNode(String uid) {
        rootRef.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                setLoading(false);
                if (snapshot.exists()) {
                    // FOUND IN USERS NODE
                    boolean isComp = Boolean.TRUE.equals(snapshot.child("isProfileCompleted").getValue(Boolean.class));
                    navigateTo(isComp ? MainActivity.class : ProfileCreationActivity.class, "user");
                } else {
                    // NOT FOUND ANYWHERE
                    mAuth.signOut();
                    Toast.makeText(LoginActivity.this, "Account Not Found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateTo(Class<?> targetActivity, String role) {
        // Save role in preferences
        getSharedPreferences("DocConnectData", MODE_PRIVATE).edit().putString("selected_role", role).apply();

        Intent intent = new Intent(LoginActivity.this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        signInBtn.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
    }
}