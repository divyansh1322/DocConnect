package com.example.docconnect;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class AdminComplaintAdapter extends RecyclerView.Adapter<AdminComplaintAdapter.ComplaintViewHolder> {
    private Context context;
    private List<AdminComplaintModel> complaintList;

    public AdminComplaintAdapter(Context context, List<AdminComplaintModel> complaintList) {
        this.context = context;
        this.complaintList = complaintList;
    }

    @NonNull
    @Override
    public ComplaintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_admin_complaints, parent, false);
        return new ComplaintViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ComplaintViewHolder holder, int position) {
        AdminComplaintModel complaint = complaintList.get(position);
        holder.tvFullName.setText(complaint.getFullName());
        holder.tvCategory.setText(complaint.getCategory());
        holder.tvDate.setText(complaint.getDate());
        holder.tvDescription.setText(complaint.getDescription());

        String status = (complaint.getStatus() == null) ? "New" : complaint.getStatus();
        holder.tvStatus.setText(status.toUpperCase());

        holder.btnView.setOnClickListener(v -> {
            Intent i = new Intent(context, AdminUDComplaintActivity.class);
            i.putExtra("ticketId", complaint.getTicketId());
            i.putExtra("userId", complaint.getUserId());
            i.putExtra("userType", complaint.getUserType()); // Path: users or doctors
            i.putExtra("nodeKey", complaint.getNodeKey());   // Path: report_issue or support_tickets
            context.startActivity(i);
        });
    }

    @Override
    public int getItemCount() { return complaintList.size(); }

    static class ComplaintViewHolder extends RecyclerView.ViewHolder {
        TextView tvFullName, tvCategory, tvDate, tvStatus, tvDescription;
        Button btnView;
        public ComplaintViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFullName = itemView.findViewById(R.id.tfullName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnView = itemView.findViewById(R.id.btnView);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}