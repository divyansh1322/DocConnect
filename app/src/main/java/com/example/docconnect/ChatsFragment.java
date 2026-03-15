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
 * Uses a static cache to ensure data is displayed INSTANTLY when switching fragments.
 * Includes fixes for Threading crashes and redundant loading states.
 */
public class ChatsFragment extends Fragment {

    // --- 1. STATIC CACHE ---
    // This stays in memory even if the Fragment is destroyed/recreated during tab switches.
    private static ArrayList<ChatModel> cachedChatList = new ArrayList<>();

    // --- UI ELEMENTS ---
    private View rootView;
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private ProgressBar progressBar;
    private LinearLayout layoutNoChats;
    private TextView tvAllChats;

    // --- FIREBASE & THREADING ---
    private DatabaseReference dbRef;
    private ValueEventListener chatListener;

    // Single thread executor for background data processing
    private final ExecutorService dataExecutor = Executors.newSingleThreadExecutor();
    // Handler to push UI updates back to the Main Thread
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // STEP 1: View Caching - Reuse the root view if it exists to prevent flickering
        if (rootView != null) {
            return rootView;
        }

        rootView = inflater.inflate(R.layout.fragment_chats, container, false);

        // STEP 2: Initialize Views
        initViews(rootView);
        setupRecyclerView();

        // STEP 3: Smart Initialization Logic
        if (!cachedChatList.isEmpty()) {
            // Data is already in memory! Show it immediately.
            setLoadingState(false);
            chatAdapter.notifyDataSetChanged();

            // Refresh from Firebase silently in the background
            initFirebase();
        } else {
            // First time opening the app or list is empty: Show loader
            setLoadingState(true);
            // Delay slightly for smooth fragment transition animations
            mainHandler.postDelayed(this::initFirebase, 150);
        }

        return rootView;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.rvChats);
        layoutNoChats = view.findViewById(R.id.layoutNoChats);
        progressBar = view.findViewById(R.id.progressBar);
        tvAllChats = view.findViewById(R.id.tvAllChats);

        // New Chat button navigation
        View btnNewChat = view.findViewById(R.id.btnNewChat);
        if (btnNewChat != null) {
            btnNewChat.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), SearchResultsActivity.class)));
        }

        // Optional Back Button (if present in your layout)
        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) getActivity().onBackPressed();
            });
        }
    }

    private void setupRecyclerView() {
        if (recyclerView == null) return;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        // Link adapter to our static memory cache
        chatAdapter = new ChatAdapter(cachedChatList);
        recyclerView.setAdapter(chatAdapter);
    }

    private void initFirebase() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            setLoadingState(false);
            return;
        }

        // Clean up any existing listener before creating a new one
        if (dbRef != null && chatListener != null) {
            dbRef.removeEventListener(chatListener);
        }

        dbRef = FirebaseDatabase.getInstance().getReference("UserBookings").child(uid);

        chatListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Check if executor is alive before submitting task (fixes RejectedExecutionException)
                if (!dataExecutor.isShutdown()) {
                    dataExecutor.execute(() -> {
                        ArrayList<ChatModel> newList = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ChatModel model = ds.getValue(ChatModel.class);
                            if (model != null && "Chat".equalsIgnoreCase(model.getConsultationMedium())) {
                                newList.add(model);
                            }
                        }

                        // Sort newest bookings to the top
                        Collections.reverse(newList);

                        // Return to Main Thread to update the UI
                        mainHandler.post(() -> {
                            // Ensure fragment is still visible to the user
                            if (isAdded() && getContext() != null) {
                                updateUI(newList);
                            }
                        });
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                mainHandler.post(() -> setLoadingState(false));
                Log.e("FirebaseChat", "Database Error: " + error.getMessage());
            }
        };

        dbRef.addValueEventListener(chatListener);
    }

    private void updateUI(ArrayList<ChatModel> newList) {
        setLoadingState(false);

        // Update the static memory cache
        cachedChatList.clear();
        cachedChatList.addAll(newList);

        if (chatAdapter != null) {
            chatAdapter.notifyDataSetChanged();
        }

        // Toggle Visibility
        if (cachedChatList.isEmpty()) {
            layoutNoChats.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            tvAllChats.setVisibility(View.GONE);
        } else {
            layoutNoChats.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            tvAllChats.setVisibility(View.VISIBLE);
        }
    }

    private void setLoadingState(boolean isLoading) {
        if (progressBar != null) {
            // ONLY show the progress bar if we have ZERO data in memory
            if (isLoading && cachedChatList.isEmpty()) {
                progressBar.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                layoutNoChats.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove listener to prevent memory leaks when the user switches tabs
        if (dbRef != null && chatListener != null) {
            dbRef.removeEventListener(chatListener);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Permanently kill the background thread when the Fragment is fully destroyed
        if (!dataExecutor.isShutdown()) {
            dataExecutor.shutdownNow();
        }
    }
}