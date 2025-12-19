package com.example.photoviewer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    private List<Post> posts;

    public PostAdapter(List<Post> posts) {
        this.posts = posts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.tvTime.setText(post.getFormattedTime());
        holder.tvTitle.setText(post.getTitle());
        holder.tvText.setText(post.getText());
        
        if (post.getImage() != null) {
            holder.ivImage.setImageBitmap(post.getImage());
            holder.ivImage.setVisibility(View.VISIBLE);
        } else {
            holder.ivImage.setVisibility(View.GONE);
        }

        // 카테고리별 배경색 설정
        int bgColor;
        if ("PHONE".equals(post.getCategory())) {
            bgColor = 0xFFFFEBEE; // 빨간색 계열
        } else if ("AWAY".equals(post.getCategory())) {
            bgColor = 0xFFFFF3E0; // 주황색 계열
        } else {
            bgColor = 0xFFE8F5E9; // 초록색 계열
        }
        holder.itemView.setBackgroundColor(bgColor);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvTitle, tvText;
        ImageView ivImage;

        ViewHolder(View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvText = itemView.findViewById(R.id.tvText);
            ivImage = itemView.findViewById(R.id.ivImage);
        }
    }
}

