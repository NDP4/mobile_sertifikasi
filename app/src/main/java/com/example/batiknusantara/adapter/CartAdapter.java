package com.example.batiknusantara.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.batiknusantara.R;
import com.example.batiknusantara.model.Product;
import com.example.batiknusantara.utils.CartManager;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {
    private List<CartManager.CartItem> cartItems;
    private OnCartChangeListener listener;

    public interface OnCartChangeListener {
        void onQuantityChanged();
    }

    public CartAdapter(List<CartManager.CartItem> cartItems, OnCartChangeListener listener) {
        this.cartItems = cartItems != null ? cartItems : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartManager.CartItem item = cartItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public void updateCart(List<CartManager.CartItem> newCart) {
        this.cartItems = newCart != null ? newCart : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<CartManager.CartItem> getCartItems() {
        return cartItems;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvQuantity, tvSubtotal;
        ImageButton btnPlus, btnMinus, btnDelete;
        View imgProduct;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCartProductName);
            tvPrice = itemView.findViewById(R.id.tvCartProductPrice);
            tvQuantity = itemView.findViewById(R.id.tvCartQuantity);
            tvSubtotal = itemView.findViewById(R.id.tvCartSubtotal);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            imgProduct = itemView.findViewById(R.id.imgCartProduct);
        }

        void bind(CartManager.CartItem item) {
            Product product = item.product;
            tvName.setText(product.getMerk());
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
                // Ada diskon, tampilkan hargaPokok dicoret dan hargaJual
                tvPrice.setText(formatter.format(hargaJual) + "  ");
                tvPrice.setPaintFlags(tvPrice.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                // Tidak ada diskon, tampilkan hanya hargaJual
                tvPrice.setText(formatter.format(hargaJual));
                tvPrice.setPaintFlags(tvPrice.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            }
            tvQuantity.setText(String.valueOf(item.quantity));
            tvSubtotal.setText(formatter.format(hargaJual * item.quantity));
            Glide.with(itemView.getContext())
                .load(product.getFoto_url())
                .placeholder(R.drawable.ic_product_placeholder)
                .error(R.drawable.ic_product_placeholder)
                .centerCrop()
                .into((android.widget.ImageView) imgProduct);
            // Delete functionality
            btnDelete.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    CartManager cartManager = new CartManager(itemView.getContext());
                    cartManager.removeFromCart(product.getKode());
                    cartItems.remove(pos);
                    notifyItemRemoved(pos);
                    if (listener != null) listener.onQuantityChanged();
                }
            });
        }
    }
}