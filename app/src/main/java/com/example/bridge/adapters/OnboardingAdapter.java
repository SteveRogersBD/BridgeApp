package com.example.bridge.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bridge.R;
import com.example.bridge.models.PagerItem;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private Context context;
    private List<PagerItem> items;

    public OnboardingAdapter(Context context, List<PagerItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_onboarding, parent, false);
        return new OnboardingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        PagerItem item = items.get(position);
        
        holder.imageView.setImageResource(item.getImage());
        holder.titleText.setText(item.getTitle());
        holder.subtitleText.setText(item.getSubTitle());

        // Resolve color resources to actual color values
        @ColorInt int iconTint = ContextCompat.getColor(context, item.getIconTint());
        @ColorInt int stroke = ContextCompat.getColor(context, item.getStroke());

        // Apply the colors
        ImageViewCompat.setImageTintList(holder.imageView, ColorStateList.valueOf(iconTint));
        holder.titleText.setTextColor(iconTint);
        holder.cardView.setStrokeColor(stroke);

        // Apply glow background based on position
        if (position == 0) {
            holder.glowView.setBackground(context.getDrawable(R.drawable.circle_glow_bg));
        } else if (position == 1) {
            holder.glowView.setBackground(context.getDrawable(R.drawable.circle_green_glow));
        } else if (position == 2) {
            holder.glowView.setBackground(context.getDrawable(R.drawable.circle_red_glow));
        } else if (position == 3) {
            holder.glowView.setBackground(context.getDrawable(R.drawable.circle_orange_glow));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class OnboardingViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleText;
        TextView subtitleText;
        MaterialCardView cardView;
        View glowView;

        public OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image);
            titleText = itemView.findViewById(R.id.title);
            subtitleText = itemView.findViewById(R.id.subtitle);
            cardView = itemView.findViewById(R.id.card);
            glowView = itemView.findViewById(R.id.view);
        }
    }
}