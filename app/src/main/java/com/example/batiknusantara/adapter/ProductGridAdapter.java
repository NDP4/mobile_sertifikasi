package com.example.batiknusantara.adapter;

import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.batiknusantara.R;
import com.example.batiknusantara.databinding.ItemProductGridBinding;
import com.example.batiknusantara.model.Product;
import com.example.batiknusantara.ui.product.ProductDetailActivity;
import com.example.batiknusantara.utils.CartManager;

import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class ProductGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_PRODUCT = 0;
    private static final int VIEW_TYPE_WATERMARK = 1;
    private List<Product> products;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onCartClick(Product product);
    }

    public ProductGridAdapter(List<Product> products, OnItemClickListener listener) {
        this.products = products;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (products.get(position) == null) {
            return VIEW_TYPE_WATERMARK;
        }
        return VIEW_TYPE_PRODUCT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_WATERMARK) {
            // Watermark view
            TextView tv = new TextView(parent.getContext());
            tv.setText("By Nur Dwi Priyambodo\nCoreX");
            tv.setTextSize(10f);
            tv.setTextColor(parent.getContext().getResources().getColor(R.color.dark_gray));
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(0, 32, 0, 32);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 24, 0, 24);
            tv.setLayoutParams(params);
            return new WatermarkViewHolder(tv);
        } else {
            ItemProductGridBinding binding = ItemProductGridBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
            return new ProductViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_WATERMARK) {
            // Tidak perlu bind apa-apa, sudah diatur di onCreateViewHolder
        } else {
            Product product = products.get(position);
            ((ProductViewHolder) holder).bind(product);
        }
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void updateProducts(List<Product> newProducts) {
        this.products = newProducts;
        notifyDataSetChanged();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private ItemProductGridBinding binding;

        ProductViewHolder(ItemProductGridBinding binding) {
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
            formatter.setCurrency(java.util.Currency.getInstance("IDR"));
            DecimalFormatSymbols symbols = ((DecimalFormat) formatter).getDecimalFormatSymbols();
            symbols.setCurrencySymbol(formattedSymbol);
            ((DecimalFormat) formatter).setDecimalFormatSymbols(symbols);

            double hargaPokok = product.getHargapokok();
            double hargaJual = product.getHargajual();
            if (hargaPokok > hargaJual) {
                binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvOriginalPrice.setText(formatter.format(hargaPokok));
                binding.tvOriginalPrice.setTextSize(10f);
                binding.tvOriginalPrice.setVisibility(View.VISIBLE);
                binding.tvDiscountedPrice.setText(formatter.format(hargaJual));
                binding.chipDiscount.setVisibility(View.VISIBLE);
                int diskon = 0;
                if (hargaPokok > 0) {
                    diskon = (int) Math.round((hargaPokok - hargaJual) / hargaPokok * 100);
                }
                binding.chipDiscount.setText(String.format("%d%%", diskon));
            } else {
                binding.tvDiscountedPrice.setText(formatter.format(hargaJual));
                binding.chipDiscount.setVisibility(View.GONE);
                binding.tvOriginalPrice.setVisibility(View.GONE);
            }
            binding.tvStock.setText("Stok: " + product.getStok());
            binding.tvViewCount.setText("Dilihat: " + product.getView_count() + "x");
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

    class WatermarkViewHolder extends RecyclerView.ViewHolder {
        WatermarkViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}