package com.example.docconnect;

import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TermsAdapter extends RecyclerView.Adapter<TermsAdapter.ViewHolder> {

    private List<TermModel> termsList;
    private RecyclerView recyclerView;

    public TermsAdapter(List<TermModel> termsList, RecyclerView recyclerView) {
        this.termsList = termsList;
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_term, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TermModel term = termsList.get(position);
        holder.titleTv.setText(term.getTitle());
        holder.contentTv.setText(term.getContent());

        // Toggle visibility based on state
        holder.contentTv.setVisibility(term.isExpanded() ? View.VISIBLE : View.GONE);
        holder.arrowIv.setRotation(term.isExpanded() ? 180 : 0);

        holder.itemView.setOnClickListener(v -> {
            // Toggle state
            term.setExpanded(!term.isExpanded());

            // Animate the change
            TransitionManager.beginDelayedTransition(recyclerView);
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return termsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTv, contentTv;
        ImageView arrowIv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTv = itemView.findViewById(R.id.textTitle);
            contentTv = itemView.findViewById(R.id.textContent);
            arrowIv = itemView.findViewById(R.id.imageArrow);
        }
    }
}