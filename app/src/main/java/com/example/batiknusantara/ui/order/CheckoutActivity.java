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

// Activity untuk menangani proses checkout/pemesanan produk
public class CheckoutActivity extends AppCompatActivity {
    // Binding untuk mengakses view di layout activity_checkout.xml
    private ActivityCheckoutBinding binding;
    
    // Service untuk mengakses API RajaOngkir (untuk cek ongkir dan destinasi)
    private ApiServiceRajaOngkir rajaOngkirService;
    
    // Service untuk mengakses API utama aplikasi (untuk create order)
    private ApiService apiService;
    
    // Manager untuk mengelola session user (login/logout)
    private SessionManager sessionManager;
    
    // Manager untuk mengelola keranjang belanja
    private CartManager cartManager;
    
    // List item yang akan di-checkout (dari cart atau buy now)
    private List<CartManager.CartItem> cartItems = new ArrayList<>();
    
    // List semua destinasi yang tersedia (dari API RajaOngkir)
    private List<Destination> destinationList = new ArrayList<>();
    
    // List label destinasi untuk ditampilkan di spinner dan autocomplete
    private List<String> destinationLabelList = new ArrayList<>();
    
    // Biaya ongkos kirim yang dipilih (default 0)
    private double ongkir = 0;
    
    // Total harga produk sebelum ongkir
    private double subtotal = 0;
    
    // Estimasi lama pengiriman (default "-")
    private String lamaKirim = "-";
    
    // ID destinasi yang dipilih user (default -1 = belum dipilih)
    private int selectedDestinationId = -1;
    
    // Data destinasi yang dipilih untuk disimpan ke order
    private Destination selectedDestination = null;

    // Konstanta untuk cache destinasi (agar tidak selalu hit API)
    private static final String DEST_CACHE_KEY = "destination_cache"; // Key untuk menyimpan data destinasi
    private static final String DEST_CACHE_TIME_KEY = "destination_cache_time"; // Key untuk menyimpan waktu cache
    private static final long DEST_CACHE_VALID_MS = 24 * 60 * 60 * 1000; // Cache valid selama 24 jam
    
    // SharedPreferences untuk menyimpan cache
    private SharedPreferences prefs;
    
    // Gson untuk konversi JSON ke object dan sebaliknya
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate layout activity_checkout.xml dan bind ke variable binding
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inisialisasi service untuk API RajaOngkir (ongkir dan destinasi)
        rajaOngkirService = ApiClientRajaOngkir.getClient().create(ApiServiceRajaOngkir.class);
        // Inisialisasi service untuk API utama aplikasi (create order)
        apiService = ApiClient.getClient().create(ApiService.class);
        // Inisialisasi session manager untuk cek status login
        sessionManager = new SessionManager(this);
        // Inisialisasi cart manager untuk mengakses data keranjang
        cartManager = new CartManager(this);
        // Inisialisasi SharedPreferences untuk cache data destinasi
        prefs = getSharedPreferences("rajaongkir_cache", Context.MODE_PRIVATE);

        // Cek apakah ini mode Buy Now (langsung beli) atau dari keranjang
        Product buyNowProduct = (Product) getIntent().getSerializableExtra("buy_now_product");
        int buyNowQty = getIntent().getIntExtra("buy_now_quantity", 1);
        if (buyNowProduct != null) {
            // Mode Buy Now: buat cart item dari produk yang dipilih
            cartItems = new ArrayList<>();
            CartManager.CartItem item = new CartManager.CartItem(buyNowProduct, buyNowQty);
            cartItems.add(item);
        } else {
            // Mode Checkout dari keranjang: ambil semua item dari cart
            cartItems = new ArrayList<>(cartManager.getCartItems().values());
        }

        // Setup UI dan data
        setupProductList(); // Setup RecyclerView untuk menampilkan produk
        setupDestinationSearch(); // Setup AutoCompleteTextView dan Spinner untuk destinasi
        loadOrFetchDestinations(); // Load data destinasi dari cache atau API

        // Hitung subtotal dari semua item
        subtotal = 0;
        for (CartManager.CartItem item : cartItems) {
            double hargaJual = item.product.getHargajual();
            subtotal += hargaJual * item.quantity;
        }
        // Update tampilan subtotal dan total
        updateSubtotal();
        updateTotal();
        // Setup event handler untuk tombol checkout
        setupCheckoutButton();
    }

    /**
     * Method untuk setup AutoCompleteTextView dan Spinner untuk pencarian destinasi
     * Menangani input user untuk mencari kota tujuan pengiriman
     */
    private void setupDestinationSearch() {
        // Setup AutoCompleteTextView untuk pencarian destinasi
        binding.actvDestination.setThreshold(3); // Mulai pencarian setelah 3 karakter
        binding.actvDestination.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>()));

        // Prevent auto-complete behavior - tutup dropdown saat kehilangan focus
        binding.actvDestination.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                binding.actvDestination.dismissDropDown();
            }
        });

        // TextWatcher untuk mendeteksi perubahan text dan melakukan pencarian
        binding.actvDestination.addTextChangedListener(new TextWatcher() {
            private String lastQuery = ""; // Simpan query terakhir untuk mencegah pencarian berulang

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Method ini dipanggil sebelum text berubah (tidak digunakan)
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim(); // Ambil text yang diketik user
                // Hanya lakukan pencarian jika query berbeda dan minimal 3 karakter
                if (query.length() >= 3 && !query.equals(lastQuery)) {
                    lastQuery = query; // Update query terakhir
                    searchDestinations(query); // Panggil method pencarian destinasi
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Method ini dipanggil setelah text berubah (tidak digunakan)
            }
        });

        // Handle suggestion selection - ketika user memilih dari dropdown autocomplete
        binding.actvDestination.setOnItemClickListener((parent, view, position, id) -> {
            String selectedLabel = parent.getItemAtPosition(position).toString(); // Ambil destinasi yang dipilih
            updateSelectedDestination(selectedLabel); // Update destinasi terpilih
            binding.actvDestination.dismissDropDown(); // Tutup dropdown
        });

        // Setup Spinner untuk pilihan destinasi alternatif
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, destinationLabelList);
        binding.spinnerDestination.setAdapter(spinnerAdapter);
        binding.spinnerDestination.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLabel = parent.getItemAtPosition(position).toString(); // Ambil destinasi yang dipilih
                updateSelectedDestination(selectedLabel); // Update destinasi terpilih
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Method ini dipanggil ketika tidak ada yang dipilih (tidak digunakan)
            }
        });
    }

    /**
     * Method untuk mencari destinasi berdasarkan query yang diinput user
     * Menggunakan cache untuk mempercepat pencarian yang sama
     * @param query - kata kunci pencarian kota/destinasi
     */
    private void searchDestinations(String query) {
        // Validasi input - jika query null atau kosong, tidak lakukan apa-apa
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        // Convert query ke lowercase untuk konsistensi pencarian
        String lowerQuery = query.toLowerCase().trim();

        // Cek apakah sudah ada hasil pencarian yang di-cache untuk query ini
        String searchCacheJson = prefs.getString("search_" + lowerQuery, null);
        if (searchCacheJson != null) {
            try {
                // Parse JSON cache menjadi List<Destination>
                Type listType = new TypeToken<List<Destination>>(){}.getType();
                List<Destination> searchResults = gson.fromJson(searchCacheJson, listType);
                // Jika cache valid dan tidak kosong, tampilkan hasil cache
                if (searchResults != null && !searchResults.isEmpty()) {
                    showSearchResults(searchResults);
                    return; // Exit method, tidak perlu hit API
                }
            } catch (Exception e) {
                Log.e("CheckoutActivity", "Error loading search cache: " + e.getMessage());
            }
        }

        // Filter dari data cache yang ada dulu (pencarian lokal)
        List<String> filteredList = new ArrayList<>();
        List<Destination> localResults = new ArrayList<>();

        // Loop semua destinasi yang sudah di-cache sebelumnya
        for (Destination dest : destinationList) {
            // Jika label destinasi mengandung query pencarian
            if (dest.label.toLowerCase().contains(lowerQuery)) {
                filteredList.add(dest.label);
                localResults.add(dest);
            }
        }

        // Jika hasil filter local tidak cukup (<5), cari di API
        if (filteredList.size() < 5) {
            // Hit API RajaOngkir untuk mencari destinasi dengan query tertentu
            rajaOngkirService.searchDestinations("cities", lowerQuery).enqueue(new Callback<DestinationResponse>() {
                @Override
                public void onResponse(Call<DestinationResponse> call, Response<DestinationResponse> response) {
                    // Cek apakah response sukses dan data tidak null
                    if (response.isSuccessful() && response.body() != null &&
                            response.body().meta != null && "success".equals(response.body().meta.status) &&
                            response.body().data != null) {

                        // Simpan hasil pencarian ke cache untuk pencarian berikutnya
                        try {
                            prefs.edit()
                                    .putString("search_" + lowerQuery, gson.toJson(response.body().data))
                                    .apply();
                            // Tampilkan hasil pencarian dari API
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
            // Jika hasil filter local sudah cukup, tampilkan hasil local
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

    /**
     * Method untuk memproses destinasi yang dipilih user
     * Mengupdate UI dan menyimpan data destinasi untuk order
     * @param dest - object destinasi yang dipilih
     * @param selectedLabel - label destinasi yang dipilih
     */
    private void processSelectedDestination(Destination dest, String selectedLabel) {
        Log.d("CheckoutActivity", "=== Processing destination: " + dest.label + " (ID: " + dest.id + ")");

        // Simpan ID destinasi yang dipilih
        selectedDestinationId = dest.id;
        
        // Simpan data destinasi lengkap untuk order
        selectedDestination = dest;
        
        // Update field kode pos dari destinasi yang dipilih
        binding.etKodepos.setText(dest.zip_code);

        // Update kedua view untuk sinkronisasi tampilan
        binding.actvDestination.setText(selectedLabel, false);
        int spinnerPosition = destinationLabelList.indexOf(selectedLabel);
        if (spinnerPosition >= 0) {
            binding.spinnerDestination.setSelection(spinnerPosition);
        }

        // Hitung ongkir dengan JNE untuk destinasi yang dipilih
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
            
            // Buat request order dengan data lengkap
            OrderCreateRequest request = new OrderCreateRequest();
            request.email = sessionManager.getEmail(); // Email user yang login
            request.subtotal = subtotal; // Total harga produk
            request.ongkir = ongkir; // Biaya ongkos kirim
            request.total_bayar = subtotal + ongkir; // Total yang harus dibayar
            request.alamat_kirim = binding.etAlamat.getText().toString(); // Alamat pengiriman dari input user
            request.telp_kirim = binding.etTelp.getText().toString(); // Nomor telepon dari input user
            // Isi data kota dan provinsi dari destinasi yang dipilih
            request.kota = selectedDestination != null ? selectedDestination.city_name : ""; // Nama kota dari destinasi
            request.provinsi = selectedDestination != null ? selectedDestination.province_name : ""; // Nama provinsi dari destinasi
            request.lamakirim = lamaKirim; // Estimasi lama pengiriman
            request.kodepos = binding.etKodepos.getText().toString(); // Kode pos dari destinasi
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

    /**
     * Method utama untuk menghitung biaya ongkos kirim menggunakan API RajaOngkir
     * @param destinationId - ID destinasi/kota tujuan pengiriman
     */
    private void calculateShippingCost(int destinationId) {
        // Log debug untuk tracking pemanggilan method
        Log.d("CheckoutActivity", "=== calculateShippingCost called for destination ID: " + destinationId);

        try {
            // Buat JSON object untuk request body API cost
            JSONObject requestJson = new JSONObject();
            requestJson.put("origin", "391"); // ID kota asal Semarang (sesuai dokumentasi RajaOngkir)
            requestJson.put("destination", String.valueOf(destinationId)); // ID kota tujuan
            requestJson.put("weight", 1000); // Berat default 1kg (1000 gram)
            requestJson.put("courier", "jne"); // Kurir default JNE

            // Convert JSON object menjadi RequestBody untuk dikirim via POST
            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json"), // Content-Type: application/json
                    requestJson.toString() // Body berisi JSON string
            );

            // Log request yang akan dikirim untuk debugging
            Log.d("CheckoutActivity", "Cost Request JSON: " + requestJson.toString());
            Log.d("CheckoutActivity", "Making POST request to cost endpoint...");

            // Kirim request POST ke API RajaOngkir endpoint cost
            rajaOngkirService.getCost(requestBody).enqueue(new Callback<RajaOngkirCostResponse>() {
                @Override
                public void onResponse(Call<RajaOngkirCostResponse> call, Response<RajaOngkirCostResponse> response) {
                    // Log detail response yang diterima
                    Log.d("CheckoutActivity", "=== Cost Response received ===");
                    Log.d("CheckoutActivity", "Response code: " + response.code()); // HTTP status code
                    Log.d("CheckoutActivity", "Response success: " + response.isSuccessful()); // Apakah request berhasil
                    Log.d("CheckoutActivity", "Response body is null: " + (response.body() == null)); // Cek body null

                    // Log full response body dalam format JSON untuk debugging
                    if (response.body() != null) {
                        Log.d("CheckoutActivity", "Response body: " + gson.toJson(response.body()));
                    }

                    // Cek apakah response berhasil dan body tidak null
                    if (response.isSuccessful() && response.body() != null) {
                        RajaOngkirCostResponse costResponse = response.body();

                        // Cek apakah meta status dari API adalah "success"
                        if (costResponse.meta != null && "success".equals(costResponse.meta.status)) {
                            // Log jumlah layanan pengiriman yang tersedia
                            Log.d("CheckoutActivity", "Available services count: " + costResponse.data.size());

                            // Log semua layanan yang tersedia untuk debugging
                            for (RajaOngkirCostResponse.ShippingCost shipping : costResponse.data) {
                                Log.d("CheckoutActivity", "Service: " + shipping.code + " - " + shipping.service +
                                        " - Cost: " + shipping.cost + " - ETD: " + shipping.etd);
                            }

                            // Cari layanan JNE REG (Regular) dari response
                            for (RajaOngkirCostResponse.ShippingCost shipping : costResponse.data) {
                                // Bandingkan code dan service (case insensitive)
                                if ("jne".equalsIgnoreCase(shipping.code) &&
                                        "REG".equalsIgnoreCase(shipping.service)) {

                                    // Simpan data ongkir dan estimasi waktu kirim
                                    ongkir = shipping.cost; // Biaya ongkir dalam rupiah
                                    lamaKirim = shipping.etd; // Estimasi waktu kirim

                                    // Log data yang berhasil ditemukan
                                    Log.d("CheckoutActivity", "=== FOUND JNE REG SERVICE ===");
                                    Log.d("CheckoutActivity", "Cost: " + ongkir);
                                    Log.d("CheckoutActivity", "ETD: " + lamaKirim);

                                    // Update UI di main thread (karena ini di background thread)
                                    runOnUiThread(() -> {
                                        // Format currency untuk tampilan rupiah
                                        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
                                        formatter.setMinimumFractionDigits(0); // Tanpa desimal
                                        String currencySymbol = formatter.getCurrency().getSymbol();
                                        String formattedSymbol = currencySymbol + " ";
                                        formatter.setCurrency(java.util.Currency.getInstance("IDR"));
                                        DecimalFormatSymbols symbols = ((DecimalFormat) formatter).getDecimalFormatSymbols();
                                        symbols.setCurrencySymbol(formattedSymbol);
                                        ((DecimalFormat) formatter).setDecimalFormatSymbols(symbols);

                                        // Update TextView ongkir dengan format rupiah
                                        binding.tvOngkir.setText(formatter.format(ongkir));
                                        // Update TextView estimasi waktu kirim
                                        binding.tvLamaKirim.setText(lamaKirim);
                                        // Update total harga (subtotal + ongkir)
                                        updateTotal();

                                        // Log konfirmasi update UI
                                        Log.d("CheckoutActivity", "UI updated with cost: " + formatter.format(ongkir));
                                    });
                                    break; // Keluar dari loop karena sudah ketemu JNE REG
                                }
                            }
                        } else {
                            // Error dari API (meta status bukan "success")
                            String errorMsg = costResponse.meta != null ? costResponse.meta.message : "Unknown error";
                            Log.e("CheckoutActivity", "Cost API Error: " + errorMsg);
                            
                            // Tampilkan error ke user via toast
                            runOnUiThread(() -> {
                                Toasty.error(CheckoutActivity.this,
                                        "Gagal menghitung ongkir: " + errorMsg,
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        // Response tidak sukses atau body null
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
                    // Network error atau failure lainnya
                    Log.e("CheckoutActivity", "=== Cost API Call FAILED ===");
                    Log.e("CheckoutActivity", "Error: " + t.getMessage());
                    t.printStackTrace(); // Print stack trace untuk debugging
                    
                    // Tampilkan error ke user
                    runOnUiThread(() -> {
                        Toasty.error(CheckoutActivity.this,
                                "Gagal menghitung ongkir: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (Exception e) {
            // Exception dalam pembuatan JSON atau RequestBody
            Log.e("CheckoutActivity", "Error in calculateShippingCost: " + e.getMessage());
            e.printStackTrace();
        }
    }
}