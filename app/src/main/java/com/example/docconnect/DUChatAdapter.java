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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Custom Adapter for the Chat System (Doctor-User).
 * Handles four different view types to differentiate between Sent/Received and Text/Image messages.
 */
public class DUChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Unique constants for each message layout type
    private static final int TYPE_TEXT_SENT = 1;
    private static final int TYPE_TEXT_RECEIVED = 2;
    private static final int TYPE_IMAGE_SENT = 3;
    private static final int TYPE_IMAGE_RECEIVED = 4;

    private final List<DoctorChatModel.Message> messageList;
    private final String currentUserId;

    public DUChatAdapter(List<DoctorChatModel.Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    /**
     * Critical Logic: Determines which layout to use for each message.
     * Checks if the sender is the current user and if the content is a URL (image) or plain text.
     */
    @Override
    public int getItemViewType(int position) {
        DoctorChatModel.Message message = messageList.get(position);

        boolean isMe = message.getSenderId().equals(currentUserId);

        // Identifies if content is an image based on explicit type or URL prefix
        boolean isImage = "image".equalsIgnoreCase(message.getType()) ||
                (message.getMessage() != null && message.getMessage().startsWith("https://firebasestorage"));

        if (isMe) {
            return isImage ? TYPE_IMAGE_SENT : TYPE_TEXT_SENT;
        } else {
            return isImage ? TYPE_IMAGE_RECEIVED : TYPE_TEXT_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        // Returns the specific ViewHolder class and XML layout based on the ViewType determined above
        switch (viewType) {
            case TYPE_IMAGE_SENT:
                return new ImageSentViewHolder(inflater.inflate(R.layout.item_chat_sent_image, parent, false));
            case TYPE_IMAGE_RECEIVED:
                return new ImageReceivedViewHolder(inflater.inflate(R.layout.item_chat_recieved_image, parent, false));
            case TYPE_TEXT_SENT:
                return new TextSentViewHolder(inflater.inflate(R.layout.item_chat_sent, parent, false));
            default:
                return new TextReceivedViewHolder(inflater.inflate(R.layout.item_chat_received, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DoctorChatModel.Message message = messageList.get(position);

        // Type-casting the generic holder to specific view holders to call their respective bind methods
        if (holder instanceof TextSentViewHolder) ((TextSentViewHolder) holder).bind(message);
        else if (holder instanceof TextReceivedViewHolder) ((TextReceivedViewHolder) holder).bind(message);
        else if (holder instanceof ImageSentViewHolder) ((ImageSentViewHolder) holder).bind(message);
        else if (holder instanceof ImageReceivedViewHolder) ((ImageReceivedViewHolder) holder).bind(message);
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    /**
     * Formats Unix timestamp (long) into a user-friendly string (e.g., 09:45 PM).
     */
    private static String formatTime(long timestamp) {
        if (timestamp == 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // --- VIEW HOLDERS ---
    // These static classes hold references to the UI elements within each specific chat bubble layout.

    static class TextSentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        TextSentViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessageSent);
            tvTime = v.findViewById(R.id.tvTimeSent);
        }
        void bind(DoctorChatModel.Message m) {
            tvMessage.setText(m.getMessage());
            tvTime.setText(formatTime(m.getTimestamp()));
        }
    }

    static class TextReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        TextReceivedViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessageRecieved);
            tvTime = v.findViewById(R.id.tvTimeRecieved);
        }
        void bind(DoctorChatModel.Message m) {
            tvMessage.setText(m.getMessage());
            tvTime.setText(formatTime(m.getTimestamp()));
        }
    }

    static class ImageSentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMessage;
        TextView tvTime;
        ImageSentViewHolder(View v) {
            super(v);
            ivMessage = v.findViewById(R.id.ivMessageSent);
            tvTime = v.findViewById(R.id.tvTimeSentImage);
        }
        void bind(DoctorChatModel.Message m) {
            tvTime.setText(formatTime(m.getTimestamp()));
            // Uses Glide with RoundedCorners transformation for a modern look
            Glide.with(itemView.getContext())
                    .load(m.getMessage())
                    .transform(new RoundedCorners(20))
                    .into(ivMessage);
        }
    }

    static class ImageReceivedViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMessage;
        TextView tvTime;
        ImageReceivedViewHolder(View v) {
            super(v);
            ivMessage = v.findViewById(R.id.ivMessageReceived);
            tvTime = v.findViewById(R.id.tvTimeReceivedImage);
        }
        void bind(DoctorChatModel.Message m) {
            tvTime.setText(formatTime(m.getTimestamp()));
            Glide.with(itemView.getContext())
                    .load(m.getMessage())
                    .transform(new RoundedCorners(20))
                    .into(ivMessage);
        }
    }
}