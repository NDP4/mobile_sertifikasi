package com.example.batiknusantara.ui.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.batiknusantara.api.ApiClient;
import com.example.batiknusantara.api.ApiService;
import com.example.batiknusantara.api.response.OrderHistoryResponse;
import com.example.batiknusantara.databinding.ActivityOrderHistoryBinding;
import com.example.batiknusantara.utils.SessionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderHistoryActivity extends AppCompatActivity {
    private ActivityOrderHistoryBinding binding;
    private OrderHistoryAdapter adapter;
    private ApiService apiService;
    private SessionManager sessionManager;
    private List<OrderHistoryResponse.OrderData> orderList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);
        adapter = new OrderHistoryAdapter(orderList, order -> {
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra("order_data", order);
            startActivity(intent);
        });
        binding.rvOrderHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrderHistory.setAdapter(adapter);
        loadOrderHistory();
    }

    private void loadOrderHistory() {
        String email = sessionManager.getEmail();
        if (email == null) {
            Toast.makeText(this, "Anda belum login", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        binding.progressBar.setVisibility(View.VISIBLE);
        apiService.getOrderHistory(email).enqueue(new Callback<OrderHistoryResponse>() {
            @Override
            public void onResponse(Call<OrderHistoryResponse> call, Response<OrderHistoryResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().status) {
                    orderList.clear();
                    orderList.addAll(response.body().data);
                    // Sort orderList by trans_id descending (latest first)
                    Collections.sort(orderList, new Comparator<OrderHistoryResponse.OrderData>() {
                        @Override
                        public int compare(OrderHistoryResponse.OrderData o1, OrderHistoryResponse.OrderData o2) {
                            return Integer.compare(o2.trans_id, o1.trans_id);
                        }
                    });
                    adapter.notifyDataSetChanged();
                    binding.tvEmpty.setVisibility(orderList.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onFailure(Call<OrderHistoryResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }
}
