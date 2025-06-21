package com.example.batiknusantara.ui.product;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.batiknusantara.R;
import com.example.batiknusantara.api.ApiClient;
import com.example.batiknusantara.api.ApiService;
import com.example.batiknusantara.api.response.ProductDetailResponse;
import com.example.batiknusantara.databinding.ActivityProductDetailBinding;
import com.example.batiknusantara.model.Product;
import com.example.batiknusantara.utils.CartManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends AppCompatActivity {
    private ActivityProductDetailBinding binding;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getClient().create(ApiService.class);

        String productId = getIntent().getStringExtra("product_id");
        if (productId == null) {
            finish();
            return;
        }

        setupToolbar();
        loadProductDetail(productId);
        setupButtons();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadProductDetail(String productId) {
        apiService.getProductDetail(productId).enqueue(new Callback<ProductDetailResponse>() {
            @Override
            public void onResponse(Call<ProductDetailResponse> call, Response<ProductDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().status) {
                    displayProductDetail(response.body().data);
                } else {
                    Toast.makeText(ProductDetailActivity.this, "Failed to load product details", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ProductDetailResponse> call, Throwable t) {
                Toast.makeText(ProductDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayProductDetail(Product product) {
        binding.tvProductName.setText(product.getMerk());
        binding.tvCategory.setText(product.getKategori());
        binding.tvStock.setText("Stock: " + product.getStok());
        binding.tvDescription.setText(product.getDeskripsi());
        this.currentProduct = product;

        // Format currency with space after Rp
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        formatter.setMinimumFractionDigits(0);
        String currencySymbol = formatter.getCurrency().getSymbol();
        String formattedSymbol = currencySymbol + " ";
        formatter.setCurrency(java.util.Currency.getInstance("IDR")); 
        DecimalFormatSymbols symbols = ((DecimalFormat) formatter).getDecimalFormatSymbols();
        symbols.setCurrencySymbol(formattedSymbol);
        ((DecimalFormat) formatter).setDecimalFormatSymbols(symbols);

        double originalPrice = product.getHargajual();
        double discountPercent = product.getDiskonjual();

        if (discountPercent > 0) {
            double discountAmount = originalPrice * (discountPercent / 100.0);
            double finalPrice = originalPrice - discountAmount;

            binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            binding.tvOriginalPrice.setText(formatter.format(originalPrice));
            binding.tvOriginalPrice.setTextSize(14f);
            binding.tvOriginalPrice.setVisibility(View.VISIBLE);

            binding.tvDiscountedPrice.setText(formatter.format(finalPrice));
            binding.chipDiscount.setText(String.format("%d%%", (int)discountPercent));
            binding.chipDiscount.setVisibility(View.VISIBLE);
        } else {
            binding.tvDiscountedPrice.setText(formatter.format(originalPrice));
            binding.tvOriginalPrice.setVisibility(View.GONE);
            binding.chipDiscount.setVisibility(View.GONE);
        }

        Glide.with(this)
            .load(product.getFoto_url())
            .placeholder(R.drawable.ic_product_placeholder)
            .error(R.drawable.ic_product_placeholder)
            .into(binding.imgProduct);
    }

    private void setupButtons() {
        binding.btnAddToCart.setOnClickListener(v -> {
            CartManager cartManager = new CartManager(this);
            cartManager.addToCart(currentProduct);
            Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
        });

        binding.btnBuyNow.setOnClickListener(v -> {
            // Buy Now: langsung checkout produk ini saja
            Intent intent = new Intent(this, com.example.batiknusantara.ui.order.CheckoutActivity.class);
            intent.putExtra("buy_now_product", currentProduct);
            intent.putExtra("buy_now_quantity", 1);
            startActivity(intent);
        });
    }

    private Product currentProduct; // Add this field


}