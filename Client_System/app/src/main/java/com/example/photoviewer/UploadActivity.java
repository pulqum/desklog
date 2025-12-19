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
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText editTitle;
    private EditText editText;
    private ImageView imagePreview;
    private Button btnSelectImage;
    private Button btnUpload;
    private Bitmap selectedBitmap;
    private String site_url = "http://10.0.2.2:8000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        // UI 요소 초기화
        editTitle = findViewById(R.id.editTitle);
        editText = findViewById(R.id.editText);
        imagePreview = findViewById(R.id.imagePreview);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnUpload = findViewById(R.id.btnUpload);

        // 이미지 선택 버튼
        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        // 업로드 버튼
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadPost();
            }
        });
    }

    // 갤러리 열기
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // 갤러리에서 이미지 선택 결과 처리
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                // URI에서 Bitmap으로 변환
                selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                // 이미지 미리보기
                imagePreview.setImageBitmap(selectedBitmap);
                imagePreview.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "이미지 로드 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 게시물 업로드
    private void uploadPost() {
        String title = editTitle.getText().toString().trim();
        String text = editText.getText().toString().trim();

        // 입력 검증
        if (title.isEmpty()) {
            Toast.makeText(this, "제목을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (text.isEmpty()) {
            Toast.makeText(this, "내용을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedBitmap == null) {
            Toast.makeText(this, "이미지를 선택하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 업로드 실행
        new UploadPostTask(selectedBitmap, title, text).execute();
        Toast.makeText(this, "업로드 중...", Toast.LENGTH_SHORT).show();
    }

    // 서버에 게시물 업로드하는 AsyncTask
    private class UploadPostTask extends AsyncTask<String, Void, Boolean> {
        private Bitmap bitmap;
        private String title;
        private String text;

        public UploadPostTask(Bitmap bitmap, String title, String text) {
            this.bitmap = bitmap;
            this.title = title;
            this.text = text;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            if (bitmap == null) {
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

                // 현재 시간을 published_date로 설정
                String publishedDate = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).format(new java.util.Date());

                // 이미지 데이터 준비
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
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
                postData.append("Content-Disposition: form-data; name=\"image\"; filename=\"upload.jpg\"\r\n");
                postData.append("Content-Type: image/jpeg\r\n\r\n");

                // 데이터 전송
                conn.getOutputStream().write(postData.toString().getBytes());
                conn.getOutputStream().write(imageBytes);
                conn.getOutputStream().write("\r\n".getBytes());
                conn.getOutputStream().write(("--" + boundary + "--\r\n").getBytes());
                conn.getOutputStream().flush();

                int responseCode = conn.getResponseCode();
                return responseCode == HttpURLConnection.HTTP_CREATED;

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(UploadActivity.this, "업로드 성공!", Toast.LENGTH_SHORT).show();
                // MainActivity로 돌아가기
                finish();
            } else {
                Toast.makeText(UploadActivity.this, "업로드 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
