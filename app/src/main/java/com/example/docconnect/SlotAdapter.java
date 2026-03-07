package com.example.docconnect;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying a grid or list of available time slots.
 * Implements a single-selection mechanism to highlight the chosen appointment time.
 */
public class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.ViewHolder> {

    private List<SlotsModel> list;
    private final OnSlotSelected callback;
    private int selectedPos = -1; // Tracks which slot is currently highlighted

    /**
     * Interface to communicate slot selection events back to the parent Fragment/Activity.
     */
    public interface OnSlotSelected {
        void onSlotSelected(SlotsModel slot);
    }

    public SlotAdapter(List<SlotsModel> list, OnSlotSelected cb) {
        this.list = list;
        this.callback = cb;
    }

    /**
     * Replaces the current data set with a new list (e.g., when a new date is selected).
     * Resets the selection state to ensure no hidden highlights remain.
     */
    public void updateList(List<SlotsModel> newList) {
        this.list = (newList != null) ? newList : new ArrayList<>();
        this.selectedPos = -1; // Reset selection on data refresh
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single time slot (usually a stylized TextView)
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_slots, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SlotsModel s = list.get(position);

        // ✅ Displays the formatted time string (e.g., "10:30 AM") from the model
        holder.tvTimeSlot.setText(s.getTimeDisplay());

        // --- SELECTION LOGIC UI ---
        // Provides immediate visual feedback when a user taps a slot
        if (selectedPos == position) {
            // Selected state: Primary theme color (Blue) background with White text
            holder.tvTimeSlot.setBackgroundResource(R.drawable.bg_selected_day);
            holder.tvTimeSlot.setTextColor(Color.WHITE);
        } else {
            // Default state: Light gray/white background with Dark Slate text (#0F172A)
            holder.tvTimeSlot.setBackgroundResource(R.drawable.bg_time_slot_default);
            holder.tvTimeSlot.setTextColor(Color.parseColor("#0F172A"));
        }

        // --- CLICK LISTENER ---
        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPos;
            selectedPos = holder.getAdapterPosition();

            // Optimization: Only refresh the specific items that changed state
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPos);

            // Pass the selected object back through the callback
            if (callback != null) {
                callback.onSlotSelected(s);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    /**
     * ViewHolder for caching view references.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimeSlot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Links to the TextView defined in item_time_slots.xml
            tvTimeSlot = itemView.findViewById(R.id.tvTimeSlot);
        }
    }
}