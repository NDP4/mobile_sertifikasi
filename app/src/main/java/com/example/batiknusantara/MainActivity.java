package com.example.batiknusantara;

import android.content.Intent;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        sessionManager = new SessionManager(this);

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
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}