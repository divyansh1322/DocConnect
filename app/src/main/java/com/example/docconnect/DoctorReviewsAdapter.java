package com.example.docconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class DoctorReviewsAdapter extends RecyclerView.Adapter<DoctorReviewsAdapter.ReviewViewHolder> {

    private List<DoctorReviewsModel> reviewsList;

    public DoctorReviewsAdapter(List<DoctorReviewsModel> reviewsList) {
        this.reviewsList = reviewsList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_doctor_reviews, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        DoctorReviewsModel review = reviewsList.get(position);

        // 1. Extract and Sanitize Data
        String name = (review.getPatientName() != null && !review.getPatientName().isEmpty())
                ? review.getPatientName() : "Anonymous";

        // 2. Bind Data to Views
        holder.tvPatientName.setText(name);
        holder.tvDate.setText(review.getDate());
        holder.tvFeedback.setText(review.getFeedback());

        // Formatting rating to 1 decimal point (e.g., 4.0)
        holder.tvRatings.setText(String.format(Locale.getDefault(), "%.1f", review.getRating()));

        // 3. Set Profile Initials
        holder.tvInitials.setText(getInitials(name));
    }

    @Override
    public int getItemCount() {
        return reviewsList == null ? 0 : reviewsList.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvDate, tvFeedback, tvRatings, tvInitials;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            // Matching the IDs from your professional XML layout
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvFeedback = itemView.findViewById(R.id.tvfeedback); // ID from your XML
            tvRatings = itemView.findViewById(R.id.tvRatings);
            tvInitials = itemView.findViewById(R.id.tvInitials);
        }
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty() || name.equalsIgnoreCase("Anonymous")) {
            return "?";
        }

        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();

        for (int i = 0; i < Math.min(parts.length, 2); i++) {
            if (!parts[i].isEmpty()) {
                initials.append(parts[i].substring(0, 1).toUpperCase());
            }
        }
        return initials.length() > 0 ? initials.toString() : "?";
    }
}