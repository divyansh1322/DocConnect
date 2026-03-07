package com.example.docconnect;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;

/**
 * A BottomSheet that generates a shareable "Doctor Card" image.
 * It captures a specific part of the UI as a Bitmap and shares it via system intents.
 */
public class DoctorShareBottomSheet extends BottomSheetDialogFragment {

    // UI components used for the shareable card and controls
    private CoordinatorLayout shareRoot; // The specific container we will turn into an image
    private ImageView doctorImage;
    private TextView tvName, tvSpecialty, tvRating, tvReviews;
    private MaterialButton btnConfirmShare;
    private View dragHandle;

    /**
     * Static factory method to create the fragment with required doctor data.
     */
    public static DoctorShareBottomSheet newInstance(String name, String specialty, String imageUrl, String rating, String reviews) {
        DoctorShareBottomSheet fragment = new DoctorShareBottomSheet();
        Bundle args = new Bundle();
        args.putString("name", name);
        args.putString("specialty", specialty);
        args.putString("image", imageUrl);
        args.putString("rating", rating);
        args.putString("reviews", reviews);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Apply rounded corner style defined in themes/styles
        setStyle(STYLE_NORMAL, R.style.RoundedBottomSheetDialog);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        // Force the BottomSheet to open fully expanded and handle transparent backgrounds
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) d;
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_doctor_share, container, false);

        // 1. Initialize Views
        shareRoot = view.findViewById(R.id.share_root);
        doctorImage = view.findViewById(R.id.doctor_image);
        tvName = view.findViewById(R.id.tv_name);
        tvSpecialty = view.findViewById(R.id.tv_specialty);
        btnConfirmShare = view.findViewById(R.id.btn_confirm_share);
        dragHandle = view.findViewById(R.id.drag_handle);

        // 2. Set Data from Arguments (The data passed from DoctorProfileActivity)
        if (getArguments() != null) {
            tvName.setText(getArguments().getString("name"));
            tvSpecialty.setText(getArguments().getString("specialty"));
            // Note: Ensure rating/reviews textviews are added if you want to display them

            Glide.with(this)
                    .load(getArguments().getString("image"))
                    .placeholder(R.drawable.ic_person)
                    .into(doctorImage);
        }

        // 3. Action: Handle the Share button click
        btnConfirmShare.setOnClickListener(v -> {
            // STEP A: Hide elements that shouldn't appear in the captured image
            btnConfirmShare.setVisibility(View.GONE);
            dragHandle.setVisibility(View.GONE);

            // STEP B: Give the UI a tiny moment to hide views before capturing the Bitmap
            view.postDelayed(this::captureAndShare, 100);
        });

        return view;
    }

    /**
     * THE MAGIC: Converts the XML layout into a PNG image and triggers the Share Intent.
     */
    private void captureAndShare() {
        try {
            // Safety check for layout dimensions
            if (shareRoot.getWidth() == 0 || shareRoot.getHeight() == 0) {
                resetUI();
                return;
            }

            // 1. Create a Bitmap with the same dimensions as our CoordinatorLayout
            Bitmap bitmap = Bitmap.createBitmap(shareRoot.getWidth(), shareRoot.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // 2. "Draw" the view onto the canvas/bitmap
            shareRoot.draw(canvas);

            // 3. Save the bitmap to the app's internal cache directory
            File cachePath = new File(requireContext().getCacheDir(), "images");
            cachePath.mkdirs(); // Ensure directory exists
            File file = new File(cachePath, "doctor_card.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            // 4. Generate a secure content URI using FileProvider
            // IMPORTANT: Requires a <provider> entry in AndroidManifest.xml
            Uri contentUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".provider", file);

            // 5. Create and launch the Send Intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Grant temporary access to the image

            startActivity(Intent.createChooser(shareIntent, "Share Doctor Profile"));

            // Close the BottomSheet after sharing is initiated
            dismiss();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Capture failed", Toast.LENGTH_SHORT).show();
            resetUI();
        }
    }

    /**
     * Restores visibility to hidden buttons if something goes wrong during capture.
     */
    private void resetUI() {
        if (btnConfirmShare != null) btnConfirmShare.setVisibility(View.VISIBLE);
        if (dragHandle != null) dragHandle.setVisibility(View.VISIBLE);
    }
}