package com.example.batiknusantara.ui.order;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.batiknusantara.api.response.OrderDetailResponse;
import com.example.batiknusantara.api.response.OrderHistoryResponse;
import com.example.batiknusantara.databinding.ActivityOrderDetailBinding;
import com.example.batiknusantara.api.ApiClient;
import com.example.batiknusantara.api.ApiService;
import com.example.batiknusantara.model.Product;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {
    private ActivityOrderDetailBinding binding;
    private ApiService apiService;
    private List<Product> productList = new ArrayList<>();
    private List<Integer> qtyList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getClient().create(ApiService.class);
        OrderHistoryResponse.OrderData order = (OrderHistoryResponse.OrderData) getIntent().getSerializableExtra("order_data");
        if (order == null) {
            finish();
            return;
        }
        showOrderDetail(order);
        loadOrderProducts(order.trans_id, order.email);
    }

    private void showOrderDetail(OrderHistoryResponse.OrderData order) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        formatter.setMinimumFractionDigits(0);
        String currencySymbol = formatter.getCurrency().getSymbol();
        String formattedSymbol = currencySymbol + " ";
        formatter.setCurrency(java.util.Currency.getInstance("IDR"));
        java.text.DecimalFormatSymbols symbols = ((java.text.DecimalFormat) formatter).getDecimalFormatSymbols();
        symbols.setCurrencySymbol(formattedSymbol);
        ((java.text.DecimalFormat) formatter).setDecimalFormatSymbols(symbols);

        binding.tvOrderId.setText("Order ID: " + order.trans_id);
        binding.tvOrderDate.setText("Tanggal: " + order.tgl_order);
        binding.tvOrderStatus.setText("Status: " + getStatusText(order.status));
        binding.tvOrderSubtotal.setText("Subtotal: " + formatter.format(order.subtotal));
        binding.tvOrderOngkir.setText("Ongkir: " + formatter.format(order.ongkir));
        binding.tvOrderTotal.setText("Total Bayar: " + formatter.format(order.total_bayar));
        binding.tvOrderAlamat.setText("Alamat: " + order.alamat_kirim);
        binding.tvOrderTelp.setText("Telp: " + order.telp_kirim);
        binding.tvOrderKota.setText("Kota: " + order.kota);
        binding.tvOrderProvinsi.setText("Provinsi: " + order.provinsi);
        binding.tvOrderKodepos.setText("Kode Pos: " + order.kodepos);
        binding.tvOrderLamaKirim.setText("Lama Kirim: " + order.lamakirim);
        binding.tvOrderMetode.setText("Metode Bayar: " + getMetodeText(order.metodebayar));
    }

    private void loadOrderProducts(int transId, String email) {
        apiService.getOrderDetail(transId, email).enqueue(new Callback<OrderDetailResponse>() {
            @Override
            public void onResponse(Call<OrderDetailResponse> call, Response<OrderDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().status) {
                    productList.clear();
                    qtyList.clear();
                    for (OrderDetailResponse.Item item : response.body().data) {
                        // Build Product object from item fields if product is null
                        Product product = item.product;
                        if (product == null) {
                            product = new Product();
                            product.setKode(item.kode_brg);
                            // Try to set other fields if available in item
                            try {
                                java.lang.reflect.Field merkField = item.getClass().getDeclaredField("merk");
                                merkField.setAccessible(true);
                                Object merkValue = merkField.get(item);
                                if (merkValue != null) product.setMerk(merkValue.toString());
                            } catch (Exception ignored) {}
                            try {
                                java.lang.reflect.Field fotoField = item.getClass().getDeclaredField("foto");
                                fotoField.setAccessible(true);
                                Object fotoValue = fotoField.get(item);
                                if (fotoValue != null) product.setFoto_url(fotoValue.toString());
                            } catch (Exception ignored) {}
                            product.setHargajual(item.harga_jual);
                        }
                        productList.add(product);
                        qtyList.add(item.qty);
                    }
                    setupProductRecycler();
                }
            }
            @Override
            public void onFailure(Call<OrderDetailResponse> call, Throwable t) {
                // Optional: show error
            }
        });
    }

    private void setupProductRecycler() {
        OrderDetailProductAdapter adapter = new OrderDetailProductAdapter(productList, qtyList);
        binding.rvOrderProducts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrderProducts.setAdapter(adapter);
    }

    private String getStatusText(int status) {
        switch (status) {
            case 0: return "Belum Dibayar";
            case 1: return "Diproses";
            case 2: return "Dikirim";
            case 3: return "Selesai";
            default: return "-";
        }
    }

    private String getMetodeText(int metode) {
        switch (metode) {
            case 0: return "Transfer";
            case 1: return "COD";
            default: return "-";
        }
    }
}
