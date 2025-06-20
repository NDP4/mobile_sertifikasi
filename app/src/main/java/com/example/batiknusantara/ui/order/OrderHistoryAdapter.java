package com.example.batiknusantara.ui.order;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.batiknusantara.R;
import com.example.batiknusantara.api.response.OrderHistoryResponse;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder> {
    public interface OnOrderClickListener {
        void onOrderClick(OrderHistoryResponse.OrderData order);
    }
    private List<OrderHistoryResponse.OrderData> orders;
    private OnOrderClickListener listener;

    public OrderHistoryAdapter(List<OrderHistoryResponse.OrderData> orders, OnOrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(orders.get(position));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvDate, tvTotal, tvStatus, tvProducts;
        ViewHolder(View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvDate = itemView.findViewById(R.id.tvOrderDate);
            tvTotal = itemView.findViewById(R.id.tvOrderTotal);
            tvStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvProducts = itemView.findViewById(R.id.tvOrderProducts);
        }
        void bind(OrderHistoryResponse.OrderData order) {
            tvOrderId.setText("Order #" + order.trans_id);
            tvDate.setText(order.tgl_order);
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            formatter.setMinimumFractionDigits(0);
            tvTotal.setText(formatter.format(order.total_bayar));
            tvProducts.setText(order.products);
            String statusText = "";
            switch (order.status) {
                case 0: statusText = "Menunggu Pembayaran"; break;
                case 1: statusText = "Diproses"; break;
                case 2: statusText = "Dikirim"; break;
                case 3: statusText = "Selesai"; break;
                default: statusText = "-";
            }
            tvStatus.setText(statusText);
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOrderClick(order);
            });
        }
    }
}
