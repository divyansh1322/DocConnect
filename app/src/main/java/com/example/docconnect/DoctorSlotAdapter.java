package com.example.docconnect;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Enhanced Adapter for DocConnect Slot Management.
 * Handles: Real-time status updates, Expiry logic, and Delete callbacks.
 */
public class DoctorSlotAdapter extends RecyclerView.Adapter<DoctorSlotAdapter.ViewHolder> {

    private final List<DoctorSlotModel> slotList;
    private OnSlotDeleteListener deleteListener;

    // Interface to communicate back to Fragment/Activity
    public interface OnSlotDeleteListener {
        void onDeleteRequested(int position, DoctorSlotModel slot);
    }

    public DoctorSlotAdapter(List<DoctorSlotModel> slotList, OnSlotDeleteListener deleteListener) {
        this.slotList = slotList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_generated_slots, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DoctorSlotModel slot = slotList.get(position);

        // 1. Set Labels
        holder.tvDate.setText(slot.getDateLabel());
        holder.tvTime.setText(slot.getTimeDisplay());

        // 2. Handle Expiry and Status Logic
        long now = System.currentTimeMillis();
        String status = slot.getStatus();

        if (slot.getEndMillis() < now) {
            holder.tvStatus.setText("EXPIRED");
            holder.tvStatus.setTextColor(Color.GRAY);
        } else {
            holder.tvStatus.setText(status);
            // Apply status colors
            if ("AVAILABLE".equalsIgnoreCase(status)) {
                holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
            } else if ("BOOKED".equalsIgnoreCase(status)) {
                holder.tvStatus.setTextColor(Color.parseColor("#C62828"));
            } else {
                holder.tvStatus.setTextColor(Color.DKGRAY);
            }
        }

        // 3. Delete Button Click (Immediate response)
        holder.btnDelete.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos != RecyclerView.NO_POSITION && deleteListener != null) {
                deleteListener.onDeleteRequested(adapterPos, slot);
            }
        });
    }

    @Override
    public int getItemCount() {
        return slotList != null ? slotList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTime, tvStatus;
        MaterialButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvSlotDate);
            tvTime = itemView.findViewById(R.id.tvSlotTime);
            tvStatus = itemView.findViewById(R.id.tvSlotStatus);
            btnDelete = itemView.findViewById(R.id.btnDeleteSlot);
        }
    }
}