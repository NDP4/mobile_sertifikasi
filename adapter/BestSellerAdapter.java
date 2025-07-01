package com.example.tokoonline.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tokoonline.R;
import com.example.tokoonline.databinding.ItemBestsellerBinding;
import com.example.tokoonline.model.Product;

import java.util.List;

public class BestSellerAdapter extends RecyclerView.Adapter<BestSellerAdapter.ViewHolder> {

    private List<Product> productList;

    public BestSellerAdapter(List<Product> productList) {
        this.productList = productList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBestsellerBinding binding = ItemBestsellerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemBestsellerBinding binding;

        public ViewHolder(ItemBestsellerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Product product) {
            binding.tvProductName.setText(product.getMerk());
            binding.tvCategory.setText(product.getKategori());
            binding.tvPrice.setText("Rp " + product.getHarga());
            binding.tvStock.setText("Stok: " + product.getStok());
            binding.tvViewCount.setText("Dilihat: " + product.getView_count() + "x");
            // ...existing code...
        }
    }
}