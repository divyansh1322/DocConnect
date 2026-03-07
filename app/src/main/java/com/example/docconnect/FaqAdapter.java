package com.example.docconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FaqAdapter extends RecyclerView.Adapter<FaqAdapter.FaqViewHolder> {

    private final List<FaqModel> faqList;

    public FaqAdapter(List<FaqModel> faqList) {
        this.faqList = faqList;
    }

    @NonNull
    @Override
    public FaqViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FaqViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_faq, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FaqViewHolder holder, int position) {
        FaqModel faq = faqList.get(position);
        holder.tvQuestion.setText(faq.getQuestion());
        holder.tvAnswer.setText(faq.getAnswer());

        // Expansion Logic
        holder.tvAnswer.setVisibility(faq.isExpanded() ? View.VISIBLE : View.GONE);
        holder.ivArrow.setRotation(faq.isExpanded() ? 180 : 0);

        holder.itemView.setOnClickListener(v -> {
            boolean expanded = faq.isExpanded();
            faq.setExpanded(!expanded);
            notifyItemChanged(position); // Efficient refresh logic
        });
    }

    @Override
    public int getItemCount() { return faqList.size(); }

    static class FaqViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion, tvAnswer;
        ImageView ivArrow;
        FaqViewHolder(View v) {
            super(v);
            tvQuestion = v.findViewById(R.id.tvQuestion);
            tvAnswer = v.findViewById(R.id.tvAnswer);
            ivArrow = v.findViewById(R.id.ivArrow);
        }
    }
}