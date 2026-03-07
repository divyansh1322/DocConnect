package com.example.docconnect;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

/**
 * Adapter for Admin User Management.
 * Handles displaying user profiles, dynamic status badges (Active/Blocked),
 * and navigating to detailed user management screens.
 */
public class UserManageAdapter extends RecyclerView.Adapter<UserManageAdapter.UserViewHolder> {

    private List<UserManageModel> userList;

    public UserManageAdapter(List<UserManageModel> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the admin-specific user management item layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_management, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserManageModel user = userList.get(position);
        Context context = holder.itemView.getContext();

        // 1. SET BASIC DATA
        // Populates the text fields with user information from the model
        holder.tvName.setText(user.getFullName());
        holder.tvEmail.setText(user.getEmail());
        holder.tvJoinDate.setText("Joined: " + user.getJoinedDate());

        // 2. LOAD PROFILE IMAGE
        // Uses Glide for efficient image caching and circular cropping
        Glide.with(context)
                .load(user.getProfilePhotoUrl())
                .placeholder(R.drawable.ic_person_placeholder) // Default icon while loading
                .error(R.drawable.ic_person_placeholder)       // Default icon if URL is broken
                .circleCrop()                     // Makes the user image round
                .into(holder.userImage);

        // 3. HANDLE STATUS BADGE STYLING
        // Dynamically changes the badge color and text based on account status
        String status = (user.getStatus() != null) ? user.getStatus() : "Active";
        holder.tvStatus.setText(status.toUpperCase());

        // Apply conditional styling for 'Blocked' vs 'Active' accounts
        if (status.equalsIgnoreCase("Blocked")) {
            // Error red styling for blocked users
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_blocked);
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.doc_error));
        } else {
            // Success green styling for active users
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_active);
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.doc_success));
        }

        // 4. NAVIGATION LOGIC
        // Directs the Admin to the details page for the specific user
        holder.btnView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AdminUserDetailsActivity.class);
            // Pass the unique user ID to fetch specific data in the next activity
            intent.putExtra("userId", user.getUserId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        // Safety check to prevent NullPointerExceptions
        return userList == null ? 0 : userList.size();
    }

    /**
     * Helper method to refresh the entire user list (e.g., after a search or filter).
     * @param newList The updated list of users.
     */
    public void updateList(List<UserManageModel> newList) {
        this.userList = newList;
        // Optimization: In a production app, consider using DiffUtil instead of notifyDataSetChanged
        notifyDataSetChanged();
    }

    /**
     * ViewHolder class to hold references to the UI components for each list item.
     */
    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvJoinDate, tvStatus;
        ImageView userImage;
        Button btnView;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            // Link Java objects to XML IDs defined in item_user_management.xml
            tvName = itemView.findViewById(R.id.tvfullName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvJoinDate = itemView.findViewById(R.id.tvJoinDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnView = itemView.findViewById(R.id.btnView);
            userImage = itemView.findViewById(R.id.userImage);
        }
    }
}