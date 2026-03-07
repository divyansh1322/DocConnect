package com.example.docconnect;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CHATS FRAGMENT
 * Manages active chat consultations using Firebase Real-time Database.
 * Optimized with View Caching to prevent re-fetching during tab switches.
 */
public class ChatsFragment extends Fragment {

    // --- 1. VIEW CACHING VARIABLES ---
    private View rootView;
    private boolean isInitialized = false;

    // --- UI ELEMENTS ---
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatModel> chatList = new ArrayList<>();
    private ImageButton btnBack;
    private ProgressBar progressBar;
    private LinearLayout layoutNoChats;
    private TextView tvAllChats;

    // --- FIREBASE & THREADING ---
    private DatabaseReference dbRef;
    private ValueEventListener chatListener;
    private final ExecutorService dataExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // STEP 1: Check if rootView exists. Return it to keep UI state.
        if (rootView != null) {
            return rootView;
        }

        // STEP 2: Inflate layout for the first time
        rootView = inflater.inflate(R.layout.fragment_chats, container, false);

        // Initialize UI components
        initViews(rootView);
        setupRecyclerView();

        // STEP 3: Setup initial state and Firebase only once
        if (!isInitialized) {
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
            if (layoutNoChats != null) layoutNoChats.setVisibility(View.GONE);
            if (tvAllChats != null) tvAllChats.setVisibility(View.GONE);

            // Delay for smooth transition
            mainHandler.postDelayed(this::initFirebase, 150);
            isInitialized = true;
        }

        return rootView;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.rvChats);
        layoutNoChats = view.findViewById(R.id.layoutNoChats);
        progressBar = view.findViewById(R.id.progressBar);
        btnBack = view.findViewById(R.id.btnBack);
        tvAllChats = view.findViewById(R.id.tvAllChats);

        // Back button: Navigates back to Home in MainActivity
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).bottomNavigationView.setSelectedItemId(R.id.nav_home);
                } else if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        // New Chat button leads to doctor search
        View btnNewChat = view.findViewById(R.id.btnNewChat);
        if (btnNewChat != null) {
            btnNewChat.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), SearchResultsActivity.class)));
        }
    }

    private void setupRecyclerView() {
        if (recyclerView == null) return;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        chatAdapter = new ChatAdapter(chatList);
        recyclerView.setAdapter(chatAdapter);
    }

    private void initFirebase() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            return;
        }

        dbRef = FirebaseDatabase.getInstance().getReference("UserBookings").child(uid);

        chatListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Background processing to keep the main thread smooth
                dataExecutor.execute(() -> {
                    List<ChatModel> newList = new ArrayList<>();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        ChatModel model = ds.getValue(ChatModel.class);
                        // Filter for "Chat" consultations
                        if (model != null && "Chat".equalsIgnoreCase(model.getConsultationMedium())) {
                            newList.add(model);
                        }
                    }

                    // Sort newest first
                    Collections.reverse(newList);

                    mainHandler.post(() -> {
                        if (isAdded()) updateUI(newList);
                    });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                mainHandler.post(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                });
                Log.e("FirebaseChat", error.getMessage());
            }
        };
        dbRef.addValueEventListener(chatListener);
    }

    private void updateUI(List<ChatModel> newList) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);

        if (newList.isEmpty()) {
            if (layoutNoChats != null) layoutNoChats.setVisibility(View.VISIBLE);
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
            if (tvAllChats != null) tvAllChats.setVisibility(View.GONE);
        } else {
            if (layoutNoChats != null) layoutNoChats.setVisibility(View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
            if (tvAllChats != null) tvAllChats.setVisibility(View.VISIBLE);

            chatList.clear();
            chatList.addAll(newList);
            if (chatAdapter != null) chatAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Memory management: Full cleanup when fragment is actually destroyed
        if (dbRef != null && chatListener != null) {
            dbRef.removeEventListener(chatListener);
        }
        dataExecutor.shutdown();
    }
}