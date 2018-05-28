package com.example.szabi.fertestapp.view.chatroom;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.szabi.fertestapp.R;
import com.example.szabi.fertestapp.model.messages.Message;
import com.example.szabi.fertestapp.utils.DisplayUtils;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class MessageListAdapter extends RecyclerView.Adapter {

    private static final int SENT_MESSAGE = 1;
    private static final int RECEIVED_MESSAGE = 2;

    private List<Message> messageList;
    private FirebaseUser currentUser;

    MessageListAdapter(List<Message> messageList, FirebaseUser currentUser) {
        this.messageList = messageList;
        this.currentUser = currentUser;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if (viewType == SENT_MESSAGE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sent_message, parent, false);
            return new SentMessageViewHolder(view);
        } else if (viewType == RECEIVED_MESSAGE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_received_message, parent, false);
            return new ReceivedMessageViewHolder(view);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message messageItem = messageList.get(position);

        switch (holder.getItemViewType()) {
            case SENT_MESSAGE:
                ((SentMessageViewHolder) holder).bind(messageItem);
                break;
            case RECEIVED_MESSAGE:
                ((ReceivedMessageViewHolder) holder).bind(messageItem);
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);

        if (message.getName().equals(currentUser.getDisplayName())) {
            return SENT_MESSAGE;
        } else {
            return RECEIVED_MESSAGE;
        }

    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }


    private class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView message, time, name;
        ImageView avatar;

        private View itemView;

        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            message = itemView.findViewById(R.id.text_received_message);
            time = itemView.findViewById(R.id.text_received_time);
            name = itemView.findViewById(R.id.text_received_name);
            avatar = itemView.findViewById(R.id.image_received_profile);
        }

        void bind(Message messageItem) {
            message.setText(messageItem.getText());
            name.setText(messageItem.getName());
            time.setText(DisplayUtils.convertToTimeString(messageItem.getCreationTime()));

            Glide.with(itemView.getContext())
                    .load(messageItem.getPhotoUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .into(avatar);
        }
    }

    private class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView message, time;

        SentMessageViewHolder(View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.text_sent_message);
            time = itemView.findViewById(R.id.text_sent_time);
        }

        void bind(Message messageItem) {
            message.setText(messageItem.getText());
            time.setText(DisplayUtils.convertToTimeString(messageItem.getCreationTime()));
        }
    }

}
