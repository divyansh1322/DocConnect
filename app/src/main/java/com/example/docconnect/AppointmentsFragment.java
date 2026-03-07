package com.example.docconnect;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * AppointmentsFragment: Manages tabbed navigation for user bookings.
 * Optimized with View Caching to prevent re-inflation and flickering.
 */
public class AppointmentsFragment extends Fragment {

    // --- 1. VIEW CACHING VARIABLES ---
    private View rootView;
    private boolean isInitialized = false;

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TabPagerAdapter adapter;

    // Search UI Elements
    private TextView tvScreenTitle;
    private ImageView ivSearch, ivCloseSearch;
    private EditText etSearch;

    // Back Button
    private ImageView ivBack;

    public AppointmentsFragment() {
        // Required empty constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // STEP 1: Check if rootView already exists. If yes, return it immediately.
        // This keeps the ViewPager, Tabs, and Search state exactly as the user left them.
        if (rootView != null) {
            return rootView;
        }

        // STEP 2: Inflate the layout for the first time
        rootView = inflater.inflate(R.layout.fragment_appointments, container, false);

        // Initialize Views
        tabLayout = rootView.findViewById(R.id.tabLayout);
        viewPager = rootView.findViewById(R.id.viewPager);
        tvScreenTitle = rootView.findViewById(R.id.tvScreenTitle);
        ivSearch = rootView.findViewById(R.id.ivSearch);
        ivCloseSearch = rootView.findViewById(R.id.ivCloseSearch);
        etSearch = rootView.findViewById(R.id.etSearch);
        ivBack = rootView.findViewById(R.id.ivBack);

        // STEP 3: Setup Logic only if not initialized
        if (!isInitialized) {
            setupNavigation();
            setupViewPager();
            setupSearchLogic();
            isInitialized = true;
        }

        return rootView;
    }

    private void setupNavigation() {
        // Back Button Logic -> Navigate to Dashboard/Home
        ivBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
    }

    private void setupViewPager() {
        // Setup ViewPager Adapter
        adapter = new TabPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Attach TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0: tab.setText("Upcoming"); break;
                        case 1: tab.setText("Completed"); break;
                        case 2: tab.setText("Cancelled"); break;
                    }
                }).attach();

        // Optimization: Keep all fragments in memory to prevent reloading when swiping tabs
        viewPager.setOffscreenPageLimit(2);
    }

    private void setupSearchLogic() {
        // Open Search UI
        ivSearch.setOnClickListener(v -> {
            tvScreenTitle.setVisibility(View.GONE);
            ivSearch.setVisibility(View.GONE);
            if (ivBack != null) ivBack.setVisibility(View.GONE);
            etSearch.setVisibility(View.VISIBLE);
            ivCloseSearch.setVisibility(View.VISIBLE);

            // Focus and show keyboard
            etSearch.requestFocus();
            showKeyboard(etSearch);
        });

        // Close Search UI
        ivCloseSearch.setOnClickListener(v -> {
            etSearch.setText("");
            etSearch.setVisibility(View.GONE);
            ivCloseSearch.setVisibility(View.GONE);
            tvScreenTitle.setVisibility(View.VISIBLE);
            ivSearch.setVisibility(View.VISIBLE);
            if (ivBack != null) ivBack.setVisibility(View.VISIBLE);

            // Hide keyboard
            hideKeyboard(etSearch);

            // Reset list in current fragment
            performSearch("");
        });

        // Search Text Listener
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Re-filter when user swipes tabs if search is active
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (etSearch.getVisibility() == View.VISIBLE) {
                    performSearch(etSearch.getText().toString());
                }
            }
        });
    }

    private void performSearch(String query) {
        // Find the currently active fragment in the ViewPager by its tag
        // ViewPager2 uses "f" + position for internal fragment tags
        Fragment currentFragment = getChildFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());

        if (currentFragment instanceof SearchableFragment) {
            ((SearchableFragment) currentFragment).filterList(query);
        }
    }

    // Keyboard Utilities
    private void showKeyboard(View view) {
        if (getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard(View view) {
        if (getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}