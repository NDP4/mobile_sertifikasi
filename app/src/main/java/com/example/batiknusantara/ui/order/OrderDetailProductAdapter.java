package com.example.batiknusantara.ui.order;

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
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class OrderDetailProductAdapter extends RecyclerView.Adapter<OrderDetailProductAdapter.ViewHolder> {
    private List<Product> products;
    private List<Integer> qtyList;
    private List<Double> bayarList;

    public OrderDetailProductAdapter(List<Product> products, List<Integer> qtyList, List<Double> bayarList) {
        this.products = products;
        this.qtyList = qtyList;
        this.bayarList = bayarList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        int qty = qtyList.get(position);
        double bayar = bayarList != null && bayarList.size() > position ? bayarList.get(position) : 0;
        String merk = null;
        String foto = null;
        if (product != null) {
            merk = product.getMerk();
            foto = product.getFoto_url();
        }
        holder.bind(product, qty, merk, foto, bayar);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvQuantity, tvSubtotal;
        ImageView imgProduct;
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCartProductName);
            tvPrice = itemView.findViewById(R.id.tvCartProductPrice);
            tvQuantity = itemView.findViewById(R.id.tvCartQuantity);
            tvSubtotal = itemView.findViewById(R.id.tvCartSubtotal);
            imgProduct = itemView.findViewById(R.id.imgCartProduct);
            View btnDelete = itemView.findViewById(R.id.btnDelete);
            if (btnDelete != null) btnDelete.setVisibility(View.GONE);
        }
        void bind(Product product, int qty, String merk, String foto, double hargaBayar) {
            tvName.setText(merk != null ? merk : (product != null ? product.getMerk() : "-"));
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            formatter.setMinimumFractionDigits(0);
            String currencySymbol = formatter.getCurrency().getSymbol();
            String formattedSymbol = currencySymbol + " ";
            formatter.setCurrency(java.util.Currency.getInstance("IDR"));
            DecimalFormatSymbols symbols = ((DecimalFormat) formatter).getDecimalFormatSymbols();
            symbols.setCurrencySymbol(formattedSymbol);
            ((DecimalFormat) formatter).setDecimalFormatSymbols(symbols);
            double hargaPokok = 0;
            double hargaJual = 0;
            if (product != null) {
                hargaPokok = product.getHargapokok();
                hargaJual = product.getHargajual();
            } else {
                hargaJual = hargaBayar / (qty > 0 ? qty : 1);
                hargaPokok = hargaJual;
            }
            if (hargaPokok > hargaJual) {
                // Ada diskon, tampilkan hargaPokok dicoret dan hargaJual
                String hargaPokokStr = formatter.format(hargaPokok);
                String hargaJualStr = formatter.format(hargaJual);
                tvPrice.setText(hargaJualStr + "  ");
                tvPrice.setPaintFlags(tvPrice.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                // Tampilkan harga pokok dicoret di bawah harga jual
                tvPrice.append("\n");
                tvPrice.append(hargaPokokStr);
                tvPrice.setPaintFlags(tvPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                // Tidak ada diskon, tampilkan hanya hargaJual
                tvPrice.setText(formatter.format(hargaJual));
                tvPrice.setPaintFlags(tvPrice.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            }
            tvQuantity.setText(String.valueOf(qty));
            tvSubtotal.setText(formatter.format(hargaJual * qty));
            String fotoUrl = foto;
            if (fotoUrl != null && !fotoUrl.startsWith("http")) {
                fotoUrl = "https://apisertif.ndp.my.id/uploads/products/" + fotoUrl;
            }
            Glide.with(itemView.getContext())
                .load(fotoUrl)
                .placeholder(R.drawable.ic_product_placeholder)
                .error(R.drawable.ic_product_placeholder)
                .centerCrop()
                .into(imgProduct);
        }
    }
}
