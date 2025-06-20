package com.example.batiknusantara.ui.auth;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.batiknusantara.api.ApiClient;
import com.example.batiknusantara.api.ApiService;
import com.example.batiknusantara.api.request.RegisterRequest;
import com.example.batiknusantara.api.response.RegisterResponse;
import com.example.batiknusantara.databinding.ActivityRegisterBinding;
import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private ApiService apiService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getClient().create(ApiService.class);

        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.btnLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String nama = binding.etNama.getText().toString();
        String email = binding.etEmail.getText().toString();
        String password = binding.etPassword.getText().toString();

        if (nama.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toasty.warning(this, "Please fill all fields", Toasty.LENGTH_SHORT, true).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        RegisterRequest request = new RegisterRequest(nama, email, password);
        apiService.register(request)
            .enqueue(new Callback<RegisterResponse>() {
                @Override
                public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                    binding.progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        RegisterResponse registerResponse = response.body();
                        if (registerResponse.status) {
                            Toasty.success(RegisterActivity.this, registerResponse.message, Toasty.LENGTH_SHORT, true).show();
                            finish();
                        } else {
                            Toasty.error(RegisterActivity.this, registerResponse.message, Toasty.LENGTH_SHORT, true).show();
                        }
                    } else {
                        Toasty.error(RegisterActivity.this, "Registration failed", Toasty.LENGTH_SHORT, true).show();
                    }
                }

                @Override
                public void onFailure(Call<RegisterResponse> call, Throwable t) {
                    binding.progressBar.setVisibility(View.GONE);
                    Toasty.error(RegisterActivity.this, "Network error", Toasty.LENGTH_SHORT, true).show();
                }
            });
    }
}
