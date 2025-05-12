package com.example.assitant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private List<MessageItem> messages = new ArrayList<>();
    
    public void addMessage(MessageItem message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }
    
    public void updateLastMessage(String newContent) {
        if (!messages.isEmpty()) {
            int lastIndex = messages.size() - 1;
            MessageItem lastMessage = messages.get(lastIndex);
            lastMessage.setContent(newContent);
            notifyItemChanged(lastIndex);
        }
    }
    
    public void clearMessages() {
        int size = messages.size();
        messages.clear();
        notifyItemRangeRemoved(0, size);
    }
    
    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        if (viewType == MessageItem.TYPE_USER) {
            View view = inflater.inflate(R.layout.item_message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_ai, parent, false);
            return new AIMessageViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageItem message = messages.get(position);
        
        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
        } else if (holder instanceof AIMessageViewHolder) {
            ((AIMessageViewHolder) holder).bind(message);
        }
    }
    
    public MessageItem getItem(int position) {
        return messages.get(position);
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        
        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
        }
        
        void bind(MessageItem message) {
            messageText.setText(message.getContent());
        }
    }
    
    static class AIMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        
        AIMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
        }
        
        void bind(MessageItem message) {
            messageText.setText(message.getContent());
        }
    }
} 