package com.example.batiknusantara.ui.order;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.batiknusantara.adapter.CartAdapter;
import com.example.batiknusantara.databinding.FragmentOrderBinding;
import com.example.batiknusantara.ui.auth.LoginActivity;
import com.example.batiknusantara.utils.CartManager;
import com.example.batiknusantara.utils.SessionManager;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderFragment extends Fragment {

    private FragmentOrderBinding binding;
    private CartAdapter cartAdapter;
    private CartManager cartManager;
    private SessionManager sessionManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOrderBinding.inflate(inflater, container, false);
        cartManager = new CartManager(requireContext());
        sessionManager = new SessionManager(requireContext());
        setupRecyclerView();
        setupCheckoutButton();
        loadCart();
        return binding.getRoot();
    }

    private void setupRecyclerView() {
        cartAdapter = new CartAdapter(new ArrayList<>(), this::onCartChanged);
        binding.rvCart.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCart.setAdapter(cartAdapter);
    }

    private void loadCart() {
        List<CartManager.CartItem> cartList = new ArrayList<>(cartManager.getCartItems().values());
        cartAdapter.updateCart(cartList);
        updateSubtotal();
    }

    private void onCartChanged() {
        // Save updated cart
        for (CartManager.CartItem item : cartAdapter.getCartItems()) {
            cartManager.updateQuantity(item.product.getKode(), item.quantity);
        }
        loadCart();
    }

    private void updateSubtotal() {
        double subtotal = cartManager.getCartTotal();
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        formatter.setMinimumFractionDigits(0);
        String currencySymbol = formatter.getCurrency().getSymbol();
        String formattedSymbol = currencySymbol + " ";
        formatter.setCurrency(java.util.Currency.getInstance("IDR"));
        DecimalFormatSymbols symbols = ((DecimalFormat) formatter).getDecimalFormatSymbols();
        symbols.setCurrencySymbol(formattedSymbol);
        ((DecimalFormat) formatter).setDecimalFormatSymbols(symbols);
        binding.tvSubtotal.setText(formatter.format(subtotal));
    }

    private void setupCheckoutButton() {
        binding.btnCheckout.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                new AlertDialog.Builder(requireContext())
                    .setTitle("Login Required")
                    .setMessage("Anda harus login untuk melanjutkan ke checkout.")
                    .setPositiveButton("Login", (dialog, which) -> {
                        startActivity(new Intent(requireContext(), LoginActivity.class));
                    })
                    .setNegativeButton("Batal", null)
                    .show();
            } else {
                // TODO: Implement checkout logic for logged in user
                Intent intent = new Intent(requireContext(), CheckoutActivity.class);
                startActivity(intent);
                // You can navigate to a new activity or show a success dialog
                new AlertDialog.Builder(requireContext())
                    .setTitle("Checkout")
                    .setMessage("Checkout berhasil!")
                    .setPositiveButton("OK", null)
                    .show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCart();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}