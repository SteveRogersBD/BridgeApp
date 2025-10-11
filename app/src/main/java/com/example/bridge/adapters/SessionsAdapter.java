package com.example.bridge.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bridge.R;
import com.example.bridge.models.SessionItem;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class SessionsAdapter extends RecyclerView.Adapter<SessionsAdapter.SessionViewHolder> {

    private Context context;
    private List<SessionItem> sessions;

    public SessionsAdapter(Context context, List<SessionItem> sessions) {
        this.context = context;
        this.sessions = sessions;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        SessionItem session = sessions.get(position);
        
        holder.titleText.setText(session.getTitle());
        holder.descriptionText.setText(session.getDescription());
        holder.typeText.setText(session.getType());
        holder.timestampText.setText(getTimeAgo(session.getTimestamp()));
        
        // Set icon based on session type
        int iconRes = getIconForType(session.getType());
        holder.typeIcon.setImageResource(iconRes);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    private int getIconForType(String type) {
        switch (type) {
            case "Voice Note":
                return R.drawable.mic;
            case "Chat":
                return R.drawable.chat;
            case "Meeting":
                return R.drawable.trans;
            default:
                return R.drawable.mic;
        }
    }

    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " min ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (days < 7) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else {
            return "1 week ago";
        }
    }

    public static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView descriptionText;
        TextView typeText;
        TextView timestampText;
        ImageView typeIcon;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.title_text);
            descriptionText = itemView.findViewById(R.id.description_text);
            typeText = itemView.findViewById(R.id.type_text);
            timestampText = itemView.findViewById(R.id.timestamp_text);
            typeIcon = itemView.findViewById(R.id.type_icon);
        }
    }
}