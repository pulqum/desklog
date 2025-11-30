package com.example.photoviewer;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PostDetailActivity extends AppCompatActivity {

    private static final String SERVER_URL = "http://10.0.2.2:8000/api_root/Post/";
    private static final String TOKEN = "696e79eba229f1fab1b970f01b24f14c1903a28f";

    private TextView tvTitle, tvText, tvDate;
    private ImageView ivImage;
    private Button btnEdit, btnDelete;
    private ProgressBar progressBar;

    private int postId;
    private String postTitle, postText, postImage, postDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        // View 초기화
        tvTitle = findViewById(R.id.tvTitle);
        tvText = findViewById(R.id.tvText);
        tvDate = findViewById(R.id.tvDate);
        ivImage = findViewById(R.id.ivImage);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        progressBar = findViewById(R.id.progressBar);

        // Intent로 전달받은 데이터
        postId = getIntent().getIntExtra("post_id", -1);
        postTitle = getIntent().getStringExtra("post_title");
        postDate = getIntent().getStringExtra("post_date");

        if (postId != -1) {
            // ID가 있으면 ID로 로드
            loadPostDetail();
        } else if (postTitle != null && postDate != null) {
            // ID가 없으면 전체 목록에서 제목과 날짜로 찾기
            loadPostByTitleAndDate();
        } else {
            Toast.makeText(this, "게시물 정보가 없습니다", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 수정 버튼
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(PostDetailActivity.this, EditPostActivity.class);
            intent.putExtra("post_id", postId);
            intent.putExtra("post_title", postTitle);
            intent.putExtra("post_text", postText);
            intent.putExtra("post_image", postImage);
            startActivity(intent);
        });

        // 삭제 버튼
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 수정 후 돌아왔을 때 새로고침
        if (postId != -1) {
            loadPostDetail();
        }
    }

    private void loadPostDetail() {
        new LoadDetailTask().execute(postId);
    }

    private void loadPostByTitleAndDate() {
        new LoadListTask().execute();
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("게시물 삭제")
                .setMessage("정말로 이 게시물을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deletePost())
                .setNegativeButton("취소", null)
                .show();
    }

    private void deletePost() {
        new DeletePostTask().execute(postId);
    }

    // 상세 정보 로드 AsyncTask
    private class LoadDetailTask extends AsyncTask<Integer, Void, JSONObject> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            btnEdit.setEnabled(false);
            btnDelete.setEnabled(false);
        }

        @Override
        protected JSONObject doInBackground(Integer... params) {
            int id = params[0];
            try {
                URL url = new URL(SERVER_URL + id + "/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Token " + TOKEN);

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    return new JSONObject(response.toString());
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            displayPostData(result);
        }
    }

    // 이미지 로드 AsyncTask
    private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {
            String imageUrl = params[0];
            try {
                // 상대 경로를 절대 경로로 변환
                if (imageUrl.startsWith("/")) {
                    imageUrl = "http://10.0.2.2:8000" + imageUrl;
                }

                URL url = new URL(imageUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoInput(true);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream input = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    input.close();
                    conn.disconnect();
                    return bitmap;
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                ivImage.setVisibility(View.VISIBLE);
                ivImage.setImageBitmap(bitmap);
            } else {
                // Django처럼 이미지 로드 실패 시 숨김
                ivImage.setVisibility(View.GONE);
            }
        }
    }

    // 삭제 AsyncTask
    private class DeletePostTask extends AsyncTask<Integer, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            btnEdit.setEnabled(false);
            btnDelete.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            int id = params[0];
            try {
                URL url = new URL(SERVER_URL + id + "/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Authorization", "Token " + TOKEN);

                int responseCode = conn.getResponseCode();
                conn.disconnect();
                return responseCode == 204;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.GONE);

            if (success) {
                Toast.makeText(PostDetailActivity.this,
                        "게시물이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(PostDetailActivity.this,
                        "삭제 실패", Toast.LENGTH_SHORT).show();
                btnEdit.setEnabled(true);
                btnDelete.setEnabled(true);
            }
        }
    }

    // 제목과 날짜로 게시물 찾기 (ID 없을 때)
    private class LoadListTask extends AsyncTask<Void, Void, JSONObject> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            btnEdit.setEnabled(false);
            btnDelete.setEnabled(false);
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                URL url = new URL(SERVER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Token " + TOKEN);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONArray jsonArray = new JSONArray(response.toString());
                    // 제목과 날짜가 일치하는 게시물 찾기
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonPost = jsonArray.getJSONObject(i);
                        String title = jsonPost.optString("title", "");
                        String date = jsonPost.optString("published_date", "");

                        // 날짜 포맷 맞추기 (YYYY-MM-DD만 비교)
                        if (date.length() >= 10) {
                            date = date.substring(0, 10);
                        }
                        if (postDate != null && postDate.length() >= 10) {
                            String compareDate = postDate.substring(0, 10);
                            if (title.equals(postTitle) && date.equals(compareDate)) {
                                postId = jsonPost.optInt("id", -1);
                                return jsonPost;
                            }
                        }
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            displayPostData(result);
        }
    }

    // 게시물 데이터 표시 (공통 메서드)
    private void displayPostData(JSONObject result) {
        progressBar.setVisibility(View.GONE);
        btnEdit.setEnabled(true);
        btnDelete.setEnabled(true);

        if (result != null) {
            try {
                postTitle = result.getString("title");
                postText = result.getString("text");
                postImage = result.optString("image", "");
                String publishedDate = result.optString("published_date", "");

                tvTitle.setText(postTitle);
                tvText.setText(postText);

                if (publishedDate != null && publishedDate.length() >= 10) {
                    tvDate.setText(publishedDate.substring(0, 10));
                } else {
                    tvDate.setText("날짜 없음");
                }

                if (!postImage.isEmpty()
                        && !postImage.equals("null")
                        && !postImage.contains("default_error.png")) {
                    new LoadImageTask().execute(postImage);
                } else {
                    ivImage.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(PostDetailActivity.this,
                        "데이터 파싱 실패", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(PostDetailActivity.this,
                    "게시물을 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }
}
