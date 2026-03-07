package com.example.docconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryGridAdapter
        extends RecyclerView.Adapter<CategoryGridAdapter.ViewHolder> {

    private List<CategoryModel> itemList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(CategoryModel item);
    }

    public CategoryGridAdapter(List<CategoryModel> itemList,
                               OnItemClickListener listener) {
        this.itemList = itemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_speciality, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryModel model = itemList.get(position);

        holder.tvName.setText(model.getName());
        holder.imgIcon.setImageResource(model.getIconResId());
        holder.imgIcon.setContentDescription(model.getName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(model);
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgIcon);
            tvName = itemView.findViewById(R.id.tvSpecialityName);
        }
    }
}
