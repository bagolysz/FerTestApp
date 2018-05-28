package com.example.szabi.fertestapp.view.home;

import android.media.Image;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.szabi.fertestapp.R;
import com.example.szabi.fertestapp.model.messages.Conversation;

import java.util.List;

public class ConversationListAdapter extends RecyclerView.Adapter {
    interface ItemClickListener {
        void onConversationClicked(int position);
    }

    private List<Conversation> conversationList;
    private ItemClickListener itemClickListener;

    ConversationListAdapter(List<Conversation> conversationList, ItemClickListener itemClickListener) {
        this.conversationList = conversationList;
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation_profile, parent, false);
        return new ConversationListAdapter.ConversationViewHolder(view, itemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((ConversationListAdapter.ConversationViewHolder) holder).bind(conversationList.get(position));
    }


    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    private class ConversationViewHolder extends RecyclerView.ViewHolder {
        TextView names;
        ImageView avatar;

        ConversationViewHolder(View itemView, ItemClickListener listener) {
            super(itemView);
            names = itemView.findViewById(R.id.conversation_participants);
            avatar = itemView.findViewById(R.id.conversation_avatar);

            itemView.setOnClickListener(v -> listener.onConversationClicked(getAdapterPosition()));
        }

        void bind(Conversation item) {
            StringBuilder builder = new StringBuilder("");
            int size = item.users.size();
            for (int i = 0; i < size - 1; i++) {
                builder.append(item.users.get(i).getName()).append(", ");
            }
            builder.append(item.users.get(size - 1).getName());
            names.setText(builder.toString());
        }
    }
}
