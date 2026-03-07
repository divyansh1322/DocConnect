package com.example.docconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * UDChatAdapter: The definitive adapter for the DocConnect Unified Chat System.
 * * HANDLES: Sent/Received Text & Sent/Received Images (Cloudinary & Firebase).
 * * PERFORMANCE: Optimized with Glide for memory efficiency and smooth scrolling.
 * * LOGIC: Includes local-time fallback for unsynced Firebase Server Timestamps.
 */
public class UDChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // --- VIEW TYPE CONSTANTS ---
    // These integers define which layout to inflate for each list item.
    private static final int TYPE_TEXT_SENT = 1;
    private static final int TYPE_TEXT_RECEIVED = 2;
    private static final int TYPE_IMAGE_SENT = 3;
    private static final int TYPE_IMAGE_RECEIVED = 4;

    private final List<ChatModel.Message> messageList;
    private final String loggedInUserId;

    /**
     * Constructor
     * @param list The list of messages pulled from Firebase.
     * @param userId The current user's UID to distinguish "my" messages from the "doctor's".
     */
    public UDChatAdapter(List<ChatModel.Message> list, String userId) {
        this.messageList = list;
        this.loggedInUserId = userId;
    }

    /**
     * getItemViewType: Determines the UI structure for each item in the list.
     * It checks: 1. Who sent it? 2. Is it text or an image?
     */
    @Override
    public int getItemViewType(int position) {
        ChatModel.Message m = messageList.get(position);

        // NULL SAFETY: Ensure senderId exists
        boolean isMe = m.getSenderId() != null && m.getSenderId().equals(loggedInUserId);

        // IMAGE DETECTION: Checks for 'type' field or specific URL patterns (Firebase or Cloudinary)
        String content = (m.getMessage() != null) ? m.getMessage() : "";
        boolean isImg = "image".equalsIgnoreCase(m.getType()) ||
                content.startsWith("https://firebasestorage") ||
                content.contains("cloudinary.com");

        if (isMe) {
            return isImg ? TYPE_IMAGE_SENT : TYPE_TEXT_SENT;
        } else {
            return isImg ? TYPE_IMAGE_RECEIVED : TYPE_TEXT_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // SELECT LAYOUT: Match the layout file to the calculated viewType
        switch (viewType) {
            case TYPE_TEXT_SENT:
                return new TextVH(inflater.inflate(R.layout.item_chat_sent, parent, false), true);
            case TYPE_TEXT_RECEIVED:
                return new TextVH(inflater.inflate(R.layout.item_chat_received, parent, false), false);
            case TYPE_IMAGE_SENT:
                return new ImageVH(inflater.inflate(R.layout.item_chat_sent_image, parent, false), true);
            case TYPE_IMAGE_RECEIVED:
                return new ImageVH(inflater.inflate(R.layout.item_chat_recieved_image, parent, false), false);
            default:
                // Safety fallback to prevent crashes on unknown data types
                return new TextVH(inflater.inflate(R.layout.item_chat_received, parent, false), false);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatModel.Message m = messageList.get(position);

        // TIMESTAMP LOGIC: Convert long to readable time string
        String timeStr = formatTime(m.getTimestamp());

        if (holder instanceof TextVH) {
            // BIND TEXT: Set message content and time
            TextVH textHolder = (TextVH) holder;
            textHolder.msg.setText(m.getMessage());
            textHolder.time.setText(timeStr);

        } else if (holder instanceof ImageVH) {
            // BIND IMAGE: Use Glide to load URLs asynchronously
            ImageVH imgHolder = (ImageVH) holder;
            imgHolder.time.setText(timeStr);

            Glide.with(imgHolder.itemView.getContext())
                    .load(m.getMessage())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_person_placeholder) // While loading
                            .error(R.drawable.ic_person)            // If URL fails
                            .transform(new RoundedCorners(32)))          // Modern rounded look
                    .into(imgHolder.img);
        }
    }

    @Override
    public int getItemCount() {
        return (messageList != null) ? messageList.size() : 0;
    }

    /**
     * formatTime: Professional Time Formatter.
     * Firebase's ServerValue.TIMESTAMP is 0 until it hits the server.
     * We use System time as a fallback to avoid showing 12:00 AM for new messages.
     */
    private String formatTime(long timestamp) {
        long finalTime = (timestamp == 0) ? System.currentTimeMillis() : timestamp;
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(finalTime));
    }

    // --- VIEW HOLDERS ---

    /**
     * TextVH: Holds references for text-based chat bubbles.
     */
    static class TextVH extends RecyclerView.ViewHolder {
        TextView msg, time;
        TextVH(View v, boolean isSent) {
            super(v);
            // Matches your XML layout IDs for sent/received text
            msg = v.findViewById(isSent ? R.id.tvMessageSent : R.id.tvMessageRecieved);
            time = v.findViewById(isSent ? R.id.tvTimeSent : R.id.tvTimeRecieved);
        }
    }

    /**
     * ImageVH: Holds references for image-based chat bubbles.
     */
    static class ImageVH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView time;
        ImageVH(View v, boolean isSent) {
            super(v);
            // Matches your XML layout IDs for sent/received images
            img = v.findViewById(isSent ? R.id.ivMessageSent : R.id.ivMessageReceived);
            time = v.findViewById(isSent ? R.id.tvTimeSentImage : R.id.tvTimeReceivedImage);
        }
    }
}