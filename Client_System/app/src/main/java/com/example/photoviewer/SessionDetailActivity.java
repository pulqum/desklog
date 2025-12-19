package com.example.photoviewer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SessionDetailActivity extends AppCompatActivity {
    private ApiClient apiClient;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvFocusScore, tvStats;
    private List<Post> postList = new ArrayList<>();
    private PostAdapter adapter;
    private int sessionId;
    private SessionStatistics stats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_detail);

        sessionId = getIntent().getIntExtra("session_id", -1);
        if (sessionId == -1) {
            Toast.makeText(this, "세션 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiClient = new ApiClient(this);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvFocusScore = findViewById(R.id.tvFocusScore);
        tvStats = findViewById(R.id.tvStats);

        adapter = new PostAdapter(postList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadSessionDetail();
    }

    private void loadSessionDetail() {
        new LoadSessionDetailTask().execute();
    }

    private class LoadSessionDetailTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                JSONObject sessionJson = apiClient.getSessionDetail(sessionId);
                
                // 통계 정보 추출
                int totalStudy = sessionJson.optInt("total_study", 0);
                int totalPhone = sessionJson.optInt("total_phone", 0);
                int totalAway = sessionJson.optInt("total_away", 0);
                double focusScore = sessionJson.optDouble("focus_score", 0.0);
                stats = new SessionStatistics(totalStudy, totalPhone, totalAway, focusScore);

                // Post 리스트 추출
                JSONArray postsJson = sessionJson.optJSONArray("posts");
                if (postsJson != null) {
                    postList.clear();
                    for (int i = 0; i < postsJson.length(); i++) {
                        JSONObject postJson = postsJson.getJSONObject(i);
                        int postId = postJson.getInt("id");
                        String title = postJson.optString("title", "");
                        String text = postJson.optString("text", "");
                        String publishedDate = postJson.optString("published_date", "");
                        String category = postJson.optString("category", "STUDY");
                        String imageUrl = postJson.optString("image", "");

                        // 이미지 다운로드
                        Bitmap imageBitmap = null;
                        if (!imageUrl.isEmpty() && !imageUrl.equals("null") && !imageUrl.contains("default_error.png")) {
                            try {
                                URL url = new URL(imageUrl);
                                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                conn.setConnectTimeout(5000);
                                conn.setReadTimeout(5000);
                                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                    InputStream is = conn.getInputStream();
                                    imageBitmap = BitmapFactory.decodeStream(is);
                                    is.close();
                                }
                                conn.disconnect();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        Post post = new Post(postId, title, text, publishedDate, category, imageBitmap, imageUrl);
                        postList.add(post);
                    }
                    // 시간순으로 정렬 (오래된 것부터)
                    java.util.Collections.sort(postList, (p1, p2) -> {
                        try {
                            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                            java.util.Date date1 = format.parse(p1.getPublishedDate());
                            java.util.Date date2 = format.parse(p2.getPublishedDate());
                            return date1.compareTo(date2);
                        } catch (Exception e) {
                            return 0;
                        }
                    });
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                android.util.Log.e("SessionDetailActivity", "Error loading session detail: " + e.getMessage());
                // 401 Unauthorized 또는 403 Forbidden인 경우 토큰 만료로 간주
                if (e.getMessage() != null && (e.getMessage().contains("401") || e.getMessage().contains("403") || e.getMessage().contains("Token expired"))) {
                    // 토큰 만료 - UI 스레드에서 로그인 화면으로 이동
                    runOnUiThread(() -> {
                        ApiClient.clearToken(SessionDetailActivity.this);
                        Toast.makeText(SessionDetailActivity.this, "로그인이 만료되었습니다. 다시 로그인해주세요.", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(SessionDetailActivity.this, LoginActivity.class));
                        finish();
                    });
                }
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.GONE);

            if (success) {
                // 통계 표시
                if (stats != null) {
                    tvFocusScore.setText(String.format("%.1f점", stats.getFocusScore()));
                    tvStats.setText(String.format("집중 %d회 | 딴짓 %d회 | 자리비움 %d회",
                            stats.getTotalStudy(), stats.getTotalPhone(), stats.getTotalAway()));
                } else {
                    // 통계가 없으면 기본값 표시
                    tvFocusScore.setText("0.0점");
                    tvStats.setText("집중 0회 | 딴짓 0회 | 자리비움 0회");
                }
                
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(SessionDetailActivity.this, "세션 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

