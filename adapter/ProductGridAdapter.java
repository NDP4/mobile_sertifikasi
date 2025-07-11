package com.example.yourapp.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.yourapp.R;
import com.example.yourapp.activities.ProductDetailActivity;
import com.example.yourapp.databinding.ItemProductBinding;
import com.example.yourapp.models.Product;
import com.example.yourapp.utils.CartManager;

import java.util.List;

public class ProductGridAdapter extends RecyclerView.Adapter<ProductGridAdapter.ProductViewHolder> {

    private List<Product> productList;

    public ProductGridAdapter(List<Product> productList) {
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemProductBinding binding = ItemProductBinding.inflate(inflater, parent, false);
        return new ProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {

        private ItemProductBinding binding;

        public ProductViewHolder(ItemProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Product product) {
            binding.tvProductName.setText(product.getMerk());
            binding.tvCategory.setText(product.getKategori());
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
}