package com.example.photoviewer;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClient {
    public static final String BASE_URL = "http://10.0.2.2:8000";
    private static final String PREFS_NAME = "DeskLogPrefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USERNAME = "username";

    private Context context;

    public ApiClient(Context context) {
        this.context = context;
    }

    public static void saveToken(Context context, String token, String username) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).putString(KEY_USERNAME, username).apply();
    }

    public static String getToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }

    public static String getUsername(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USERNAME, null);
    }

    public static void clearToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USERNAME).apply();
    }

    public static boolean isLoggedIn(Context context) {
        return getToken(context) != null;
    }

    private String getAuthToken() {
        return getToken(context);
    }

    public JSONObject login(String username, String password) throws Exception {
        URL url = new URL(BASE_URL + "/api-token-auth/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("password", password);

        OutputStream os = conn.getOutputStream();
        os.write(json.toString().getBytes(StandardCharsets.UTF_8));
        os.close();

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            JSONObject result = new JSONObject(response.toString());
            // JWT 토큰은 "access" 필드에 있음
            return result;
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder error = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                error.append(line);
            }
            reader.close();
            throw new Exception("Login failed: " + error.toString());
        }
    }

    public JSONObject signup(String username, String email, String password1, String password2) throws Exception {
        URL url = new URL(BASE_URL + "/signup/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("email", email);
        json.put("password1", password1);
        json.put("password2", password2);

        OutputStream os = conn.getOutputStream();
        os.write(json.toString().getBytes(StandardCharsets.UTF_8));
        os.close();

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return new JSONObject(response.toString());
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder error = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                error.append(line);
            }
            reader.close();
            throw new Exception("Signup failed: " + error.toString());
        }
    }

    public JSONArray getSessions() throws Exception {
        String token = getAuthToken();
        if (token == null) throw new Exception("Not logged in");

        URL url = new URL(BASE_URL + "/api_root/Post/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token); // JWT는 Bearer 사용
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return new JSONArray(response.toString());
        } else {
            throw new Exception("Failed to get sessions: " + responseCode);
        }
    }

    public JSONArray getSessionsList() throws Exception {
        String token = getAuthToken();
        if (token == null) throw new Exception("Not logged in");

        URL url = new URL(BASE_URL + "/api_root/Session/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token); // JWT는 Bearer 사용
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return new JSONArray(response.toString());
        } else {
            throw new Exception("Failed to get sessions: " + responseCode);
        }
    }

    public JSONObject getSessionDetail(int sessionId) throws Exception {
        String token = getAuthToken();
        if (token == null) throw new Exception("Not logged in");

        URL url = new URL(BASE_URL + "/api_root/Session/" + sessionId + "/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token); // JWT는 Bearer 사용
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return new JSONObject(response.toString());
        } else {
            throw new Exception("Failed to get session: " + responseCode);
        }
    }

    public boolean startSession() throws Exception {
        String token = getAuthToken();
        if (token == null) throw new Exception("Not logged in");

        URL url = new URL(BASE_URL + "/session/start/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token); // JWT는 Bearer 사용
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        // JSON 응답 또는 redirect 모두 성공으로 처리
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // JSON 응답 확인
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            JSONObject json = new JSONObject(response.toString());
            return json.optBoolean("success", true);
        }
        return responseCode == HttpURLConnection.HTTP_OK || responseCode == 302; // 302 = HTTP Found (redirect)
    }

    public boolean checkActiveSession() throws Exception {
        String token = getAuthToken();
        if (token == null) return false;

        JSONArray sessions = getSessionsList();
        for (int i = 0; i < sessions.length(); i++) {
            JSONObject session = sessions.getJSONObject(i);
            if (session.optBoolean("is_active", false)) {
                return true;
            }
        }
        return false;
    }
}

