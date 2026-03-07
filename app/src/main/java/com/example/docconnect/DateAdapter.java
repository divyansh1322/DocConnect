package com.example.docconnect;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * Adapter for a horizontal calendar strip.
 * It manages a list of dates and handles the visual "selected" state
 * to provide feedback when a user chooses an appointment day.
 */
public class DateAdapter extends RecyclerView.Adapter<DateAdapter.ViewHolder> {

    // Data source: List of dates formatted as "yyyy-MM-dd"
    private List<String> dates;
    // Interface callback to send the selected date back to the Activity
    private final OnDateSelected callback;
    // Keeps track of which index is currently highlighted
    private int selectedPosition = 0;

    /**
     * Interface used to notify the parent Activity/Fragment when a date is clicked.
     */
    public interface OnDateSelected {
        void onDateSelected(String date);
    }

    /**
     * Constructor for DateAdapter.
     * @param dates List of date strings to display.
     * @param cb Implementation of the OnDateSelected interface.
     */
    public DateAdapter(List<String> dates, OnDateSelected cb) {
        this.dates = dates;
        this.callback = cb;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the custom item layout for a single calendar date
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calender_date, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the full date string (e.g., "2026-02-14")
        String fullDate = dates.get(position);

        // UI Parsing: Extract "dd" (the day) from "yyyy-MM-dd" for the display
        String[] parts = fullDate.split("-");
        holder.tvDate.setText(parts.length == 3 ? parts[2] : fullDate);

        // --- Selection Logic: Update the CardView and TextView based on the state ---
        if (position == selectedPosition) {
            // SELECTED STATE: Highlight with primary color and white text
            holder.cardDate.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.doc_primary));
            holder.tvDate.setTextColor(Color.WHITE);
            holder.cardDate.setCardElevation(12f); // Increased elevation to make it look active
        } else {
            // DEFAULT STATE: Neutral background and heading text color
            holder.cardDate.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.doc_surface));
            holder.tvDate.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.doc_text_heading));
            holder.cardDate.setCardElevation(2f);
        }

        // --- Interaction Logic ---
        holder.itemView.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                // Update the tracked position
                selectedPosition = currentPos;
                // notifyDataSetChanged() triggers a re-bind of all items to refresh background colors
                notifyDataSetChanged();
                // Send the selected date string to the interface implementation
                callback.onDateSelected(fullDate);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dates != null ? dates.size() : 0;
    }

    /**
     * ViewHolder holds references to the views within item_calender_date.xml
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        CardView cardDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Links the TextView for the day number and the CardView for the background container
            tvDate = itemView.findViewById(R.id.tvDate);
            cardDate = itemView.findViewById(R.id.cardDate);
        }
    }
}