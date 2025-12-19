package com.example.photoviewer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class SessionListActivity extends AppCompatActivity {
    private ApiClient apiClient;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private List<StudySession> sessionList = new ArrayList<>();
    private SessionAdapter adapter;
    private Handler handler = new Handler();
    private Runnable checkActiveSessionRunnable;
    private boolean isCheckingActiveSession = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_list);

        if (!ApiClient.isLoggedIn(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        apiClient = new ApiClient(this);
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        adapter = new SessionAdapter(sessionList, session -> {
            Intent intent = new Intent(SessionListActivity.this, SessionDetailActivity.class);
            intent.putExtra("session_id", session.getId());
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(() -> loadSessions());

        loadSessions();

        // 실시간 알림 체크 (30초마다)
        checkActiveSessionRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isCheckingActiveSession) {
                    checkActiveSession();
                }
                handler.postDelayed(this, 30000); // 30초마다 체크
            }
        };
        handler.postDelayed(checkActiveSessionRunnable, 30000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (checkActiveSessionRunnable != null) {
            handler.removeCallbacks(checkActiveSessionRunnable);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.session_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_logout) {
            ApiClient.clearToken(this);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        } else if (item.getItemId() == R.id.menu_start_session) {
            startSession();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadSessions() {
        new LoadSessionsTask().execute();
    }

    private void startSession() {
        new StartSessionTask().execute();
    }

    private void checkActiveSession() {
        isCheckingActiveSession = true;
        new CheckActiveSessionTask().execute();
    }

    private class LoadSessionsTask extends AsyncTask<Void, Void, List<StudySession>> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<StudySession> doInBackground(Void... voids) {
            try {
                JSONArray sessionsJson = apiClient.getSessionsList();
                List<StudySession> sessions = new ArrayList<>();
                
                for (int i = 0; i < sessionsJson.length(); i++) {
                    JSONObject sessionJson = sessionsJson.getJSONObject(i);
                    int id = sessionJson.getInt("id");
                    String startTime = sessionJson.optString("start_time", "");
                    String endTime = sessionJson.optString("end_time", "");
                    boolean isActive = sessionJson.optBoolean("is_active", false);
                    int totalStudy = sessionJson.optInt("total_study", 0);
                    int totalPhone = sessionJson.optInt("total_phone", 0);
                    int totalAway = sessionJson.optInt("total_away", 0);
                    double focusScore = sessionJson.optDouble("focus_score", 0.0);
                    
                    StudySession session = new StudySession(id, startTime, endTime, isActive,
                            totalStudy, totalPhone, totalAway, focusScore);
                    sessions.add(session);
                }
                
                return sessions;
            } catch (Exception e) {
                e.printStackTrace();
                android.util.Log.e("SessionListActivity", "Error loading sessions: " + e.getMessage());
                // 401 Unauthorized 또는 403 Forbidden인 경우 토큰 만료로 간주
                if (e.getMessage() != null && (e.getMessage().contains("401") || e.getMessage().contains("403") || e.getMessage().contains("Token expired"))) {
                    // 토큰 만료 - UI 스레드에서 로그인 화면으로 이동
                    runOnUiThread(() -> {
                        ApiClient.clearToken(SessionListActivity.this);
                        Toast.makeText(SessionListActivity.this, "로그인이 만료되었습니다. 다시 로그인해주세요.", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(SessionListActivity.this, LoginActivity.class));
                        finish();
                    });
                    return new ArrayList<>();
                }
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<StudySession> sessions) {
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);

            if (sessions == null) {
                Toast.makeText(SessionListActivity.this, "세션을 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            // 토큰 만료 체크 (빈 리스트가 반환되고 에러 메시지에 401/403이 포함된 경우)
            // 실제로는 API 호출 시 에러가 발생하면 null을 반환하므로 여기서는 일반 처리
            
            sessionList.clear();
            sessionList.addAll(sessions);
            adapter.notifyDataSetChanged();
            
            if (sessionList.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("아직 기록된 공부 세션이 없습니다.");
            } else {
                tvEmpty.setVisibility(View.GONE);
            }
        }
    }

    private class StartSessionTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                return apiClient.startSession();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(SessionListActivity.this, "공부 시작!", Toast.LENGTH_SHORT).show();
                loadSessions();
            } else {
                Toast.makeText(SessionListActivity.this, "공부 시작 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class CheckActiveSessionTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                return apiClient.checkActiveSession();
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean hasActiveSession) {
            isCheckingActiveSession = false;
            if (hasActiveSession) {
                // 알림 표시
                showNotification("공부 시작 알림", "공부를 시작하셨습니다! 집중하세요! 💪");
            }
        }
    }

    private void showNotification(String title, String message) {
        android.app.NotificationManager notificationManager = 
            (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                "desklog_channel", "DeskLog 알림", android.app.NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("공부 시작 알림");
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        android.app.Notification.Builder builder = new android.app.Notification.Builder(this, "desklog_channel")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setPriority(android.app.Notification.PRIORITY_HIGH);

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            builder.setPriority(android.app.Notification.PRIORITY_HIGH);
        }

        notificationManager.notify(1, builder.build());
    }
}

