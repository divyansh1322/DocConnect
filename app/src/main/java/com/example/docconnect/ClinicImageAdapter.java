package com.example.docconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Adapter class for displaying a list of clinic images in a RecyclerView.
 * This adapter handles image loading, item clicks, and deletion requests.
 */
public class ClinicImageAdapter extends RecyclerView.Adapter<ClinicImageAdapter.ViewHolder> {

    // List containing the image data (URLs and IDs)
    private List<ClinicImageModel> imageList;
    // Callback listener to handle user interactions outside of the adapter
    private OnImageClickListener listener;

    /**
     * Interface to handle clicks (Delete or View) back in the Activity/Fragment.
     * This decouples the UI logic from the data adapter.
     */
    public interface OnImageClickListener {
        // Triggered when the user clicks the delete (trash) icon
        void onDeleteClick(ClinicImageModel imageModel);
        // Triggered when the user clicks the image itself (e.g., to open full screen)
        void onImageClick(String imageUrl);
    }

    /**
     * Constructor for the adapter.
     * @param imageList The data source.
     * @param listener The implementation of the click interface.
     */
    public ClinicImageAdapter(List<ClinicImageModel> imageList, OnImageClickListener listener) {
        this.imageList = imageList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the individual item layout (XML) into a View object
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_clinic_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the specific data model for the current position in the list
        ClinicImageModel model = imageList.get(position);

        // Load image using Glide: Handles background threading, caching, and memory optimization
        Glide.with(holder.itemView.getContext())
                .load(model.getUrl())
                .placeholder(R.color.doc_surface_input) // Shown while the image is downloading
                .centerCrop() // Ensures the image fills the ImageView bounds nicely
                .into(holder.imgClinicItem);

        // Set listener for the Delete Button
        holder.btnDeleteImage.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(model);
            }
        });

        // Set listener for the entire Item View (for full-screen viewing)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageClick(model.getUrl());
            }
        });
    }

    @Override
    public int getItemCount() {
        // Returns the total number of images in the list; prevents null-pointer errors
        return imageList != null ? imageList.size() : 0;
    }

    /**
     * ViewHolder class that holds references to the views for each data item.
     * This improves performance by avoiding repeated findViewById() calls.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgClinicItem;
        ImageView btnDeleteImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Link the Java variables to the IDs defined in item_clinic_image.xml
            imgClinicItem = itemView.findViewById(R.id.imgClinicItem);
            btnDeleteImage = itemView.findViewById(R.id.btnDeleteImage);
        }
    }
}