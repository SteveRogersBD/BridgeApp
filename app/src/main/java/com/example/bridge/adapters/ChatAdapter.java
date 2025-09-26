package com.example.bridge.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.bridge.R;
import com.example.bridge.models.ChatModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder>{

    Context context;
    List<ChatModel>messages;

    public ChatAdapter(Context context, List<ChatModel> messages) {
        this.context = context;
        this.messages = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatModel message = messages.get(position);
        if(message.getSendBy()== ChatModel.SENT_BY_ME)
        {
            holder.leftL.setVisibility(View.GONE);
            holder.rightL.setVisibility(View.VISIBLE);
            holder.rightTV.setText(message.getMessage());
        }
        else{
            holder.rightL.setVisibility(View.GONE);
            holder.leftL.setVisibility(View.VISIBLE);
            holder.leftTV.setText(message.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView leftL,rightL;
        TextView leftTV, rightTV;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            leftL = itemView.findViewById(R.id.left_chat);
            rightL = itemView.findViewById(R.id.right_chat);
            leftTV = itemView.findViewById(R.id.left_chat_tv);
            rightTV = itemView.findViewById(R.id.right_chat_tv);
        }
    }
}
