package com.example.docconnect;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PatientHistoryAdapter extends RecyclerView.Adapter<PatientHistoryAdapter.ViewHolder> {

    private final List<PatientHistoryModel> historyList;

    public PatientHistoryAdapter(List<PatientHistoryModel> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PatientHistoryModel model = historyList.get(position);

        // Set text first
        holder.tvDoctorName.setText(model.getDoctorName());
        holder.tvDate.setText(model.getDate());
        holder.tvTime.setText(model.getTime());
        holder.tvStatus.setText(model.getStatus().toUpperCase());

        // Define colors for each state
        int bgColor;
        int textColor;

        switch (model.getStatus().toUpperCase()) {
            case "COMPLETED":
                bgColor = Color.parseColor("#E8F8F5"); // Light Mint
                textColor = Color.parseColor("#27AE60"); // Deep Green
                break;

            case "CANCELLED":
                bgColor = Color.parseColor("#FDEDEC"); // Light Rose
                textColor = Color.parseColor("#C0392B"); // Deep Red
                break;

            case "MISSED":
                bgColor = Color.parseColor("#FEF9E7"); // Light Amber
                textColor = Color.parseColor("#F39C12"); // Deep Orange
                break;

            case "UPCOMING":
            default:
                bgColor = Color.parseColor("#EBF5FB"); // Light Sky
                textColor = Color.parseColor("#2980B9"); // Deep Blue
                break;
        }

        // Apply the premium badge style
        holder.tvStatus.setBackgroundResource(R.drawable.bg_status_badge); // Use the XML from previous step
        holder.tvStatus.getBackground().setTint(bgColor);
        holder.tvStatus.setTextColor(textColor);

        // Hide the old side-indicator if it's redundant now
        holder.statusIndicator.setVisibility(View.GONE);
    }
    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDoctorName, tvDate, tvTime, tvStatus;
        View statusIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDoctorName = itemView.findViewById(R.id.doctorName);
            tvDate = itemView.findViewById(R.id.hist_date);
            tvTime = itemView.findViewById(R.id.hist_time);
            tvStatus = itemView.findViewById(R.id.hist_status);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }
    }
}