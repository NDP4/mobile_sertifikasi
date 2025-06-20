package com.example.batiknusantara.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.batiknusantara.R;
import com.example.batiknusantara.model.CategoryModel;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private List<CategoryModel> categories;
    private OnCategoryClickListener onCategoryClickListener;

    public CategoryAdapter(List<CategoryModel> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.onCategoryClickListener = listener;
    }

    public interface OnCategoryClickListener {
        void onCategoryClick(String categoryName);
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.onCategoryClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryModel category = categories.get(position);
        String name = category.getName();
        if ("Dress".equalsIgnoreCase(name)) {
            holder.icon.setImageResource(R.drawable.ic_dress);
        } else if ("Blouse".equalsIgnoreCase(name)) {
            holder.icon.setImageResource(R.drawable.ic_blouse);
        } else if ("Kemeja Anak".equalsIgnoreCase(name)) {
            holder.icon.setImageResource(R.drawable.ic_kemeja_anak);
        } else {
            holder.icon.setImageResource(category.getIconResource());
        }
        holder.name.setText(name);
        holder.itemView.setOnClickListener(v -> {
            if (onCategoryClickListener != null) {
                onCategoryClickListener.onCategoryClick(name);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void updateCategories(List<CategoryModel> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;

        ViewHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.ivCategoryIcon);
            name = view.findViewById(R.id.tvCategoryName);
        }
    }
}