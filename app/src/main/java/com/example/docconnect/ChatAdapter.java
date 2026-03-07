package com.example.docconnect;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

/**
 * ChatAdapter: Manages the Patient's consultation list.
 * Implements a "State Machine" UI approach where the card morphs based on
 * the appointment status (Active, Upcoming, Completed, Missed, Cancelled).
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatModel> chatList;

    public ChatAdapter(List<ChatModel> chatList) {
        this.chatList = chatList;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_card, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatModel model = chatList.get(position);
        Context context = holder.itemView.getContext();
        String status = (model.getStatus() != null) ? model.getStatus().toUpperCase() : "UPCOMING";

        // 1. BASIC DATA BINDING
        holder.tvDoctorName.setText(model.getDoctorName());
        holder.tvSpecialty.setText(model.getDoctorSpecialty());
        holder.tvDate.setText(model.getDate());
        holder.tvtime.setText(model.getTime());

        // 2. RESET VIEW STATES (Prevents UI bugs during scrolling)
        holder.btnViewDetails.setVisibility(View.VISIBLE);
        holder.onlineContainer.setVisibility(View.GONE);
        holder.dateTimeContainer.setVisibility(View.VISIBLE);

        // 3. UI STATE MACHINE LOGIC
        switch (status) {
            case "ACTIVE":
                // Live Session: Highlight Green, show "Live" indicator, enable Chat
                holder.onlineContainer.setVisibility(View.VISIBLE);
                holder.dateTimeContainer.setVisibility(View.GONE);

                holder.tvStatusBadge.setText("ACTIVE");
                holder.tvStatusBadge.setTextColor(Color.parseColor("#2E7D32"));
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_success);

                holder.btnViewDetails.setText("Enter Chat");
                holder.btnViewDetails.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_chat));

                // 1. Apply the general style first
                applyButtonStyle(holder.btnViewDetails, context, R.color.doc_primary, false);

                // 2. Override with Green specifically for this case
                int greenColor = Color.parseColor("#4CAF50");
                holder.btnViewDetails.setBackgroundTintList(ColorStateList.valueOf(greenColor));
                break;

            case "UPCOMING":
                // Future Session: Show Primary color, hide button (cannot chat yet)
                holder.tvStatusBadge.setText("UPCOMING");
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.doc_primary));
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_upcoming_pill);
                holder.btnViewDetails.setVisibility(View.GONE);
                break;

            case "COMPLETED":
                // Past Session: Gray theme, allow reading history
                holder.tvStatusBadge.setText("COMPLETED");
                holder.tvStatusBadge.setTextColor(Color.GRAY);
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_completed_pill);

                holder.btnViewDetails.setText("View Chat");
                holder.btnViewDetails.setIcon(null);
                applyButtonStyle(holder.btnViewDetails, context, android.R.color.darker_gray, true);
                break;

            case "MISSED":
                // Missed Session: Red theme, trigger Reschedule flow
                holder.tvStatusBadge.setText("MISSED");
                holder.tvStatusBadge.setTextColor(Color.parseColor("#D32F2F"));
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_warning_pill);

                holder.btnViewDetails.setText("Reschedule");
                holder.btnViewDetails.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_calendar));
                applyButtonStyle(holder.btnViewDetails, context, android.R.color.holo_red_dark, true);
                break;

            case "CANCELLED":
                // Cancelled Session: Neutral theme, allow rebooking
                holder.tvStatusBadge.setText("CANCELLED");
                holder.tvStatusBadge.setTextColor(Color.BLACK);
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_cancelled_pill);

                holder.btnViewDetails.setText("Rebook Now");
                holder.btnViewDetails.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_info));
                applyButtonStyle(holder.btnViewDetails, context, android.R.color.black, true);
                break;
        }

        // 4. DOCTOR IMAGE LOADING
        Glide.with(context)
                .load(model.getDoctorImage())
                .placeholder(R.drawable.ic_doctor)
                .circleCrop()
                .into(holder.ivDoctor);

        // 5. INTELLIGENT NAVIGATION LOGIC
        holder.btnViewDetails.setOnClickListener(v -> {
            if (status.equals("MISSED") || status.equals("CANCELLED")) {
                // Route to Rebooking/Rescheduling flow
                Intent intent = new Intent(context, BookAppointmentActivity.class);
                intent.putExtra("doctorId", model.getDoctorId());
                intent.putExtra("doctorName", model.getDoctorName());
                intent.putExtra("doctorImage", model.getDoctorImage());
                intent.putExtra("clinicName", model.getClinicName());
                intent.putExtra("clinicAddress", model.getClinicAddress());
                intent.putExtra("doctorFee", model.getDoctorFee());
                intent.putExtra("doctorSpecialty", model.getDoctorSpecialty());
                context.startActivity(intent);
            } else {
                // Route to Chat flow (Active or Completed)
                Intent intent = new Intent(context, UDChatActivity.class);
                intent.putExtra("CHAT_DATA", model);
                context.startActivity(intent);
            }
        });
    }

    /**
     * Helper to toggle between Filled and Outlined Material Button styles programmatically.
     */
    private void applyButtonStyle(MaterialButton button, Context context, int colorRes, boolean isOutlined) {
        int color = ContextCompat.getColor(context, colorRes);
        if (isOutlined) {
            button.setBackgroundColor(Color.TRANSPARENT);
            button.setStrokeColor(ColorStateList.valueOf(color));
            button.setStrokeWidth(3);
            button.setTextColor(color);
            button.setIconTint(ColorStateList.valueOf(color));
        } else {
            button.setBackgroundColor(color);
            button.setStrokeWidth(0);
            button.setTextColor(Color.WHITE);
            button.setIconTint(ColorStateList.valueOf(Color.WHITE));
        }
    }

    @Override
    public int getItemCount() {
        return chatList != null ? chatList.size() : 0;
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvDoctorName, tvSpecialty, tvDate, tvtime, tvStatusBadge;
        ShapeableImageView ivDoctor;
        MaterialButton btnViewDetails;
        View dateTimeContainer, onlineContainer;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDoctorName = itemView.findViewById(R.id.tvDoctorName);
            tvSpecialty = itemView.findViewById(R.id.tvSpecialty);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvtime = itemView.findViewById(R.id.tvtime);
            tvStatusBadge = itemView.findViewById(R.id.statusBadge);
            ivDoctor = itemView.findViewById(R.id.ivDoctorScheduled);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            dateTimeContainer = itemView.findViewById(R.id.dateTimeContainer);
            onlineContainer = itemView.findViewById(R.id.onlineContainer);
        }
    }
}