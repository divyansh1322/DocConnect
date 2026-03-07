package com.example.docconnect;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

/**
 * Adapter for the Doctor's Chat/Appointment list.
 * This class handles complex UI states (Active, Completed, Cancelled, etc.)
 * to provide the Doctor with immediate visual feedback on appointment status.
 */
public class DoctorChatAdapter extends RecyclerView.Adapter<DoctorChatAdapter.ChatViewHolder> {

    private final List<DoctorChatModel> chatList;
    private final Context context;
    private final OnChatClickListener listener;

    /**
     * Interface to handle interactions from the Activity/Fragment level.
     */
    public interface OnChatClickListener {
        void onStartConsultation(DoctorChatModel model);
        void onDetailsClick(DoctorChatModel model);
    }

    public DoctorChatAdapter(List<DoctorChatModel> chatList, Context context, OnChatClickListener listener) {
        this.chatList = chatList;
        this.context = context;
        this.listener = listener;
    }

    /**
     * Inflates the card layout for each individual appointment item.
     */
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_doctor_chat_card, parent, false);
        return new ChatViewHolder(view);
    }

    /**
     * The core logic engine. It binds data to the views and applies conditional
     * styling based on the status of the appointment.
     */
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        DoctorChatModel model = chatList.get(position);

        // --- 1. DATA BINDING ---
        // Populating basic text fields from the model
        holder.tvName.setText(model.getPatientName());
        holder.tvAge.setText(model.getPatientAge());
        holder.tvGender.setText(model.getPatientGender());
        holder.tvDate.setText(model.getDate());
        holder.tvTime.setText(model.getTime());

        // --- 2. VIEW RECYCLING PROTECTION ---
        // Crucial: Clear any previous state before applying new ones to prevent
        // "ghost" UI bugs when scrolling.
        resetUI(holder);

        // --- 3. STATUS LOGIC (UI STATE MACHINE) ---
        String status = model.getStatus().toUpperCase();
        holder.tvstatusBadge.setText(status);

        switch (status) {
            case "ACTIVE":
                // State: Consultation is happening NOW
                holder.mainCard.setStrokeColor(Color.parseColor("#4CAF50")); // Green border
                holder.tvstatusBadge.setBackgroundResource(R.drawable.bg_success_pill);
                holder.tvstatusBadge.setTextColor(Color.parseColor("#4CAF50"));

                holder.btnStart.setText("Enter Chat");
                holder.btnStart.setVisibility(View.VISIBLE);
                holder.btnStart.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                holder.btnStart.setStrokeWidth(0);
                holder.btnDetails.setVisibility(View.GONE);
                break;

            case "UPCOMING":
                // State: Appointment is scheduled for the future
                holder.mainCard.setStrokeColor(ContextCompat.getColor(context, R.color.doc_divider));
                holder.tvstatusBadge.setBackgroundResource(R.drawable.bg_upcoming_pill);
                holder.tvstatusBadge.setTextColor(ContextCompat.getColor(context, R.color.doc_primary));

                holder.btnStart.setText("Start Consultation");
                holder.btnStart.setVisibility(View.VISIBLE);
                holder.btnDetails.setVisibility(View.GONE);

                // Clean up button styles for consistency
                holder.btnDetails.setSupportBackgroundTintList(null);
                holder.btnDetails.setTextColor(ContextCompat.getColor(context, R.color.doc_primary));
                break;

            case "COMPLETED":
                // 1. Static UI Styling
                holder.mainCard.setStrokeColor(Color.LTGRAY);
                holder.tvstatusBadge.setText("COMPLETED");
                holder.tvstatusBadge.setBackgroundResource(R.drawable.bg_completed_pill);
                holder.tvstatusBadge.setTextColor(Color.GRAY);
                // 2. Button Visibility Logic
                holder.btnStart.setVisibility(View.GONE);
                holder.btnDetails.setVisibility(View.VISIBLE);
                // 3. Button Customization (Safe Implementation)
                holder.btnDetails.setText("View Chat");
                // Logic: Use a null-safe check for the drawable
                Drawable chatIcon = ContextCompat.getDrawable(context, R.drawable.ic_chat);
                if (chatIcon != null) {
                    holder.btnDetails.setIcon(chatIcon);
                    holder.btnDetails.setIconTint(ColorStateList.valueOf(Color.GRAY));
                }
                int bgColor = ContextCompat.getColor(context, R.color.completed_btn_bg);
                holder.btnDetails.setSupportBackgroundTintList(ColorStateList.valueOf(bgColor));

                holder.btnDetails.setTextColor(Color.GRAY);
                holder.btnDetails.setStrokeWidth(0); // Flat look for history
                break;

                case "CANCELLED":
                // State: Appointment voided. Red/Neutral theme.
                holder.mainCard.setStrokeColor(Color.parseColor("#EF5350"));
                holder.tvstatusBadge.setBackgroundResource(R.drawable.bg_cancelled_pill);
                holder.tvstatusBadge.setTextColor(Color.parseColor("#D32F2F"));

                // Disable all action buttons for cancelled items
                holder.btnStart.setVisibility(View.GONE);
                holder.btnDetails.setVisibility(View.GONE);
                break;

            case "MISSED":
                // State: Doctor or Patient did not show up.
                holder.mainCard.setStrokeColor(Color.RED);
                holder.tvstatusBadge.setTextColor(Color.RED);
                holder.tvstatusBadge.setBackgroundResource(R.drawable.bg_warning_pill);

                holder.btnStart.setVisibility(View.GONE);
                holder.btnDetails.setVisibility(View.GONE);
                break;
        }

        // --- 4. IMAGE LOADING ---
        // Load patient profile picture using Glide
        Glide.with(context)
                .load(model.getPatientImage())
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .into(holder.imgPatient);

        // --- 5. NAVIGATION & CLICKS ---

        // Handle "Start/Enter Chat"
        holder.btnStart.setOnClickListener(v -> {
            // 1. Update the local data model immediately
            model.setStatus("ACTIVE");

            // 2. Notify the adapter that this specific item changed
            // This triggers a re-bind, which will now hit the "ACTIVE" case in your switch
            notifyItemChanged(holder.getBindingAdapterPosition());

            // 3. Proceed with your existing logic (navigation/API call)
            listener.onStartConsultation(model);
        });

        // Handle "View Chat" or "Details"
        holder.btnDetails.setOnClickListener(v -> {
            if ("COMPLETED".equals(status)) {
                // If completed, navigating here allows doctor to read old messages
                Intent intent = new Intent(context, DUChatsActivity.class);
                intent.putExtra("CHAT_DATA", model);
                context.startActivity(intent);
            } else {
                // General purpose callback for other states
                listener.onDetailsClick(model);
            }
        });
    }

    /**
     * Resets the views to their default state.
     * Since RecyclerView re-uses view objects, failure to reset will cause
     * "Completed" styling to appear on "Active" cards during scrolling.
     */
    private void resetUI(ChatViewHolder holder) {
        holder.btnStart.setVisibility(View.GONE);
        holder.btnDetails.setVisibility(View.GONE);
        holder.btnDetails.setIcon(null);
        holder.btnDetails.setStrokeWidth(3); // Reset to default outlined width
        holder.mainCard.setStrokeWidth(3);
    }

    @Override
    public int getItemCount() {
        return chatList != null ? chatList.size() : 0;
    }

    /**
     * ViewHolder pattern to cache View references for performance.
     */
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView mainCard;
        ShapeableImageView imgPatient;
        TextView tvName, tvAge, tvGender, tvDate, tvTime, tvstatusBadge;
        MaterialButton btnStart, btnDetails;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            mainCard = (MaterialCardView) itemView;
            imgPatient = itemView.findViewById(R.id.patientImg);
            tvName = itemView.findViewById(R.id.patientName);
            tvAge = itemView.findViewById(R.id.patient_age);
            tvGender = itemView.findViewById(R.id.patient_gender);
            tvDate = itemView.findViewById(R.id.consult_date);
            tvTime = itemView.findViewById(R.id.consult_time);
            tvstatusBadge = itemView.findViewById(R.id.tvstatusBadge);
            btnStart = itemView.findViewById(R.id.btn_start);
            btnDetails = itemView.findViewById(R.id.btnDetails);
        }
    }
}