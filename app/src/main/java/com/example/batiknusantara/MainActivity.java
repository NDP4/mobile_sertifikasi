package com.example.batiknusantara;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.batiknusantara.utils.CartManager;
import com.example.batiknusantara.utils.CartUpdateListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.batiknusantara.databinding.ActivityMainBinding;
import com.example.batiknusantara.utils.SessionManager;
import androidx.fragment.app.Fragment;
import com.example.batiknusantara.ui.product.ProductFragment;

public class MainActivity extends AppCompatActivity implements CartUpdateListener {

    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private NavController navController;
    private TextView badgeCartCount; // Add field for badge

    private static CartUpdateListener cartUpdateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        sessionManager = new SessionManager(this);
        badgeCartCount = findViewById(R.id.badgeCartCount); // Initialize badge view
        updateCartBadge(); // Initial badge update

        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_product, R.id.navigation_profile, R.id.navigation_order)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        // Hapus action bar agar fragment tidak ada topbar
        // NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration); // DIHAPUS
        NavigationUI.setupWithNavController(binding.navView, navController);

        // Handle navigation dan filter kategori
        navView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_profile && !sessionManager.isLoggedIn()) {
                Intent intent = new Intent(this, com.example.batiknusantara.ui.auth.LoginActivity.class);
                startActivity(intent);
                return false;
            }

            // Tunggu fragment berubah sebelum mengatur filter
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (destination.getId() == R.id.navigation_product) {
                    Fragment currentFragment = getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment_activity_main)
                        .getChildFragmentManager()
                        .getFragments().get(0);
                    
                    if (currentFragment instanceof ProductFragment && currentFragment.getArguments() != null) {
                        ((ProductFragment) currentFragment).applyFilter(
                            currentFragment.getArguments().getString("filter_kategori")
                        );
                    }
                }
            });

            return NavigationUI.onNavDestinationSelected(item, navController);
        });

        cartUpdateListener = this;
    }

    // Add method to update cart badge
    public void updateCartBadge() {
        if (badgeCartCount != null) {
            CartManager cartManager = new CartManager(this);
            int count = cartManager.getCartItemCount();
            if (count > 0) {
                badgeCartCount.setText(String.valueOf(count));
                badgeCartCount.setVisibility(View.VISIBLE);
            } else {
                badgeCartCount.setVisibility(View.GONE);
            }
        }
    }

    public static void updateCartBadgeStatic() {
        if (cartUpdateListener != null) {
            cartUpdateListener.onCartUpdated();
        }
    }

    @Override
    public void onCartUpdated() {
        updateCartBadge();
    }

    public void setCartUpdateListener(CartUpdateListener listener) {
        cartUpdateListener = listener;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}