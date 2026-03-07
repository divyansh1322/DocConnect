package com.example.docconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * DoctorUpcomingAdapter: Professional, Null-Safe implementation.
 * FIXED: NullPointerException on setText by adding view and data validation.
 * FIXED: Potential crashes during time calculation.
 */
public class DoctorUpcomingAdapter extends RecyclerView.Adapter<DoctorUpcomingAdapter.ViewHolder> {

    private List<DoctorUpcomingModel> appointmentList;

    public DoctorUpcomingAdapter(List<DoctorUpcomingModel> appointmentList) {
        // A2Z: Ensure list is never null to avoid getItemCount crashes
        this.appointmentList = appointmentList != null ? appointmentList : new ArrayList<>();
    }

    public void updateList(List<DoctorUpcomingModel> newList) {
        this.appointmentList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Ensure item_appointment_row.xml is the correct name in your layout folder
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DoctorUpcomingModel model = appointmentList.get(position);
        if (model == null) return;

        // --- NULL-SAFE DATA BINDING ---
        // We check if the TextView exists in XML AND if the data exists in Model

        if (holder.tvTime != null) {
            holder.tvTime.setText(model.getTime() != null ? model.getTime() : "--:--");
        }

        if (holder.tvName != null) {
            holder.tvName.setText(model.getPatientName() != null ? model.getPatientName() : "Unknown Patient");
        }

        if (holder.tvGender != null) {
            holder.tvGender.setText(model.getPatientGender() != null ? model.getPatientGender() : "N/A");
        }

        if (holder.tvAge != null) {
            String age = model.getPatientAge() != null ? model.getPatientAge() : "0";
            holder.tvAge.setText(age + " Yrs");
        }

        // --- TIME LEFT CALCULATION ---
        if (holder.tvTimeLeft != null) {
            String timeLeftText = calculateTimeLeft(model.getTime());
            holder.tvTimeLeft.setText(timeLeftText + " left");
        }

        // --- STATUS STYLING ---
        if (holder.tvStatus != null) {
            holder.tvStatus.setText("WAITING");
            try {
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.doc_warning));
            } catch (Exception e) {
                // Fallback if color resource is missing
            }
        }
    }

    /**
     * calculateTimeLeft: Safely parses time strings to show countdown.
     */
    private String calculateTimeLeft(String appointmentTime) {
        if (appointmentTime == null || appointmentTime.isEmpty()) return "--";

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date appDate = sdf.parse(appointmentTime);
            if (appDate == null) return "--";

            Calendar now = Calendar.getInstance();
            Calendar appCal = Calendar.getInstance();
            appCal.setTime(appDate);

            // Set appointment to today's date
            appCal.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

            long diffInMinutes = (appCal.getTimeInMillis() - now.getTimeInMillis()) / (60 * 1000);

            if (diffInMinutes > 0) {
                if (diffInMinutes >= 60) {
                    return (diffInMinutes / 60) + "h " + (diffInMinutes % 60) + "m";
                }
                return diffInMinutes + " Mins";
            }
            return "Started";
        } catch (Exception e) {
            return "--";
        }
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    /**
     * ViewHolder: Maps Java objects to XML IDs.
     * CRITICAL: If any ID here is wrong, it will return NULL.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvName, tvGender, tvAge, tvStatus, tvTimeLeft;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Double check these IDs match your item_appointment_row.xml exactly
            tvTime = itemView.findViewById(R.id.tvAppointmentTime);
            tvTimeLeft = itemView.findViewById(R.id.tvTimeLeft);
            tvName = itemView.findViewById(R.id.tvPatientName);
            tvGender = itemView.findViewById(R.id.tvGender);
            tvAge = itemView.findViewById(R.id.tvAge);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}