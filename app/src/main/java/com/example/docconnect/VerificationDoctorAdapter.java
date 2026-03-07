package com.example.docconnect;

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
 * Adapter for the Admin side Doctor Verification list.
 * This class handles the display of doctors awaiting approval and provides
 * visual feedback on their current application status (Pending, Approved, or Rejected).
 */
public class VerificationDoctorAdapter extends RecyclerView.Adapter<VerificationDoctorAdapter.ViewHolder> {

    private final List<VerificationDoctorModel> doctorList;
    private final OnStatusActionListener actionListener;

    /**
     * Interface for handling status updates (Approve/Reject) from the UI.
     */
    public interface OnStatusActionListener {
        void onStatusUpdate(String doctorId, String newStatus);
    }

    public VerificationDoctorAdapter(List<VerificationDoctorModel> doctorList, OnStatusActionListener actionListener) {
        this.doctorList = doctorList;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the custom verification item layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_doctor_verification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VerificationDoctorModel doctor = doctorList.get(position);

        // Set basic doctor information
        holder.tvName.setText(doctor.getFullName());
        holder.tvDetails.setText(doctor.getSpeciality() + " | " + doctor.getClinicName());

        // --- STATUS UI LOGIC ---
        // Dynamically updates the badge appearance based on the doctor's verification state.
        String status = (doctor.getStatus() != null) ? doctor.getStatus().toLowerCase() : "pending";
        holder.tvStatus.setText(status.toUpperCase());

        if (status.equals("approved") || status.equals("verified")) {
            // Success state: Green styling
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_success);
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.doc_success));
        } else if (status.equals("rejected")) {
            // Error state: Red styling
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_rejected);
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.doc_error));
        } else {
            // Waiting state: Default/Yellow styling for "Pending"
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_pending);
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.doc_waiting));
        }

        // --- IMAGE LOADING ---
        // Load profile picture with Glide, using a placeholder while the network request is active.
        Glide.with(holder.itemView.getContext())
                .load(doctor.getProfileImageUrl())
                .placeholder(R.drawable.ic_doctor)
                .circleCrop()
                .into(holder.ivProfile);

        // --- NAVIGATION ---
        // Clicking "View Documents" takes the admin to the comprehensive details screen.
        holder.btnViewDocs.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), ComprehensiveVerificationActivity.class);
            // Pass the ID to fetch full registration details and medical licenses
            intent.putExtra("doctorId", doctor.getId());
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return (doctorList != null) ? doctorList.size() : 0;
    }

    /**
     * ViewHolder for caching view references within the verification list item.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDetails, tvStatus;
        Button btnViewDocs;
        ImageView ivProfile;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Link XML components to Java objects
            tvName = itemView.findViewById(R.id.tvDoctorName);
            tvDetails = itemView.findViewById(R.id.tvDoctorDetails);
            tvStatus = itemView.findViewById(R.id.tvStatusBadge);
            btnViewDocs = itemView.findViewById(R.id.btnViewDocuments);
            ivProfile = itemView.findViewById(R.id.ivDoctorProfile);
        }
    }
}