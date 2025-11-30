package com.example.photoviewer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditPostActivity extends AppCompatActivity {

    private static final String SERVER_URL = "http://10.0.2.2:8000/api_root/Post/";
    private static final String TOKEN = "696e79eba229f1fab1b970f01b24f14c1903a28f";
    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText etTitle, etText;
    private ImageView ivPreview;
    private Button btnSelectImage, btnUpdate;
    private ProgressBar progressBar;

    private int postId;
    private Bitmap selectedBitmap;
    private boolean imageChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        etTitle = findViewById(R.id.etTitle);
        etText = findViewById(R.id.etText);
        ivPreview = findViewById(R.id.ivPreview);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnUpdate = findViewById(R.id.btnUpdate);
        progressBar = findViewById(R.id.progressBar);

        // Intent로 전달받은 데이터
        postId = getIntent().getIntExtra("post_id", -1);
        String postTitle = getIntent().getStringExtra("post_title");
        String postText = getIntent().getStringExtra("post_text");
        String postImage = getIntent().getStringExtra("post_image");

        // 기존 데이터 표시
        etTitle.setText(postTitle);
        etText.setText(postText);

        // 기존 이미지 로드
        if (postImage != null && !postImage.isEmpty()) {
            new LoadImageTask().execute(postImage);
        }

        // 이미지 선택 버튼
        btnSelectImage.setOnClickListener(v -> openGallery());

        // 업데이트 버튼
        btnUpdate.setOnClickListener(v -> updatePost());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                ivPreview.setImageBitmap(selectedBitmap);
                imageChanged = true;
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "이미지 로드 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updatePost() {
        String title = etTitle.getText().toString().trim();
        String text = etText.getText().toString().trim();

        if (title.isEmpty() || text.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        new UpdatePostTask().execute(title, text);
    }

    // 이미지 로드 AsyncTask
    private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {
            String imageUrl = params[0];
            try {
                if (imageUrl.startsWith("/")) {
                    imageUrl = "http://10.0.2.2:8000" + imageUrl;
                }
                URL url = new URL(imageUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.connect();
                InputStream input = conn.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                conn.disconnect();
                return bitmap;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                selectedBitmap = bitmap;
                ivPreview.setImageBitmap(bitmap);
            }
        }
    }

    // 게시물 업데이트 AsyncTask
    private class UpdatePostTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            btnUpdate.setEnabled(false);
            btnSelectImage.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String title = params[0];
            String text = params[1];

            try {
                URL url = new URL(SERVER_URL + postId + "/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Authorization", "Token " + TOKEN);
                conn.setDoOutput(true);

                String boundary = "===" + System.currentTimeMillis() + "===";
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                // Title
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"title\"\r\n\r\n");
                dos.writeBytes(title + "\r\n");

                // Text
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"text\"\r\n\r\n");
                dos.writeBytes(text + "\r\n");

                // Published Date
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"published_date\"\r\n\r\n");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                dos.writeBytes(sdf.format(new Date()) + "\r\n");

                // Image (이미지가 변경된 경우에만 전송)
                if (imageChanged && selectedBitmap != null) {
                    dos.writeBytes("--" + boundary + "\r\n");
                    dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"image.jpg\"\r\n");
                    dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                    byte[] imageBytes = baos.toByteArray();
                    dos.write(imageBytes);
                    dos.writeBytes("\r\n");
                }

                dos.writeBytes("--" + boundary + "--\r\n");
                dos.flush();
                dos.close();

                int responseCode = conn.getResponseCode();
                conn.disconnect();
                return responseCode == 200; // PUT 성공 시 200 OK
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.GONE);
            btnUpdate.setEnabled(true);
            btnSelectImage.setEnabled(true);

            if (success) {
                Toast.makeText(EditPostActivity.this,
                        "게시물이 수정되었습니다", Toast.LENGTH_SHORT).show();
                finish(); // PostDetailActivity로 돌아가기
            } else {
                Toast.makeText(EditPostActivity.this,
                        "수정 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
