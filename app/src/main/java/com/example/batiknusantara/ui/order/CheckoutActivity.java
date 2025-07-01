package com.example.batiknusantara.ui.order;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.batiknusantara.adapter.OrderProductAdapter;
import com.example.batiknusantara.api.ApiClient;
import com.example.batiknusantara.api.ApiClientRajaOngkir;
import com.example.batiknusantara.api.ApiService;
import com.example.batiknusantara.api.ApiServiceRajaOngkir;
import com.example.batiknusantara.api.request.OrderCreateRequest;
import com.example.batiknusantara.api.response.DestinationResponse;
import com.example.batiknusantara.api.response.OrderCreateResponse;
import com.example.batiknusantara.api.response.RajaOngkirCostResponse;
import com.example.batiknusantara.databinding.ActivityCheckoutBinding;
import com.example.batiknusantara.model.Destination;
import com.example.batiknusantara.model.Product;
import com.example.batiknusantara.utils.CartManager;
import com.example.batiknusantara.utils.SessionManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.dmoral.toasty.Toasty;
import okhttp3.MediaType;
import okhttp3.RequestBody;
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
    private List<Destination> destinationList = new ArrayList<>();
    private List<String> destinationLabelList = new ArrayList<>();
    private double ongkir = 0;
    private double subtotal = 0;
    private String lamaKirim = "-";
    private int selectedDestinationId = -1;

    private static final String DEST_CACHE_KEY = "destination_cache";
    private static final String DEST_CACHE_TIME_KEY = "destination_cache_time";
    private static final long DEST_CACHE_VALID_MS = 24 * 60 * 60 * 1000; // 1 hari
    private SharedPreferences prefs;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rajaOngkirService = ApiClientRajaOngkir.getClient().create(ApiServiceRajaOngkir.class);
        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);
        cartManager = new CartManager(this);
        prefs = getSharedPreferences("rajaongkir_cache", Context.MODE_PRIVATE);

        // Cek apakah ini mode Buy Now
        Product buyNowProduct = (Product) getIntent().getSerializableExtra("buy_now_product");
        int buyNowQty = getIntent().getIntExtra("buy_now_quantity", 1);
        if (buyNowProduct != null) {
            cartItems = new ArrayList<>();
            CartManager.CartItem item = new CartManager.CartItem(buyNowProduct, buyNowQty);
            cartItems.add(item);
        } else {
            cartItems = new ArrayList<>(cartManager.getCartItems().values());
        }

        setupProductList();
        setupDestinationSearch();
        loadOrFetchDestinations();

        subtotal = 0;
        for (CartManager.CartItem item : cartItems) {
            double hargaJual = item.product.getHargajual();
            subtotal += hargaJual * item.quantity;
        }
        updateSubtotal();
        updateTotal();
        setupCheckoutButton();
    }

    private void setupDestinationSearch() {
        // Setup AutoCompleteTextView
        binding.actvDestination.setThreshold(3);
        binding.actvDestination.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>()));

        // Prevent auto-complete behavior
        binding.actvDestination.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                binding.actvDestination.dismissDropDown();
            }
        });

        binding.actvDestination.addTextChangedListener(new TextWatcher() {
            private String lastQuery = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                // Only search if query is different and >= 3 chars
                if (query.length() >= 3 && !query.equals(lastQuery)) {
                    lastQuery = query;
                    searchDestinations(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Handle suggestion selection
        binding.actvDestination.setOnItemClickListener((parent, view, position, id) -> {
            String selectedLabel = parent.getItemAtPosition(position).toString();
            updateSelectedDestination(selectedLabel);
            binding.actvDestination.dismissDropDown();
        });

        // Setup Spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, destinationLabelList);
        binding.spinnerDestination.setAdapter(spinnerAdapter);
        binding.spinnerDestination.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLabel = parent.getItemAtPosition(position).toString();
                updateSelectedDestination(selectedLabel);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void searchDestinations(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        String lowerQuery = query.toLowerCase().trim();

        // Cek apakah sudah ada di cache
        String searchCacheJson = prefs.getString("search_" + lowerQuery, null);
        if (searchCacheJson != null) {
            try {
                Type listType = new TypeToken<List<Destination>>(){}.getType();
                List<Destination> searchResults = gson.fromJson(searchCacheJson, listType);
                if (searchResults != null && !searchResults.isEmpty()) {
                    showSearchResults(searchResults);
                    return;
                }
            } catch (Exception e) {
                Log.e("CheckoutActivity", "Error loading search cache: " + e.getMessage());
            }
        }

        // Filter dari data cache yang ada dulu
        List<String> filteredList = new ArrayList<>();
        List<Destination> localResults = new ArrayList<>();

        for (Destination dest : destinationList) {
            if (dest.label.toLowerCase().contains(lowerQuery)) {
                filteredList.add(dest.label);
                localResults.add(dest);
            }
        }

        // Jika hasil filter local tidak cukup, cari di API
        if (filteredList.size() < 5) {
            rajaOngkirService.searchDestinations("cities", lowerQuery).enqueue(new Callback<DestinationResponse>() {
                @Override
                public void onResponse(Call<DestinationResponse> call, Response<DestinationResponse> response) {
                    if (response.isSuccessful() && response.body() != null &&
                            response.body().meta != null && "success".equals(response.body().meta.status) &&
                            response.body().data != null) {

                        // Simpan hasil pencarian ke cache
                        try {
                            prefs.edit()
                                    .putString("search_" + lowerQuery, gson.toJson(response.body().data))
                                    .apply();
                            showSearchResults(response.body().data);
                        } catch (Exception e) {
                            Log.e("CheckoutActivity", "Error saving search cache: " + e.getMessage());
                        }
                    }
                }

                @Override
                public void onFailure(Call<DestinationResponse> call, Throwable t) {
                    Log.e("CheckoutActivity", "Search API error: " + t.getMessage());
                }
            });
        } else {
            showSearchResults(localResults);
        }
    }

    private void showSearchResults(List<Destination> results) {
        List<String> labels = new ArrayList<>();
        for (Destination dest : results) {
            labels.add(dest.label);
        }

        // Update suggestion list
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, labels);
        binding.actvDestination.setAdapter(adapter);

        if (!labels.isEmpty()) {
            binding.actvDestination.showDropDown();
        }
    }

    private void updateSelectedDestination(String selectedLabel) {
        Log.d("CheckoutActivity", "=== updateSelectedDestination called with: " + selectedLabel);

        // Cari destinasi yang dipilih dari semua sumber
        Destination selectedDest = null;

        // Cari di destinationList
        for (Destination dest : destinationList) {
            if (dest.label.equals(selectedLabel)) {
                selectedDest = dest;
                Log.d("CheckoutActivity", "Found destination in main list: " + dest.id);
                break;
            }
        }

        // Jika tidak ditemukan, cari dari hasil pencarian terakhir
        if (selectedDest == null) {
            Log.d("CheckoutActivity", "Destination not found in main list, searching...");
            // Trigger search untuk mendapatkan detail destinasi
            searchAndSelectDestination(selectedLabel);
            return;
        }

        // Proses destinasi yang ditemukan
        processSelectedDestination(selectedDest, selectedLabel);
    }

    private void searchAndSelectDestination(String selectedLabel) {
        Log.d("CheckoutActivity", "Searching for destination: " + selectedLabel);
        rajaOngkirService.searchDestinations("cities", selectedLabel).enqueue(new Callback<DestinationResponse>() {
            @Override
            public void onResponse(Call<DestinationResponse> call, Response<DestinationResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                        response.body().meta != null && "success".equals(response.body().meta.status) &&
                        response.body().data != null) {

                    for (Destination dest : response.body().data) {
                        if (dest.label.equals(selectedLabel)) {
                            Log.d("CheckoutActivity", "Found destination in search: " + dest.id);
                            processSelectedDestination(dest, selectedLabel);
                            return;
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<DestinationResponse> call, Throwable t) {
                Log.e("CheckoutActivity", "Failed to search destination: " + t.getMessage());
            }
        });
    }

    private void processSelectedDestination(Destination dest, String selectedLabel) {
        Log.d("CheckoutActivity", "=== Processing destination: " + dest.label + " (ID: " + dest.id + ")");

        selectedDestinationId = dest.id;
        binding.etKodepos.setText(dest.zip_code);

        // Update kedua view untuk sinkronisasi
        binding.actvDestination.setText(selectedLabel, false);
        int spinnerPosition = destinationLabelList.indexOf(selectedLabel);
        if (spinnerPosition >= 0) {
            binding.spinnerDestination.setSelection(spinnerPosition);
        }

        // Hitung ongkir dengan JNE
        Log.d("CheckoutActivity", "Calling calculateShippingCost for destination ID: " + dest.id);
        calculateShippingCost(dest.id);
    }

    private void loadOrFetchDestinations() {
        long now = System.currentTimeMillis();
        long lastCache = prefs.getLong(DEST_CACHE_TIME_KEY, 0);
        String cacheJson = prefs.getString(DEST_CACHE_KEY, null);
        
        // Coba load dari cache dulu jika masih valid (24 jam)
        if (cacheJson != null && (now - lastCache) < DEST_CACHE_VALID_MS) {
            try {
                Type listType = new TypeToken<List<Destination>>(){}.getType();
                List<Destination> cached = gson.fromJson(cacheJson, listType);
                
                if (cached != null && !cached.isEmpty()) {
                    destinationList.clear();
                    destinationList.addAll(cached);
                    updateDestinationSpinnerAndSearch("");
                    return;
                }
            } catch (Exception e) {
                Log.e("CheckoutActivity", "Error loading cache: " + e.getMessage());
            }
        }

        // Cache expired atau kosong, fetch dari API dengan keyword "ja" sebagai data awal
        binding.spinnerDestination.setEnabled(false);
        binding.actvDestination.setEnabled(false);

        rajaOngkirService.searchDestinations("cities", "ja").enqueue(new Callback<DestinationResponse>() {
            @Override
            public void onResponse(Call<DestinationResponse> call, Response<DestinationResponse> response) {
                binding.spinnerDestination.setEnabled(true);
                binding.actvDestination.setEnabled(true);

                if (response.isSuccessful() && response.body() != null &&
                    response.body().meta != null && "success".equals(response.body().meta.status) &&
                    response.body().data != null && !response.body().data.isEmpty()) {
                    
                    Log.d("CheckoutActivity", "Received " + response.body().data.size() + " destinations");
                    destinationList.clear();
                    destinationList.addAll(response.body().data);
                    
                    // Simpan ke cache dengan validity 24 jam
                    try {
                        prefs.edit()
                            .putString(DEST_CACHE_KEY, gson.toJson(destinationList))
                            .putLong(DEST_CACHE_TIME_KEY, System.currentTimeMillis())
                            .apply();
                    } catch (Exception e) {
                        Log.e("CheckoutActivity", "Error saving cache: " + e.getMessage());
                    }
                    
                    updateDestinationSpinnerAndSearch("");
                } else {
                    String message = response.body() != null && response.body().meta != null ? 
                                   response.body().meta.message : "Gagal memuat data destinasi";
                    Log.e("CheckoutActivity", "API Error: " + message);
                    Toasty.error(CheckoutActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DestinationResponse> call, Throwable t) {
                binding.spinnerDestination.setEnabled(true);
                binding.actvDestination.setEnabled(true);
                Log.e("CheckoutActivity", "Network error: " + t.getMessage());
                Toasty.error(CheckoutActivity.this, "Gagal memuat data: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDestinationSpinnerAndSearch(String filter) {
        destinationLabelList.clear();
        
        for (Destination dest : destinationList) {
            if (filter == null || filter.isEmpty() || 
                dest.label.toLowerCase().contains(filter.toLowerCase())) {
                destinationLabelList.add(dest.label);
            }
        }

        // Update spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, destinationLabelList);
        binding.spinnerDestination.setAdapter(spinnerAdapter);
        spinnerAdapter.notifyDataSetChanged();

        // Update autocomplete
        ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, destinationLabelList);
        binding.actvDestination.setAdapter(autoCompleteAdapter);
        autoCompleteAdapter.notifyDataSetChanged();
    }

    private void setupProductList() {
        // Gunakan cartItems yang sudah di-set di onCreate
        binding.rvOrderProducts.setLayoutManager(new LinearLayoutManager(this));
        OrderProductAdapter adapter = new OrderProductAdapter(cartItems);
        binding.rvOrderProducts.setAdapter(adapter);
    }

    private void updateSubtotal() {
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

    private void setupCheckoutButton() {
        binding.btnProsesCheckout.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                Toasty.warning(this, "Anda harus login terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (selectedDestinationId == -1) {
                Toasty.warning(this, "Silakan pilih destinasi pengiriman", Toast.LENGTH_SHORT).show();
                return;
            }
            
            OrderCreateRequest request = new OrderCreateRequest();
            request.email = sessionManager.getEmail();
            request.subtotal = subtotal;
            request.ongkir = ongkir;
            request.total_bayar = subtotal + ongkir;
            request.alamat_kirim = binding.etAlamat.getText().toString();
            request.telp_kirim = binding.etTelp.getText().toString();
            request.kota = ""; // Will be filled from selected destination
            request.provinsi = "";
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

    private void calculateShippingCost(int destinationId) {
        Log.d("CheckoutActivity", "=== calculateShippingCost called for destination ID: " + destinationId);

        try {
            JSONObject requestJson = new JSONObject();
            requestJson.put("origin", "391"); // ID kota asal Semarang
            requestJson.put("destination", String.valueOf(destinationId));
            requestJson.put("weight", 1000); // Default 1kg
            requestJson.put("courier", "jne"); // Default kurir JNE

            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestJson.toString()
            );

            Log.d("CheckoutActivity", "Cost Request JSON: " + requestJson.toString());
            Log.d("CheckoutActivity", "Making POST request to cost endpoint...");

            rajaOngkirService.getCost(requestBody).enqueue(new Callback<RajaOngkirCostResponse>() {
                @Override
                public void onResponse(Call<RajaOngkirCostResponse> call, Response<RajaOngkirCostResponse> response) {
                    Log.d("CheckoutActivity", "=== Cost Response received ===");
                    Log.d("CheckoutActivity", "Response code: " + response.code());
                    Log.d("CheckoutActivity", "Response success: " + response.isSuccessful());
                    Log.d("CheckoutActivity", "Response body is null: " + (response.body() == null));

                    if (response.body() != null) {
                        Log.d("CheckoutActivity", "Response body: " + gson.toJson(response.body()));
                    }

                    if (response.isSuccessful() && response.body() != null) {
                        RajaOngkirCostResponse costResponse = response.body();

                        if (costResponse.meta != null && "success".equals(costResponse.meta.status)) {
                            Log.d("CheckoutActivity", "Available services count: " + costResponse.data.size());

                            // Log all available services
                            for (RajaOngkirCostResponse.ShippingCost shipping : costResponse.data) {
                                Log.d("CheckoutActivity", "Service: " + shipping.code + " - " + shipping.service +
                                        " - Cost: " + shipping.cost + " - ETD: " + shipping.etd);
                            }

                            // Find JNE REG service
                            for (RajaOngkirCostResponse.ShippingCost shipping : costResponse.data) {
                                if ("jne".equalsIgnoreCase(shipping.code) &&
                                        "REG".equalsIgnoreCase(shipping.service)) {

                                    ongkir = shipping.cost;
                                    lamaKirim = shipping.etd;

                                    Log.d("CheckoutActivity", "=== FOUND JNE REG SERVICE ===");
                                    Log.d("CheckoutActivity", "Cost: " + ongkir);
                                    Log.d("CheckoutActivity", "ETD: " + lamaKirim);

                                    runOnUiThread(() -> {
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
                                        updateTotal();

                                        Log.d("CheckoutActivity", "UI updated with cost: " + formatter.format(ongkir));
                                    });
                                    break;
                                }
                            }
                        } else {
                            Log.e("CheckoutActivity", "Cost API Error: " +
                                    (costResponse.meta != null ? costResponse.meta.message : "Unknown error"));
                            runOnUiThread(() -> {
                                Toasty.error(CheckoutActivity.this,
                                        "Gagal menghitung ongkir: " + costResponse.meta.message,
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        Log.e("CheckoutActivity", "Cost API Call Failed - Response code: " + response.code());
                        runOnUiThread(() -> {
                            Toasty.error(CheckoutActivity.this,
                                    "Gagal menghitung ongkir",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onFailure(Call<RajaOngkirCostResponse> call, Throwable t) {
                    Log.e("CheckoutActivity", "=== Cost API Call FAILED ===");
                    Log.e("CheckoutActivity", "Error: " + t.getMessage());
                    t.printStackTrace();
                    runOnUiThread(() -> {
                        Toasty.error(CheckoutActivity.this,
                                "Gagal menghitung ongkir: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (Exception e) {
            Log.e("CheckoutActivity", "Error in calculateShippingCost: " + e.getMessage());
            e.printStackTrace();
        }
    }
}