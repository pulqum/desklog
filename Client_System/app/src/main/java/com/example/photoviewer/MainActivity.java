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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_POST_DETAIL = 100;

    TextView textView;
    String site_url = "http://10.0.2.2:8000";
    JSONObject post_json;
    String imageUrl = null;
    Bitmap bmImg = null;
    CloadImage taskDownload;

    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private List<Post> postList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);

        // Pull-to-Refresh 설정
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadPosts();
            }
        });

        // 초기 로드
        loadPosts();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_POST_DETAIL && resultCode == RESULT_OK) {
            // 상세보기에서 삭제 등의 작업 후 목록 새로고침
            loadPosts();
        }
    }

    private void loadPosts() {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post/");
    }

    public void onClickDownload(View v) {
        loadPosts();
        Toast.makeText(getApplicationContext(), "새로고침", Toast.LENGTH_SHORT).show();
    }

    public void onClickUpload(View v) {
        // UploadActivity 실행
        Intent intent = new Intent(MainActivity.this, UploadActivity.class);
        startActivity(intent);
    }

    private class CloadImage extends AsyncTask<String, Integer, JSONArray> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected JSONArray doInBackground(String... urls) {
            try {
                String apiUrl = urls[0];
                String token = "696e79eba229f1fab1b970f01b24f14c1903a28f";
                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();

                    String strJson = result.toString();
                    JSONArray aryJson = new JSONArray(strJson);

                    // Post 리스트 초기화
                    postList.clear();

                    // 배열 내 모든 게시물 처리
                    for (int i = 0; i < aryJson.length(); i++) {
                        post_json = (JSONObject) aryJson.get(i);

                        // Post 데이터 추출
                        int postId = post_json.has("id") ? post_json.getInt("id") : -1;
                        String title = post_json.optString("title", "제목 없음");
                        String text = post_json.optString("text", "");
                        String publishedDate = post_json.optString("published_date", "");
                        String category = post_json.optString("category", "STUDY");

                        // 이미지 다운로드
                        Bitmap imageBitmap = null;
                        imageUrl = post_json.optString("image", "");
                        // default_error.png는 무시
                        if (!imageUrl.equals("")
                                && !imageUrl.equals("null")
                                && !imageUrl.contains("default_error.png")) {
                            try {
                                URL myImageUrl = new URL(imageUrl);
                                HttpURLConnection imgConn = (HttpURLConnection) myImageUrl.openConnection();
                                imgConn.setConnectTimeout(5000);
                                imgConn.setReadTimeout(5000);

                                int imgResponseCode = imgConn.getResponseCode();
                                if (imgResponseCode == HttpURLConnection.HTTP_OK) {
                                    InputStream imgStream = imgConn.getInputStream();
                                    imageBitmap = BitmapFactory.decodeStream(imgStream);
                                    imgStream.close();
                                }
                                imgConn.disconnect();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        // Post 객체 생성 및 리스트에 추가
                        Post post = new Post(postId, title, text, publishedDate, category, imageBitmap, imageUrl);
                        postList.add(post);
                    }
                    return aryJson;
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONArray result) {
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);

            if (result == null || postList.isEmpty()) {
                textView.setText("불러올 게시물이 없습니다.");
            } else {
                textView.setText("게시물 " + postList.size() + "개 로드 완료!");
                ImageAdapter adapter = new ImageAdapter(postList, new ImageAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(int position) {
                        Post post = postList.get(position);
                        Intent intent = new Intent(MainActivity.this, PostDetailActivity.class);
                        intent.putExtra("post_id", post.getId());
                        intent.putExtra("post_title", post.getTitle());
                        intent.putExtra("post_date", post.getPublishedDate());
                        startActivityForResult(intent, REQUEST_POST_DETAIL);
                    }
                });
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                recyclerView.setAdapter(adapter);
            }
        }
    }

    private class PutPost extends AsyncTask<String, Void, Boolean> {
        private Bitmap bitmap;

        public PutPost(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            // null 체크 강화
            if (bitmap == null || bitmap.isRecycled()) {
                android.util.Log.e("MainActivity", "Bitmap is null or recycled");
                return false;
            }
            try {
                String boundary = "----FormBoundary" + System.currentTimeMillis();
                URL url = new URL(site_url + "/api_root/Post/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Token 696e79eba229f1fab1b970f01b24f14c1903a28f");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setDoOutput(true);

                // 하드코딩된 데이터
                String title = "테스트 제목";
                String text = "테스트 내용";
                // 현재 시간을 published_date로 설정 (ISO 8601 형식)
                String publishedDate = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).format(new java.util.Date());

                // 이미지 데이터 준비 (생성자로 받은 bitmap 사용)
                // 추가 null 체크
                if (bitmap == null || bitmap.isRecycled()) {
                    android.util.Log.e("MainActivity", "Bitmap became null or recycled during processing");
                    return false;
                }
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                boolean compressResult = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                if (!compressResult) {
                    android.util.Log.e("MainActivity", "Failed to compress bitmap");
                    return false;
                }
                byte[] imageBytes = baos.toByteArray();

                // multipart 데이터 작성
                StringBuilder postData = new StringBuilder();
                postData.append("--").append(boundary).append("\r\n");
                postData.append("Content-Disposition: form-data; name=\"title\"\r\n\r\n");
                postData.append(title).append("\r\n");

                postData.append("--").append(boundary).append("\r\n");
                postData.append("Content-Disposition: form-data; name=\"text\"\r\n\r\n");
                postData.append(text).append("\r\n");

                postData.append("--").append(boundary).append("\r\n");
                postData.append("Content-Disposition: form-data; name=\"published_date\"\r\n\r\n");
                postData.append(publishedDate).append("\r\n");

                // 이미지 파일 추가
                postData.append("--").append(boundary).append("\r\n");
                postData.append("Content-Disposition: form-data; name=\"image\"; filename=\"test.jpg\"\r\n");
                postData.append("Content-Type: image/jpeg\r\n\r\n");

                // 바이트 데이터 추가
                if (imageBytes == null || imageBytes.length == 0) {
                    android.util.Log.e("MainActivity", "Image bytes are null or empty");
                    return false;
                }
                
                conn.getOutputStream().write(postData.toString().getBytes());
                conn.getOutputStream().write(imageBytes);
                conn.getOutputStream().write("\r\n".getBytes());
                conn.getOutputStream().write(("--" + boundary + "--\r\n").getBytes());
                conn.getOutputStream().flush();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    return true;
                } else {
                    android.util.Log.e("MainActivity", "Upload failed with response code: " + responseCode);
                    return false;
                }

            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error in PutPost: " + e.getMessage(), e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                // 업로드 성공 시 다운로드 자동 호출
                onClickDownload(null);
            } else {
                // 업로드 실패 시 사용자에게 알림
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "업로드 실패: 이미지가 없거나 잘못되었습니다", Toast.LENGTH_LONG).show();
                });
            }
        }
    }
}