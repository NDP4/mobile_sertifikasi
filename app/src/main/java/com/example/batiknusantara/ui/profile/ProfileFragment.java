package com.example.batiknusantara.ui.profile;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.batiknusantara.R;
import com.example.batiknusantara.databinding.FragmentProfileBinding;
import com.example.batiknusantara.ui.auth.LoginActivity;
import com.example.batiknusantara.api.ApiClient;
import com.example.batiknusantara.api.ApiService;
import com.example.batiknusantara.api.response.LoginResponse;
import com.example.batiknusantara.api.response.ProfileResponse;
import com.example.batiknusantara.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import es.dmoral.toasty.Toasty;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private SessionManager sessionManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private ApiService apiService;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        sessionManager = new SessionManager(requireContext());

        TextView watermark = new TextView(requireContext());
        watermark.setText("By Nur Dwi Priyambodo\nCoreX");
        watermark.setTextSize(10f);
        watermark.setTextColor(getResources().getColor(R.color.dark_gray));
        watermark.setGravity(Gravity.CENTER);
        watermark.setPadding(0, 32, 0, 16);

        LinearLayout rootLayout = (LinearLayout) binding.getRoot().findViewById(R.id.llProfile);
        rootLayout.addView(watermark);

        // ApiService dengan timeout 60 detik
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://apisertif.ndp.my.id/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        apiService = retrofit.create(ApiService.class);

        // Set avatar default pakai Glide tanpa background circle
        Glide.with(this)
                .load(R.drawable.ic_avatar_default)
                .circleCrop()
                .into(binding.imgAvatar);

        // Info user
        String name = sessionManager.getName();
//        String email = sessionManager.getEmail();
        if (name != null && !name.isEmpty()) {
            binding.tvUserInfo.setText("Login sebagai: " + name);
        } else {
//            binding.tvUserInfo.setText("Login sebagai: " + email);
        }

        // Lokasi
        binding.tvLocation.setText("Lokasi: -");
        checkLocationPermissionAndFetch();

        // Tombol edit avatar (tidak difungsikan)
        binding.btnEditAvatar.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Edit avatar belum tersedia", Toast.LENGTH_SHORT).show();
        });

        // Tombol edit profile
        binding.btnEditProfile.setOnClickListener(v -> {
            // Navigasi ke EditProfileActivity
            startActivity(new android.content.Intent(getActivity(), com.example.batiknusantara.ui.profile.EditProfileActivity.class));
        });

        // Tombol riwayat pemesanan (placeholder)
        binding.btnOrderHistory.setOnClickListener(v -> {
//            Toast.makeText(getContext(), "Riwayat pemesanan belum tersedia", Toast.LENGTH_SHORT).show();
            // Navigasi ke OrderHistoryActivity jika sudah tersedia
            startActivity(new Intent(getActivity(), com.example.batiknusantara.ui.order.OrderHistoryActivity.class));
        });

        // Tombol logout
        binding.btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            // Redirect to HomeFragment after logout
            if (getActivity() != null) {
                androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(getActivity(), com.example.batiknusantara.R.id.nav_host_fragment_activity_main);
                navController.navigate(com.example.batiknusantara.R.id.navigation_home);
            }
        });

        return root;
    }

    private void checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchLocation();
        }
    }

    private void fetchLocation() {
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(getContext(), "Aktifkan GPS untuk mendapatkan lokasi", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (!isAdded()) return; // prevent crash if fragment is detached
                try {
                    Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        String city = address.getSubAdminArea();
                        String province = address.getAdminArea();
                        String lokasi = "Lokasi: " + (city != null ? city : "-") + ", " + (province != null ? province : "-");
                        binding.tvLocation.setText(lokasi);
                    }
                } catch (Exception e) {
                    Log.e("ProfileFragment", "Geocoder error", e);
                    if (isAdded()) binding.tvLocation.setText("Lokasi: -");
                }
                // Stop update setelah dapat lokasi
                if (isAdded() && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.removeUpdates(this);
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                Toast.makeText(getContext(), "Izin lokasi diperlukan untuk menampilkan lokasi", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (Exception e) {
                Log.e("ProfileFragment", "Error removing location updates", e);
            }
        }
        binding = null;
    }
}