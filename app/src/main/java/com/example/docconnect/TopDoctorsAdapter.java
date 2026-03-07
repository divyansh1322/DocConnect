package com.example.docconnect;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the Top Doctors list.
 * Implements Filterable to allow real-time searching through specialties.
 * Dynamically changes UI colors based on doctor availability status.
 */
public class TopDoctorsAdapter extends RecyclerView.Adapter<TopDoctorsAdapter.ViewHolder> implements Filterable {

    private Context context;
    private List<TopDoctorsModel> doctorList;
    private List<TopDoctorsModel> doctorListFull; // Copy of the list for filtering purposes

    public TopDoctorsAdapter(Context context, List<TopDoctorsModel> doctorList) {
        this.context = context;
        this.doctorList = doctorList;
        // Keep a full copy of the data to restore the list after filtering
        this.doctorListFull = new ArrayList<>(doctorList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout for a single doctor card
        View view = LayoutInflater.from(context).inflate(R.layout.item_top_doctors, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TopDoctorsModel model = doctorList.get(position);

        // Set text and image data from the model
        holder.tvSpeciality.setText(model.getSpeciality());
        holder.tvAvailability.setText(model.getAvailability());
        holder.imgDoctor.setImageResource(model.getImageResId());

        // ============================================================
        // 🔴🟢 DYNAMIC COLOR LOGIC (Availability Status)
        // ============================================================

        String statusText = model.getAvailability().toLowerCase();

        // Hex colors for status indicators
        int colorGreen = Color.parseColor("#4CAF50"); // Standard Green
        int colorRed = Color.parseColor("#F44336");   // Standard Red

        // Use BackgroundTintList to change the color of the status badge/TextView
        if (statusText.contains("not available")) {
            // Set background tint to RED if doctor is unavailable
            holder.tvAvailability.setBackgroundTintList(ColorStateList.valueOf(colorRed));
        } else {
            // Set background tint to GREEN if doctor is available
            holder.tvAvailability.setBackgroundTintList(ColorStateList.valueOf(colorGreen));
        }

        // ============================================================

        // Handle item clicks with a simple Toast (Can be updated to navigate to Profile)
        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(context, "Clicked: " + model.getSpeciality(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return doctorList != null ? doctorList.size() : 0;
    }

    // --- SEARCH/FILTER LOGIC ---

    @Override
    public Filter getFilter() {
        return exampleFilter;
    }

    /**
     * Filter logic that compares the search query against the doctor specialty.
     */
    private Filter exampleFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<TopDoctorsModel> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                // If search query is empty, show the full list
                filteredList.addAll(doctorListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                // Match against specialties
                for (TopDoctorsModel item : doctorListFull) {
                    if (item.getSpeciality().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            // Update the main list and refresh the UI
            doctorList.clear();
            doctorList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    /**
     * ViewHolder class for caching view references within the doctor card.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSpeciality, tvAvailability;
        ImageView imgDoctor;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Links to XML IDs in item_top_doctors.xml
            tvSpeciality = itemView.findViewById(R.id.tvSpeciality);
            tvAvailability = itemView.findViewById(R.id.tvAvailability);
            imgDoctor = itemView.findViewById(R.id.imgDoctor);
        }
    }
}