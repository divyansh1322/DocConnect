package com.example.docconnect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Adapter for managing the TabLayout and ViewPager2 inside the Appointments screen.
 * It handles the creation and lifecycle of three distinct fragments: Upcoming, Completed, and Cancelled.
 */
public class TabPagerAdapter extends FragmentStateAdapter {

    /**
     * Constructor used when the ViewPager is hosted within another Fragment.
     * @param fragment The parent Fragment (e.g., AppointmentHistoryFragment).
     */
    public TabPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    /**
     * Logic to determine which Fragment should be displayed based on the tab position.
     * @param position Current index (0, 1, or 2).
     * @return The specific fragment for that category.
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                // Displays active/future appointments
                return new UpcomingFragment();
            case 1:
                // Displays past appointments that were successfully finished
                return new CompletedFragment();
            case 2:
                // Displays appointments that were rejected or cancelled
                return new CancelledFragment();
            default:
                // Default fallback to ensure the app doesn't crash on invalid positions
                return new UpcomingFragment();
        }
    }

    /**
     * Defines the total number of tabs available in the UI.
     * @return Constant 3 for the three categories.
     */
    @Override
    public int getItemCount() {
        return 3;
    }

    /**
     * Provides the text label for each tab.
     * Note: In ViewPager2 with TabLayout, this is often used inside a TabLayoutMediator.
     * @param position Tab index.
     * @return The capitalized title of the tab.
     */
    @Nullable
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "UPCOMING";
            case 1:
                return "COMPLETED";
            case 2:
                return "CANCELLED";
            default:
                return null;
        }
    }
}