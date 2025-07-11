package com.example.batiknusantara.ui.profile;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.batiknusantara.api.ApiClient;
import com.example.batiknusantara.api.ApiService;
import com.example.batiknusantara.api.response.ProfileResponse;
import com.example.batiknusantara.databinding.ActivityEditProfileBinding;
import com.example.batiknusantara.utils.SessionManager;
import org.json.JSONObject;
import es.dmoral.toasty.Toasty;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {
    private ActivityEditProfileBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up back button
        binding.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);
        int userId = sessionManager.getUserId();
        loadProfile(userId);
        binding.btnSave.setOnClickListener(v -> saveProfile(userId));
    }

    private void loadProfile(int userId) {
        apiService.getProfile(userId).enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().status) {
                    ProfileResponse.Data data = response.body().data;
                    binding.etNama.setText(data.nama);
                    binding.etAlamat.setText(data.alamat);
                    binding.etKota.setText(data.kota);
                    binding.etProvinsi.setText(data.provinsi);
                    binding.etKodepos.setText(data.kodepos);
                    binding.etTelp.setText(data.telp);
                    // Email dari sessionManager dan tidak bisa diedit
                    binding.etEmail.setText(sessionManager.getEmail());
                    binding.etEmail.setEnabled(false);
                }
            }
            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {}
        });
    }

    private void saveProfile(int userId) {
        try {
            JSONObject json = new JSONObject();
            json.put("nama", binding.etNama.getText().toString());
            json.put("alamat", binding.etAlamat.getText().toString());
            json.put("kota", binding.etKota.getText().toString());
            json.put("provinsi", binding.etProvinsi.getText().toString());
            json.put("kodepos", binding.etKodepos.getText().toString());
            json.put("telp", binding.etTelp.getText().toString());
            // Email tidak dikirim karena tidak bisa diedit
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), json.toString());
            apiService.updateProfile(userId, body).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        // ambil nama baru dari input
                        String newName = binding.etNama.getText().toString();
                        // update session manager dengan nama baru
                        sessionManager.setName(newName);
                        Toasty.success(EditProfileActivity.this, "Profile berhasil diupdate", Toasty.LENGTH_SHORT, true).show();
                        finish();
                    } else {
                        Toasty.error(EditProfileActivity.this, "Gagal update profile", Toasty.LENGTH_SHORT, true).show();
                    }
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toasty.error(EditProfileActivity.this, "Network error", Toasty.LENGTH_SHORT, true).show();
                }
            });
        } catch (Exception e) {
            Toasty.error(EditProfileActivity.this, "Error", Toasty.LENGTH_SHORT, true).show();
        }
    }
}
