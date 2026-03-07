package com.example.docconnect;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import java.util.List;
import java.util.Locale;

/**
 * Adapter class for managing and displaying a list of doctors in the search results.
 * Handles data binding, image loading with Glide, and navigation to details/booking.
 */
public class SearchDoctorAdapter extends RecyclerView.Adapter<SearchDoctorAdapter.ViewHolder> {

    private final Context context;
    private List<SearchDoctorModel> list;

    public SearchDoctorAdapter(Context context, List<SearchDoctorModel> list) {
        this.context = context;
        this.list = list;
    }

    /**
     * Updates the list data when a search filter is applied.
     * @param filteredList The new list of doctors matching the search query.
     */
    public void filterList(List<SearchDoctorModel> filteredList) {
        this.list = filteredList;
        // Optimization: Notify the adapter that the underlying data set has changed
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the custom card layout for each doctor item
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_doctor_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchDoctorModel doctor = list.get(position);

        // Safety Null Checks: Provide default values to prevent layout breaks or crashes
        String avgRating = doctor.getAverageRating() != null ? doctor.getAverageRating() : "0.0";
        String totalReviews = doctor.getReviewCount() != null ? doctor.getReviewCount() : "0";

        // Set primary text data
        holder.tvName.setText(doctor.getFullName());
        holder.tvSpecialty.setText(doctor.getSpeciality());
        holder.tvRating.setText(avgRating);
        holder.tvReviewCount.setText("(" + totalReviews + " Reviews)"); // UI formatting: (45)

        // Address check: Only set if the view exists in the layout
        if (holder.tvAddress != null) {
            holder.tvAddress.setText(doctor.getClinicAddress());
        }

        // Distance Logic: Dynamically show/hide distance based on availability
        if (doctor.getDistance() > 0) {
            holder.tvDistance.setVisibility(View.VISIBLE);
            // Format to 1 decimal place (e.g., "2.4 km away")
            holder.tvDistance.setText(String.format(Locale.getDefault(), "%.1f km away", doctor.getDistance()));
        } else {
            holder.tvDistance.setVisibility(View.GONE);
        }

        // Image Loading: Efficiently load profile images using Glide
        Glide.with(context)
                .load(doctor.getProfileImageUrl())
                .placeholder(R.drawable.ic_doctor) // Shown while loading
                .error(R.drawable.ic_doctor)       // Shown on failure
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache for performance/battery
                .into(holder.ivDoctorImage);

        // Set click listeners for the action buttons
        holder.btnProfile.setOnClickListener(v -> navigateToProfile(doctor));
        holder.btnBookNow.setOnClickListener(v -> navigateToBooking(doctor));
    }

    /**
     * Passes doctor data to the Profile Activity.
     */
    private void navigateToProfile(SearchDoctorModel doctor) {
        Intent intent = new Intent(context, DoctorProfileActivity.class);
        intent.putExtra("id", doctor.getId());
        intent.putExtra("name", doctor.getFullName());
        intent.putExtra("specialty", doctor.getSpeciality());
        intent.putExtra("ratings", doctor.getAverageRating());
        intent.putExtra("reviewCount", doctor.getReviewCount());
        intent.putExtra("experience", doctor.getExperience());
        intent.putExtra("patients", doctor.getTotalPatients());
        intent.putExtra("fee", doctor.getConsultationFees());
        intent.putExtra("clinicAddress", doctor.getClinicAddress());
        intent.putExtra("clinicName", doctor.getClinicName());
        intent.putExtra("bio", doctor.getBio());
        intent.putExtra("image", doctor.getProfileImageUrl());
        context.startActivity(intent);
    }

    /**
     * Passes relevant data to the Consultation/Booking Activity.
     */
    private void navigateToBooking(SearchDoctorModel doctor) {
        Intent intent = new Intent(context, ChooseConsultationActivity.class);
        intent.putExtra("id", doctor.getId());
        intent.putExtra("doctorName", doctor.getFullName());
        intent.putExtra("doctorSpecialty", doctor.getSpeciality());
        intent.putExtra("doctorFee", doctor.getConsultationFees());
        intent.putExtra("ratings", doctor.getAverageRating());
        intent.putExtra("clinicName", doctor.getClinicName());
        intent.putExtra("clinicAddress", doctor.getClinicAddress());
        intent.putExtra("doctorImage", doctor.getProfileImageUrl());
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    /**
     * ViewHolder class to hold and cache view references for smooth scrolling.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSpecialty, tvRating, tvAddress, tvReviewCount, tvDistance;
        ImageView ivDoctorImage;
        MaterialButton btnProfile, btnBookNow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvDoctorName);
            tvSpecialty = itemView.findViewById(R.id.tvSpecialty);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvReviewCount = itemView.findViewById(R.id.tvReviewCount);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            ivDoctorImage = itemView.findViewById(R.id.ivDoctorImage);
            btnProfile = itemView.findViewById(R.id.btnProfile);
            btnBookNow = itemView.findViewById(R.id.btnBookNow);
        }
    }
}