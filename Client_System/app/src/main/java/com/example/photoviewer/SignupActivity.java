package com.example.photoviewer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

public class SignupActivity extends AppCompatActivity {
    private EditText etUsername, etEmail, etPassword1, etPassword2;
    private Button btnSignup;
    private ProgressBar progressBar;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        apiClient = new ApiClient(this);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword1 = findViewById(R.id.etPassword1);
        etPassword2 = findViewById(R.id.etPassword2);
        btnSignup = findViewById(R.id.btnSignup);
        progressBar = findViewById(R.id.progressBar);

        btnSignup.setOnClickListener(v -> performSignup());
    }

    private void performSignup() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password1 = etPassword1.getText().toString();
        String password2 = etPassword2.getText().toString();

        if (username.isEmpty() || email.isEmpty() || password1.isEmpty() || password2.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password1.equals(password2)) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        new SignupTask().execute(username, email, password1, password2);
    }

    private class SignupTask extends AsyncTask<String, Void, Boolean> {
        private String errorMessage = "";

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            btnSignup.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                apiClient.signup(params[0], params[1], params[2], params[3]);
                // 회원가입 성공 후 자동 로그인
                JSONObject loginResponse = apiClient.login(params[0], params[2]);
                String token = loginResponse.optString("access", "");
                if (!token.isEmpty()) {
                    ApiClient.saveToken(SignupActivity.this, token, params[0]);
                    return true;
                }
            } catch (Exception e) {
                errorMessage = e.getMessage();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.GONE);
            btnSignup.setEnabled(true);

            if (success) {
                Toast.makeText(SignupActivity.this, "회원가입 성공!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SignupActivity.this, SessionListActivity.class));
                finish();
            } else {
                Toast.makeText(SignupActivity.this, "회원가입 실패: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }
}

