package com.example.docconnect;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

/**
 * Adapter for displaying a history of completed consultations.
 * Reuses the DoctorUpcomingModel but focuses on past dates and finalized session data.
 */
public class PastConsultantAdapter extends RecyclerView.Adapter<PastConsultantAdapter.ViewHolder> {

    private List<DoctorUpcomingModel> historyList;
    private Context context;

    public PastConsultantAdapter(List<DoctorUpcomingModel> historyList) {
        this.historyList = historyList;
    }

    /**
     * Refreshes the adapter with a new set of data (e.g., after a search or filter).
     */
    public void updateList(List<DoctorUpcomingModel> newList) {
        this.historyList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        // Inflates the specialized "Past Consultants" card layout
        View view = LayoutInflater.from(context).inflate(R.layout.item_past_consultants, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DoctorUpcomingModel model = historyList.get(position);

        // 1. Data Binding: Fetching sanitized strings from the model
        holder.tvName.setText(model.getPatientName());

        // 2. Format UI Strings: Combining Age and Gender into a single line for clean UX
        String details = String.format("%s yrs | %s", model.getPatientAge(), model.getPatientGender());
        holder.tvDetails.setText(details);

        holder.tvDate.setText(model.getDate());

        // 3. Navigation Logic: Accessing specific patient records
        holder.btnViewProfile.setOnClickListener(v -> {
            String patientId = model.getPatientId();

            // Placeholder for viewing historical patient charts or prescriptions
            Toast.makeText(context, "Opening profile of " + model.getPatientName(), Toast.LENGTH_SHORT).show();

            /* Intent intent = new Intent(context, PatientDetailsActivity.class);
            intent.putExtra("patientId", patientId);
            context.startActivity(intent);
            */
        });
    }

    @Override
    public int getItemCount() {
        return historyList != null ? historyList.size() : 0;
    }

    /**
     * ViewHolder pattern for efficient UI element caching.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDetails, tvDate;
        MaterialButton btnViewProfile;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Linking IDs from item_past_consultants.xml
            tvName = itemView.findViewById(R.id.tvPastPatientName);
            tvDetails = itemView.findViewById(R.id.tvPastDetails);
            tvDate = itemView.findViewById(R.id.tvPastDate);
            btnViewProfile = itemView.findViewById(R.id.btnViewProfile);
        }
    }
}