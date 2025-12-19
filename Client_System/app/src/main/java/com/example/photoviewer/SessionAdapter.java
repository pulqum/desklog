package com.example.photoviewer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.ViewHolder> {
    private List<StudySession> sessions;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(StudySession session);
    }

    public SessionAdapter(List<StudySession> sessions, OnItemClickListener listener) {
        this.sessions = sessions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudySession session = sessions.get(position);
        holder.tvDate.setText(session.getFormattedDate());
        holder.tvTimeRange.setText(session.getFormattedTimeRange());
        holder.tvFocusScore.setText(String.format("%.1f점", session.getFocusScore()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(session);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTimeRange, tvFocusScore;

        ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTimeRange = itemView.findViewById(R.id.tvTimeRange);
            tvFocusScore = itemView.findViewById(R.id.tvFocusScore);
        }
    }
}

