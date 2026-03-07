package com.example.docconnect;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * AdminAppointmentAdapter: Professional Version
 * Optimized with DiffUtil, dynamic styling, and crash-resilient context handling.
 */
public class AdminAppointmentAdapter extends RecyclerView.Adapter<AdminAppointmentAdapter.ViewHolder> {

    private List<AdminAppointmentModel> list;

    /**
     * Constructor initializes the list. Prevents NullPointerExceptions by
     * defaulting to an empty ArrayList if the input is null.
     */
    public AdminAppointmentAdapter(List<AdminAppointmentModel> list) {
        this.list = (list != null) ? list : new ArrayList<>();
    }

    /**
     * Updates the list using the DiffUtil algorithm.
     * This is significantly more efficient than notifyDataSetChanged().
     */
    public void updateList(List<AdminAppointmentModel> newList) {
        if (newList == null) return; // Protection against null updates

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return list.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                // Returns true if the unique ID of the appointment matches
                String oldId = list.get(oldItemPosition).getId();
                String newId = newList.get(newItemPosition).getId();
                return Objects.equals(oldId, newId);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                // Returns true if the specific data (status, time) is identical
                return Objects.equals(list.get(oldItemPosition), newList.get(newItemPosition));
            }
        });

        this.list.clear();
        this.list.addAll(newList);
        // Dispatches specific animations (add/remove/move) to the RecyclerView
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the custom layout for the admin appointment item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_appointments, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminAppointmentModel model = list.get(position);

        // Map data to the TextViews with null safety checks
        holder.tvDoctorName.setText(model.getDoctorName() != null ? model.getDoctorName() : "Unknown Doctor");
        holder.tvPatientName.setText(model.getPatientName() != null ? model.getPatientName() : "Unknown Patient");
        holder.tvDate.setText(model.getDate());
        holder.tvTime.setText(model.getTime());

        // Process status string and apply dynamic UI styling
        String status = (model.getStatus() != null) ? model.getStatus().toUpperCase() : "UPCOMING";
        holder.tvStatusTag.setText(status);
        updateStatusUI(holder, status);

        /**
         * Detail Navigation Logic:
         * Passes the specific Booking ID and User ID to the detail screen.
         */
        holder.btnviewDetail.setOnClickListener(v -> {
            Context context = holder.itemView.getContext();
            if (context != null) {
                try {
                    Intent intent = new Intent(context, AdminAppointmentDetailActivity.class);
                    intent.putExtra("userId", model.getUserId());
                    intent.putExtra("bookingId", model.getId());
                    // Professional handling for starting activities from an adapter context
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Unable to open details", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Dynamically changes the color of the Status Badge and Indicator
     * based on the current state of the appointment.
     */
    private void updateStatusUI(ViewHolder holder, String status) {
        int mainColor;
        int backgroundColor;

        try {
            switch (status) {
                case "COMPLETED":
                    mainColor = Color.parseColor("#2E7D32"); // Green
                    backgroundColor = Color.parseColor("#E8F5E9");
                    break;
                case "UPCOMING":
                    mainColor = Color.parseColor("#1976D2"); // Blue
                    backgroundColor = Color.parseColor("#E3F2FD");
                    break;
                case "CANCELLED":
                    mainColor = Color.parseColor("#D32F2F"); // Red
                    backgroundColor = Color.parseColor("#FFEBEE");
                    break;
                case "MISSED":
                    mainColor = Color.parseColor("#ED6C02"); // Orange
                    backgroundColor = Color.parseColor("#FFF3E0");
                    break;
                default:
                    mainColor = Color.parseColor("#616161"); // Grey
                    backgroundColor = Color.parseColor("#F5F5F5");
                    break;
            }
        } catch (IllegalArgumentException e) {
            // Fallback colors if parsing fails
            mainColor = Color.GRAY;
            backgroundColor = Color.LTGRAY;
        }

        // Apply colors to the Material Components
        holder.statusBadge.setCardBackgroundColor(backgroundColor);
        holder.tvStatusTag.setTextColor(mainColor);
        holder.statusIndicator.setBackgroundColor(mainColor);
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    /**
     * ViewHolder Pattern: Holds references to the views.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDoctorName, tvPatientName, tvDate, tvTime, tvStatusTag, btnviewDetail;
        MaterialCardView statusBadge;
        View statusIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDoctorName = itemView.findViewById(R.id.tvDoctorName);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatusTag = itemView.findViewById(R.id.tvStatusTag);
            statusBadge = itemView.findViewById(R.id.statusBadge);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            btnviewDetail = itemView.findViewById(R.id.btnviewDetail);
        }
    }
}