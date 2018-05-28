package com.example.szabi.fertestapp.view.home;

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
import com.example.szabi.fertestapp.model.messages.User;

import java.util.List;

public class UserListAdapter extends RecyclerView.Adapter {

    interface ItemClickListener {
        void onUserClicked(int position);
    }

    private List<User> userList;
    private ItemClickListener itemClickListener;

    UserListAdapter(List<User> userList, ItemClickListener itemClickListener) {
        this.userList = userList;
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_profile, parent, false);
        return new UserListAdapter.UserViewHolder(view, itemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((UserListAdapter.UserViewHolder) holder).bind(userList.get(position));
    }


    @Override
    public int getItemCount() {
        return userList.size();
    }

    private class UserViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView avatar;
        private View itemView;

        UserViewHolder(View itemView, ItemClickListener listener) {
            super(itemView);
            this.itemView = itemView;
            name = itemView.findViewById(R.id.user_name);
            avatar = itemView.findViewById(R.id.user_image);

            itemView.setOnClickListener(v -> listener.onUserClicked(getAdapterPosition()));
        }

        void bind(User item) {
            name.setText(item.getName());

            Glide.with(itemView.getContext())
                    .load(item.getPhotoUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .into(avatar);


        }
    }
}
