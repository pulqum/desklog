package com.example.photoviewer;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private List<Post> postList;
    private OnItemClickListener listener;

    public ImageAdapter(List<Post> postList, OnItemClickListener listener) {
        this.postList = postList;
        this.listener = listener;
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        Post post = postList.get(position);

        // 제목 설정
        holder.tvTitle.setText(post.getTitle());

        // 날짜 설정 (간단하게 포맷)
        String date = post.getPublishedDate();
        if (date != null && date.length() >= 10) {
            holder.tvDate.setText(date.substring(0, 10)); // YYYY-MM-DD만 표시
        } else {
            holder.tvDate.setText("날짜 없음");
        }

        // 내용 미리보기 설정
        holder.tvText.setText(post.getText());

        // 이미지 설정
        Bitmap bitmap = post.getImage();
        if (bitmap != null) {
            holder.imageView.setImageBitmap(bitmap);
        } else {
            // 기본 이미지 또는 플레이스홀더 설정
            holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // 클릭 리스너 설정
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvDate;
        TextView tvText;
        ImageView imageView;

        public ImageViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvPostTitle);
            tvDate = itemView.findViewById(R.id.tvPostDate);
            tvText = itemView.findViewById(R.id.tvPostText);
            imageView = itemView.findViewById(R.id.imageViewItem);
        }
    }
}