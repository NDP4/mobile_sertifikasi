package com.example.batiknusantara.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.batiknusantara.model.Product;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartManager {
    private static final String PREF_NAME = "BatikNusantaraCart";
    private static final String KEY_GUEST_CART = "guestCart";
    private static final String KEY_USER_CART_PREFIX = "userCart_";
    
    private SharedPreferences pref;
    private Gson gson;
    private SessionManager sessionManager;

    public CartManager(Context context) {
        this.pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.sessionManager = new SessionManager(context);
    }

    private String getCartKey() {
        if (sessionManager.isLoggedIn()) {
            return KEY_USER_CART_PREFIX + sessionManager.getUserId();
        }
        return KEY_GUEST_CART;
    }

    public void addToCart(Product product) {
        String cartKey = getCartKey();
        Map<String, CartItem> cartItems = getCartItems();
        
        CartItem item = cartItems.get(product.getKode());
        if (item == null) {
            item = new CartItem(product, 1);
        } else {
            item.quantity++;
        }
        
        cartItems.put(product.getKode(), item);
        saveCartItems(cartItems);
    }

    public void removeFromCart(String productId) {
        Map<String, CartItem> cartItems = getCartItems();
        cartItems.remove(productId);
        saveCartItems(cartItems);
    }

    public void updateQuantity(String productId, int quantity) {
        if (quantity <= 0) {
            removeFromCart(productId);
            return;
        }

        Map<String, CartItem> cartItems = getCartItems();
        CartItem item = cartItems.get(productId);
        if (item != null) {
            item.quantity = quantity;
            cartItems.put(productId, item);
            saveCartItems(cartItems);
        }
    }

    public Map<String, CartItem> getCartItems() {
        String cartJson = pref.getString(getCartKey(), "");
        Type type = new TypeToken<Map<String, CartItem>>(){}.getType();
        Map<String, CartItem> cartItems = gson.fromJson(cartJson, type);
        return cartItems != null ? cartItems : new HashMap<>();
    }

    private void saveCartItems(Map<String, CartItem> cartItems) {
        String cartJson = gson.toJson(cartItems);
        pref.edit().putString(getCartKey(), cartJson).apply();
    }

    public void clearCart() {
        pref.edit().remove(getCartKey()).apply();
    }

    public static class CartItem {
        public Product product;
        public int quantity;

        public CartItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }
    }

    public int getCartItemCount() {
        Map<String, CartItem> cartItems = getCartItems();
        return cartItems.size();
    }

    public double getCartTotal() {
        Map<String, CartItem> cartItems = getCartItems();
        double total = 0;
        for (CartItem item : cartItems.values()) {
            double price = item.product.getHargajual();
            double discount = item.product.getDiskonjual();
            if (discount > 0) {
                price = price - (price * discount / 100);
            }
            total += price * item.quantity;
        }
        return total;
    }
    public int getCartTotalWeight() {
        Map<String, CartItem> cartItems = getCartItems();
        int totalWeight = 0;
        for (CartItem item : cartItems.values()) {
            // Assuming each item has a weight of 1 unit
            // If your Product class has a weight field, you can use: item.product.getWeight() * item.quantity
            totalWeight += item.quantity;
        }
        return totalWeight;
    }
}