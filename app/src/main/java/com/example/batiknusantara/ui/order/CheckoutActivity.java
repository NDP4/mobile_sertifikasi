package com.example.batiknusantara.ui.order;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.batiknusantara.R;
import com.example.batiknusantara.adapter.OrderProductAdapter;
import com.example.batiknusantara.api.ApiClient;
import com.example.batiknusantara.api.ApiClientRajaOngkir;
import com.example.batiknusantara.api.ApiService;
import com.example.batiknusantara.api.ApiServiceRajaOngkir;
import com.example.batiknusantara.api.request.OrderCreateRequest;
import com.example.batiknusantara.api.response.OrderCreateResponse;
import com.example.batiknusantara.api.response.RajaOngkirCostResponse;
import com.example.batiknusantara.api.response.RajaOngkirResponse;
import com.example.batiknusantara.databinding.ActivityCheckoutBinding;
import com.example.batiknusantara.model.City;
import com.example.batiknusantara.model.Product;
import com.example.batiknusantara.model.Province;
import com.example.batiknusantara.utils.CartManager;
import com.example.batiknusantara.utils.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import es.dmoral.toasty.Toasty;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutActivity extends AppCompatActivity {
    private ActivityCheckoutBinding binding;
    private ApiServiceRajaOngkir rajaOngkirService;
    private ApiService apiService;
    private SessionManager sessionManager;
    private CartManager cartManager;
    private List<CartManager.CartItem> cartItems = new ArrayList<>();
    private List<Province> provinces = new ArrayList<>();
    private List<City> cities = new ArrayList<>();
    private List<String> provinsiList = new ArrayList<>();
    private List<String> provinsiIdList = new ArrayList<>();
    private List<String> kotaList = new ArrayList<>();
    private List<String> kotaIdList = new ArrayList<>();
    private String selectedProvinsiId;
    private String selectedKotaId;
    private double ongkir = 0;
    private double subtotal = 0;
    private String lamaKirim = "-";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rajaOngkirService = ApiClientRajaOngkir.getClient().create(ApiServiceRajaOngkir.class);
        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);
        cartManager = new CartManager(this);

        // Cek apakah ini mode Buy Now
        Product buyNowProduct = (Product) getIntent().getSerializableExtra("buy_now_product");
        int buyNowQty = getIntent().getIntExtra("buy_now_quantity", 1);
        if (buyNowProduct != null) {
            // Mode Buy Now: hanya produk ini yang di-checkout
            cartItems = new ArrayList<>();
            CartManager.CartItem item = new CartManager.CartItem(buyNowProduct, buyNowQty);
            cartItems.add(item);
        } else {
            // Mode normal: checkout seluruh cart
            cartItems = new ArrayList<>(cartManager.getCartItems().values());
        }
        setupSpinnerProvinsi();
        setupProductList();
        // Ambil subtotal dari cartItems (bukan cartManager jika buy now)
        subtotal = 0;
        for (CartManager.CartItem item : cartItems) {
            double hargaJual = item.product.getHargajual();
            subtotal += hargaJual * item.quantity;
        }
        updateSubtotal();
        updateTotal();
        setupCheckoutButton();
    }

    private void setupProductList() {
        // Gunakan cartItems yang sudah di-set di onCreate
        binding.rvOrderProducts.setLayoutManager(new LinearLayoutManager(this));
        OrderProductAdapter adapter = new OrderProductAdapter(cartItems);
        binding.rvOrderProducts.setAdapter(adapter);
    }

    private void updateSubtotal() {
        // Selalu update field subtotal dari cart
        CartManager cartManager = new CartManager(this);
        subtotal = 0;
        for (CartManager.CartItem item : cartManager.getCartItems().values()) {
            double hargaJual = item.product.getHargajual();
            subtotal += hargaJual * item.quantity;
        }
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        formatter.setMinimumFractionDigits(0);
        String currencySymbol = formatter.getCurrency().getSymbol();
        String formattedSymbol = currencySymbol + " ";
        formatter.setCurrency(java.util.Currency.getInstance("IDR"));
        DecimalFormatSymbols symbols = ((DecimalFormat) formatter).getDecimalFormatSymbols();
        symbols.setCurrencySymbol(formattedSymbol);
        ((DecimalFormat) formatter).setDecimalFormatSymbols(symbols);
        binding.tvSubtotal.setText(formatter.format(subtotal));
        updateTotal();
    }

    private void updateTotal() {
        double total = subtotal + ongkir;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        formatter.setMinimumFractionDigits(0);
        String currencySymbol = formatter.getCurrency().getSymbol();
        String formattedSymbol = currencySymbol + " ";
        formatter.setCurrency(java.util.Currency.getInstance("IDR"));
        DecimalFormatSymbols symbols = ((DecimalFormat) formatter).getDecimalFormatSymbols();
        symbols.setCurrencySymbol(formattedSymbol);
        ((DecimalFormat) formatter).setDecimalFormatSymbols(symbols);
        binding.tvTotalBayar.setText(formatter.format(total));
    }

    private void setupSpinnerProvinsi() {
        rajaOngkirService.getProvinces().enqueue(new Callback<RajaOngkirResponse<List<Province>>>() {
            @Override
            public void onResponse(Call<RajaOngkirResponse<List<Province>>> call, Response<RajaOngkirResponse<List<Province>>> response) {
                provinsiList.clear();
                provinsiIdList.clear();
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Province> provinces = response.body().getData();
                    for (Province prov : provinces) {
                        provinsiList.add(prov.getProvince());
                        provinsiIdList.add(prov.getProvinceId());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(CheckoutActivity.this, android.R.layout.simple_spinner_dropdown_item, provinsiList);
                    binding.spinnerProvinsi.setAdapter(adapter);
                    binding.spinnerProvinsi.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedProvinsiId = provinsiIdList.get(position);
                            setupSpinnerKota(selectedProvinsiId);
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                } else {
                    Toasty.error(CheckoutActivity.this, "Gagal load provinsi", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<RajaOngkirResponse<List<Province>>> call, Throwable t) {
                Toasty.error(CheckoutActivity.this, "Gagal load provinsi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpinnerKota(String provinsiId) {
        rajaOngkirService.getCitiesByProvince("cities", provinsiId).enqueue(new Callback<RajaOngkirResponse<List<City>>>() {
            @Override
            public void onResponse(Call<RajaOngkirResponse<List<City>>> call, Response<RajaOngkirResponse<List<City>>> response) {
                kotaList.clear();
                kotaIdList.clear();
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<City> cities = response.body().getData();
                    for (City city : cities) {
                        kotaList.add(city.getCityName());
                        kotaIdList.add(city.getCityId());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(CheckoutActivity.this, android.R.layout.simple_spinner_dropdown_item, kotaList);
                    binding.spinnerKota.setAdapter(adapter);
                    binding.spinnerKota.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (parent.getId() == R.id.spinnerKota) {
                                if (position > 0) {
                                    selectedKotaId = kotaIdList.get(position - 1);
                                    int totalWeight = new CartManager(CheckoutActivity.this).getCartTotalWeight();
                                    getShippingCost(selectedKotaId, totalWeight);
                                }
                            }
                            //getShippingCost(selectedKotaId);
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                } else {
                    Toasty.error(CheckoutActivity.this, "Gagal load kota", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<RajaOngkirResponse<List<City>>> call, Throwable t) {
                Toasty.error(CheckoutActivity.this, "Gagal load kota", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void getShippingCost(String cityId, int weight) {
        Log.d("ShippingCost", "City ID: " + cityId + ", Weight: " + weight);
        if (cityId == null || cityId.isEmpty() || weight <= 0) {
            Log.d("ShippingCost", "Invalid input");
            return;
        }
        try {
            JSONObject json = new JSONObject();
            json.put("destination", cityId);
            json.put("weight", weight);
            Log.d("ShippingCost", "Request JSON: " + json.toString());
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    json.toString(), okhttp3.MediaType.parse("application/json"));

            rajaOngkirService.getCost(body).enqueue(new Callback<RajaOngkirCostResponse>() {
                @Override
                public void onResponse(Call<RajaOngkirCostResponse> call, Response<RajaOngkirCostResponse> response) {
                    Log.d("ShippingCost", "Response: " + response.toString());
                    if (response.isSuccessful() && response.body() != null && response.body().status) {
                        RajaOngkirCostResponse.Data data = response.body().data;
                        if (data != null && data.results != null && !data.results.isEmpty()) {
                            RajaOngkirCostResponse.Result result = data.results.get(0);
                            if (result.costs != null && !result.costs.isEmpty()) {
                                // Ambil layanan REG (atau yang pertama)
                                RajaOngkirCostResponse.CostItem reg = null;
                                for (RajaOngkirCostResponse.CostItem c : result.costs) {
                                    if ("REG".equalsIgnoreCase(c.service)) {
                                        reg = c;
                                        break;
                                    }
                                }
                                if (reg == null) reg = result.costs.get(0);
                                if (reg.cost != null && !reg.cost.isEmpty()) {
                                    ongkir = reg.cost.get(0).value;
                                    lamaKirim = reg.cost.get(0).etd + " hari";
                                    // Tampilkan ke UI
                                    NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
                                    formatter.setMinimumFractionDigits(0);
                                    String currencySymbol = formatter.getCurrency().getSymbol();
                                    String formattedSymbol = currencySymbol + " ";
                                    formatter.setCurrency(java.util.Currency.getInstance("IDR"));
                                    DecimalFormatSymbols symbols = ((DecimalFormat) formatter).getDecimalFormatSymbols();
                                    symbols.setCurrencySymbol(formattedSymbol);
                                    ((DecimalFormat) formatter).setDecimalFormatSymbols(symbols);

                                    binding.tvOngkir.setText(formatter.format(ongkir));
                                    binding.tvLamaKirim.setText(lamaKirim);

                                    // Update total bayar
                                    updateTotal();
                                }
                            }
                        }
                    } else {
                        Toasty.error(CheckoutActivity.this, "Gagal hitung ongkir", Toast.LENGTH_SHORT).show();
                    }

                }
                @Override
                public void onFailure(Call<RajaOngkirCostResponse> call, Throwable t) {
                    Toasty.error(CheckoutActivity.this, "Gagal hitung ongkir", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
                Toasty.error(this, "Gagal hitung ongkir", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupCheckoutButton() {
        binding.btnProsesCheckout.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                Toasty.warning(this, "Anda harus login terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }
            OrderCreateRequest request = new OrderCreateRequest();
            request.email = sessionManager.getEmail();
            request.subtotal = subtotal;
            request.ongkir = ongkir;
            request.total_bayar = subtotal + ongkir;
            request.alamat_kirim = binding.etAlamat.getText().toString();
            request.telp_kirim = binding.etTelp.getText().toString();
            request.kota = binding.spinnerKota.getSelectedItem() != null ? binding.spinnerKota.getSelectedItem().toString() : "";
            request.provinsi = binding.spinnerProvinsi.getSelectedItem() != null ? binding.spinnerProvinsi.getSelectedItem().toString() : "";
            request.lamakirim = lamaKirim;
            request.kodepos = binding.etKodepos.getText().toString();
            // Metode bayar: 0 = Transfer, 1 = COD
            int metodeBayarInt = binding.rbTransfer.isChecked() ? 0 : 1;
            request.metodebayar = String.valueOf(metodeBayarInt);
            request.items = new ArrayList<>();
            for (CartManager.CartItem item : cartItems) {
                OrderCreateRequest.Item reqItem = new OrderCreateRequest.Item();
                reqItem.kode_brg = item.product.getKode();
                reqItem.harga_jual = item.product.getHargajual();
                reqItem.qty = item.quantity;
                reqItem.bayar = (item.product.getHargajual() - (item.product.getHargajual() * item.product.getDiskonjual() / 100.0)) * item.quantity;
                request.items.add(reqItem);
            }
            binding.btnProsesCheckout.setEnabled(false);
            apiService.createOrder(request).enqueue(new Callback<OrderCreateResponse>() {
                @Override
                public void onResponse(Call<OrderCreateResponse> call, Response<OrderCreateResponse> response) {
                    binding.btnProsesCheckout.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null && response.body().status) {
                        cartManager.clearCart();
                        Toasty.success(CheckoutActivity.this, "Order berhasil dibuat!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toasty.error(CheckoutActivity.this, "Gagal membuat order", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<OrderCreateResponse> call, Throwable t) {
                    binding.btnProsesCheckout.setEnabled(true);
                    Toasty.error(CheckoutActivity.this, "Gagal membuat order: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}