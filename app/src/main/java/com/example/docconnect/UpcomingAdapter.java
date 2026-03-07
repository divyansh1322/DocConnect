package com.example.docconnect;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Adapter for displaying appointment cards with dynamic states.
 * Handles UI changes for 'Upcoming', 'Cancelled', and 'Completed' appointments.
 */
public class UpcomingAdapter extends RecyclerView.Adapter<UpcomingAdapter.ViewHolder> {

    private Context context;
    private List<AppointmentModel> list;
    private OnAppointmentActionListener listener;

    /**
     * Interface to handle button clicks within the item.
     * Decouples logic from the adapter to the Activity/Fragment.
     */
    public interface OnAppointmentActionListener {
        void onCancelClick(AppointmentModel appointment, int position);
        void onRescheduleClick(AppointmentModel appointment);
        void onRateReviewClick(AppointmentModel appointment);
        void onReasonClick(AppointmentModel appointment);
        void onBookNowClick(AppointmentModel appointment);
    }

    public UpcomingAdapter(Context context, List<AppointmentModel> list,
                           OnAppointmentActionListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the XML layout for individual items
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_appointment_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppointmentModel item = list.get(position);

        // Basic data binding
        holder.tvDoctorName.setText(item.getDoctorName());
        holder.tvSpecialty.setText(item.getSpecialty());
        holder.tvDate.setText(item.getDate());
        holder.tvBookingId.setText(item.getBookingId());
        holder.tvTime.setText(item.getTime());
        holder.tvFee.setText("₹" + item.getDoctorFee());

        // Loading doctor image with Glide; circleCrop ensures a professional profile look
        Glide.with(context)
                .load(item.getDoctorImage())
                .placeholder(R.drawable.ic_person)
                .circleCrop()
                .into(holder.imgDoctor);

        // Handle null safety for status and default to UPCOMING
        String status = item.getStatus() != null
                ? item.getStatus().toUpperCase()
                : "UPCOMING";

        holder.tvStatus.setText(status);

        // --- DYNAMIC UI STYLING BASED ON STATUS ---
        // Sets the background tint and text color for the status badge
        if ("UPCOMING".equals(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#10B981")); // Emerald Green
            ViewCompat.setBackgroundTintList(holder.tvStatus, ColorStateList.valueOf(Color.parseColor("#E1F5FE")));

        } else if ("CANCELLED".equals(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#F44336")); // Red
            ViewCompat.setBackgroundTintList(holder.tvStatus, ColorStateList.valueOf(Color.parseColor("#FFEBEE")));

        } else if ("COMPLETED".equals(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32")); // Dark Green
            ViewCompat.setBackgroundTintList(holder.tvStatus, ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
        }

        // Logic for configuring buttons based on the appointment state
        setupButtons(holder, item, position, status);
    }

    /**
     * Configures the text and click listeners for the two action buttons.
     * This avoids cluttering onBindViewHolder.
     */
    private void setupButtons(ViewHolder h,
                              AppointmentModel item,
                              int position,
                              String status) {

        if ("UPCOMING".equals(status)) {
            h.btnLeft.setText("Cancel");
            h.btnLeft.setOnClickListener(v -> listener.onCancelClick(item, position));

            h.btnRight.setText("Reschedule");
            h.btnRight.setOnClickListener(v -> listener.onRescheduleClick(item));

        } else if ("CANCELLED".equals(status)) {
            h.btnLeft.setText("Reason");
            h.btnRight.setText("Book Now");

            h.btnLeft.setOnClickListener(v -> listener.onReasonClick(item));
            h.btnRight.setOnClickListener(v -> listener.onBookNowClick(item));

        } else if ("COMPLETED".equals(status)) {
            h.btnLeft.setText("Rate");
            h.btnRight.setText("Review");

            // Both buttons currently trigger the same Rate/Review action
            h.btnLeft.setOnClickListener(v -> listener.onRateReviewClick(item));
            h.btnRight.setOnClickListener(v -> listener.onRateReviewClick(item));
        }
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    /**
     * ViewHolder holds references to the views to avoid repeated findViewById() calls.
     * This significantly improves scrolling performance.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvDoctorName, tvSpecialty, tvDate, tvTime, tvStatus, tvFee, tvBookingId;
        ImageView imgDoctor;
        MaterialButton btnLeft, btnRight;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvDoctorName = itemView.findViewById(R.id.tvDoctorName);
            tvSpecialty = itemView.findViewById(R.id.tvSpecialty);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvFee = itemView.findViewById(R.id.tvFee);
            tvBookingId = itemView.findViewById(R.id.tvBookingId);
            imgDoctor = itemView.findViewById(R.id.imgDoctor);

            // Map the layout buttons to the generic btnLeft/btnRight variables
            btnLeft = itemView.findViewById(R.id.btnCancelBooking);
            btnRight = itemView.findViewById(R.id.btnViewBooking);
        }
    }
}