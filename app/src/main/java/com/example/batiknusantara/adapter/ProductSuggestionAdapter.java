package com.example.batiknusantara.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.batiknusantara.R;
import com.example.batiknusantara.model.Product;
import java.util.List;

public class ProductSuggestionAdapter extends RecyclerView.Adapter<ProductSuggestionAdapter.ViewHolder> {
    private List<Product> suggestions;
    private OnSuggestionClickListener listener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(Product product);
    }

    public ProductSuggestionAdapter(List<Product> suggestions, OnSuggestionClickListener listener) {
        this.suggestions = suggestions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_suggestion_produk, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = suggestions.get(position);
        holder.tvMerk.setText(product.getMerk());
        Glide.with(holder.itemView.getContext())
                .load(product.getFoto_url())
                .placeholder(R.drawable.ic_product_placeholder)
                .error(R.drawable.ic_product_placeholder)
                .centerCrop()
                .into(holder.imgProduct);
        holder.itemView.setOnClickListener(v -> listener.onSuggestionClick(product));
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    public void updateSuggestions(List<Product> newSuggestions) {
        this.suggestions = newSuggestions;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView tvMerk;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgSuggestionProduk);
            tvMerk = itemView.findViewById(R.id.txtSuggestionMerk);
        }
    }
}
