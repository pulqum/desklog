package com.example.photoviewer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private Button btnLogin, btnSignup;
    private ProgressBar progressBar;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 이미 로그인되어 있으면 세션 목록으로 이동
        if (ApiClient.isLoggedIn(this)) {
            startActivity(new Intent(this, SessionListActivity.class));
            finish();
            return;
        }

        apiClient = new ApiClient(this);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignup = findViewById(R.id.btnSignup);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> performLogin());
        btnSignup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void performLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "아이디와 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        new LoginTask().execute(username, password);
    }

    private class LoginTask extends AsyncTask<String, Void, Boolean> {
        private String errorMessage = "";

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                JSONObject response = apiClient.login(params[0], params[1]);
                // JWT 토큰은 "access" 필드에 있음
                String token = response.optString("access", "");
                if (!token.isEmpty()) {
                    ApiClient.saveToken(LoginActivity.this, token, params[0]);
                    return true;
                } else {
                    errorMessage = "토큰을 받을 수 없습니다";
                }
            } catch (Exception e) {
                errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "로그인에 실패했습니다";
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);

            if (success) {
                Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, SessionListActivity.class));
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "로그인 실패: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }
}

