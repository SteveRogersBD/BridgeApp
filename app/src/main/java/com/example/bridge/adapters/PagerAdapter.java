package com.example.bridge.adapters;

import static java.security.AccessController.getContext;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bridge.ChatActivity;
import com.example.bridge.R;
import com.example.bridge.databinding.ActivityMainBinding;
import com.example.bridge.models.PagerItem;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class PagerAdapter extends RecyclerView.Adapter<PagerAdapter.ViewHolder> {

    Context context;
    List<PagerItem> list;
    public PagerAdapter(Context context, List<PagerItem> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_layout, parent, false);
        return new ViewHolder(view);
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder,
                                 @SuppressLint("RecyclerView") int position)
    {
        PagerItem item = list.get(position);
        holder.imageView.setImageResource(item.getImage());
        holder.textView.setText(item.getTitle());

        // Resolve color resources to actual color values
        @ColorInt int iconTint = ContextCompat.getColor(context, item.getIconTint());
        @ColorInt int stroke = ContextCompat.getColor(context, item.getStroke());

        // Apply the colors
        ImageViewCompat.setImageTintList(holder.imageView, ColorStateList.valueOf(iconTint));
        holder.textView.setTextColor(iconTint);
        holder.cardView.setStrokeColor(stroke);
        if(position == 0) {
            holder.view.setBackground(context.getDrawable(R.drawable.circle_glow_bg));
        } else if (position==1) {
            holder.view.setBackground(context.getDrawable(R.drawable.circle_green_glow));
        }else if (position==2) {
            holder.view.setBackground(context.getDrawable(R.drawable.circle_red_glow));
        }else if (position==3) {
            holder.view.setBackground(context.getDrawable(R.drawable.circle_orange_glow));
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (position == 0) {
                    context.startActivity(new Intent(context, ChatActivity.class));
                }
                else if (position == 1) {
                    context.startActivity(new Intent(context, ChatActivity.class));
                }
                else if (position == 2) {
                    context.startActivity(new Intent(context, ChatActivity.class));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        TextView textView;
        MaterialCardView cardView;
        LinearLayout linearLayout;
        View view;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image);
            textView = itemView.findViewById(R.id.title);
            cardView = itemView.findViewById(R.id.card);
            linearLayout = itemView.findViewById(R.id.container);
            view = itemView.findViewById(R.id.view);
        }
    }

    private int dp(View v, int value) {
        float d = v.getResources().getDisplayMetrics().density;
        return Math.round(value * d);
    }
}
