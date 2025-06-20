package com.example.batiknusantara.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.batiknusantara.R;
import com.example.batiknusantara.databinding.ItemProductBestSellerBinding;
import com.example.batiknusantara.model.Product;
import com.example.batiknusantara.ui.product.ProductDetailActivity;
import com.example.batiknusantara.utils.CartManager;

import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class BestSellerAdapter extends RecyclerView.Adapter<BestSellerAdapter.ViewHolder> {
    private List<Product> products;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onCartClick(Product product);
        void onDetailClick(Product product);
    }

    public BestSellerAdapter(List<Product> products, OnItemClickListener listener) {
        this.products = products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductBestSellerBinding binding = ItemProductBestSellerBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return Math.min(products.size(), 4); // Only show max 4 items
    }

    public void updateProducts(List<Product> newProducts) {
        this.products = newProducts;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ItemProductBestSellerBinding binding;

        ViewHolder(ItemProductBestSellerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Product product) {
            binding.tvProductName.setText(product.getMerk());
            binding.tvCategory.setText(product.getKategori());

            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            formatter.setMinimumFractionDigits(0);
            String currencySymbol = formatter.getCurrency().getSymbol();
            String formattedSymbol = currencySymbol + " ";
            formatter.setCurrency(java.util.Currency.getInstance("IDR")); // Reset to IDR
            DecimalFormatSymbols symbols = ((DecimalFormat) formatter).getDecimalFormatSymbols();
            symbols.setCurrencySymbol(formattedSymbol);
            ((DecimalFormat) formatter).setDecimalFormatSymbols(symbols);
            double originalPrice = product.getHargajual();
            double discountPercent = product.getDiskonjual();

            if (discountPercent > 0) {
                // Calculate discounted price
                double discountAmount = originalPrice * (discountPercent / 100.0);
                double finalPrice = originalPrice - discountAmount;

                // Show original price with strikethrough and smaller text size
                binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvOriginalPrice.setText(formatter.format(originalPrice));
                binding.tvOriginalPrice.setTextSize(10f); // Set smaller text size (12sp)
                binding.tvOriginalPrice.setVisibility(View.VISIBLE);

                // Show discounted price with normal text size
                binding.tvDiscountedPrice.setText(formatter.format(finalPrice));
                binding.tvDiscountedPrice.setTextSize(14f); // Set normal text size (14sp)

                // Show discount chip
                binding.chipDiscount.setText(String.format("%d%%", (int)discountPercent));
                binding.chipDiscount.setVisibility(View.VISIBLE);
            } else {
                // Show only original price without strikethrough
                binding.tvDiscountedPrice.setText(formatter.format(originalPrice));
                binding.tvDiscountedPrice.setTextSize(14f); // Set normal text size
                binding.chipDiscount.setVisibility(View.GONE);
                binding.tvOriginalPrice.setVisibility(View.GONE);
            }
            
            binding.tvStock.setText("Stok: " + product.getStok());

            // Load image with error handling
            Glide.with(itemView.getContext())
                .load(product.getFoto_url())
                .placeholder(R.drawable.ic_product_placeholder)
                .error(R.drawable.ic_product_placeholder)
                .centerCrop()
                .into(binding.imgProduct);

            binding.btnDetail.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), ProductDetailActivity.class);
                intent.putExtra("product_id", product.getKode());
                itemView.getContext().startActivity(intent);
            });
            binding.btnCart.setOnClickListener(v -> {
                CartManager cartManager = new CartManager(itemView.getContext());
                cartManager.addToCart(product);
                Toast.makeText(itemView.getContext(), "Added to cart", Toast.LENGTH_SHORT).show();
            });
        }
    }
}