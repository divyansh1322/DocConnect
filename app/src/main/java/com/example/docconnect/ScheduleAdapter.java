package com.example.docconnect;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

/**
 * Adapter for the Doctor's daily schedule view.
 * Navigation: Opens DoctorActivePatientActivity with required extras.
 */
public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private List<DoctorUpcomingModel> list;

    public ScheduleAdapter(List<DoctorUpcomingModel> list) {
        this.list = list;
    }

    public void updateList(List<DoctorUpcomingModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule_visit, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DoctorUpcomingModel model = list.get(position);

        // 1. Bind Basic Patient Demographics
        holder.tvTime.setText(model.getTime());
        holder.tvName.setText(model.getPatientName());
        String details = model.getPatientAge() + " Yrs • " + model.getPatientGender();
        holder.tvDetails.setText(details);

        // 2. Load Patient Image
        Glide.with(holder.itemView.getContext())
                .load(model.getPatientImage())
                .placeholder(R.drawable.ic_doctor)
                .circleCrop()
                .into(holder.ivPatientImage);

        // 3. Status Indicator Logic
        String status = (model.getStatus() != null) ? model.getStatus().toUpperCase() : "UPCOMING";
        int color;

        if (status.equals("COMPLETED")) {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.doc_success);
        } else if (status.equals("CANCELLED") || status.equals("MISSED")) {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.doc_error);
        } else {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.brand_blue);
        }
        holder.indicator.setBackgroundColor(color);

        // 4. UPDATED NAVIGATION: Open DoctorActivePatientActivity
        holder.itemView.setOnClickListener(v -> {
            String patientId = model.getPatientId();
            String currentDoctorId = FirebaseAuth.getInstance().getUid();

            if (patientId != null && !patientId.isEmpty() && currentDoctorId != null) {
                // Create Intent to move to DoctorActivePatientActivity
                Intent intent = new Intent(v.getContext(), DoctorActivePatientActivity.class);

                // Pass the required IDs as extras
                intent.putExtra("patientId", patientId);
                intent.putExtra("doctorId", currentDoctorId);

                // Start the Activity
                v.getContext().startActivity(intent);
            } else {
                Toast.makeText(v.getContext(), "Data incomplete to open profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvName, tvDetails;
        View indicator;
        ImageView ivPatientImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTimeLabel);
            tvName = itemView.findViewById(R.id.tvPatientName);
            tvDetails = itemView.findViewById(R.id.tvVisitDetails);
            indicator = itemView.findViewById(R.id.viewStatusIndicator);
            ivPatientImage = itemView.findViewById(R.id.ivPatientImage);
        }
    }
}