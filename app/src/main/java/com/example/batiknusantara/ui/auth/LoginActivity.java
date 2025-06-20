package com.example.batiknusantara.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.batiknusantara.MainActivity;
import com.example.batiknusantara.api.ApiClient;
import com.example.batiknusantara.api.ApiService;
import com.example.batiknusantara.api.request.LoginRequest;
import com.example.batiknusantara.api.response.LoginResponse;
import com.example.batiknusantara.databinding.ActivityLoginBinding;
import com.example.batiknusantara.utils.SessionManager;
import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText().toString();
        String password = binding.etPassword.getText().toString();

        android.util.Log.d("LOGIN_DEBUG", "Attempting login with email: " + email);

        if (email.isEmpty() || password.isEmpty()) {
            Toasty.warning(this, "Please fill all fields", Toasty.LENGTH_SHORT, true).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        LoginRequest request = new LoginRequest(email, password);
        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                binding.progressBar.setVisibility(View.GONE);

                // Add debug logs
                android.util.Log.d("LOGIN_DEBUG", "Response code: " + response.code());
                if (!response.isSuccessful()) {
                    try {
                        android.util.Log.e("LOGIN_DEBUG", "Error body: " + response.errorBody().string());
                    } catch (Exception e) {
                        android.util.Log.e("LOGIN_DEBUG", "Error reading error body", e);
                    }
                }

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    android.util.Log.d("LOGIN_DEBUG", "Response body: " + new com.google.gson.Gson().toJson(loginResponse));

                    if (loginResponse.status) {
                        sessionManager.createLoginSession(
                                loginResponse.data.id,
                                loginResponse.data.nama,
                                loginResponse.data.email
                        );
                        Toasty.success(LoginActivity.this, "Login berhasil!", Toasty.LENGTH_SHORT, true).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("navigate_home", true);
                        startActivity(intent);
                        finish();
                    } else {
                        android.util.Log.d("LOGIN_DEBUG", "Login failed: " + loginResponse.message);
                        Toasty.error(LoginActivity.this, loginResponse.message, Toasty.LENGTH_SHORT, true).show();
                    }
                } else {
                    android.util.Log.e("LOGIN_DEBUG", "Response not successful or body null");
                    Toasty.error(LoginActivity.this, "Login failed", Toasty.LENGTH_SHORT, true).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                android.util.Log.e("LOGIN_DEBUG", "Network error", t);
                Toasty.error(LoginActivity.this, "Network error: " + t.getMessage(), Toasty.LENGTH_SHORT, true).show();
            }
        });
    }
}
