package com.example.docconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for displaying medical symptoms in a RecyclerView.
 * This class binds SymptomModel data to the item_symptom layout and handles click events.
 */
public class SymptomAdapter extends RecyclerView.Adapter<SymptomAdapter.ViewHolder> {

    private List<SymptomModel> list;
    private OnItemClickListener listener;

    /**
     * Interface to handle clicks on symptom items.
     * Communicates the selected symptom title back to the Fragment or Activity.
     */
    public interface OnItemClickListener {
        void onItemClick(String symptomName);
    }

    /**
     * Constructor for the SymptomAdapter.
     * @param list     List of SymptomModel objects to display.
     * @param listener Callback for item click events.
     */
    public SymptomAdapter(List<SymptomModel> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for an individual symptom item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_symptom, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SymptomModel item = list.get(position);

        // Bind the symptom title to the TextView
        holder.tvName.setText(item.getTitle());

        // Bind the symptom icon/image resource to the ImageView
        holder.imgIcon.setImageResource(item.getImage());

        // Setup the click listener for the entire item view
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item.getTitle());
            }
        });
    }

    @Override
    public int getItemCount() {
        // Return the size of the symptom list
        return list != null ? list.size() : 0;
    }

    /**
     * ViewHolder class that holds references to the views for each data item.
     * This avoids repeated calls to findViewById and improves performance.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView imgIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Matches android:id="@+id/tvSymptomName" in item_symptom.xml
            tvName = itemView.findViewById(R.id.tvSymptomName);
            // Matches android:id="@+id/imgSymptom" in item_symptom.xml
            imgIcon = itemView.findViewById(R.id.imgSymptom);
        }
    }
}