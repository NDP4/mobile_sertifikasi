package com.example.batiknusantara.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.batiknusantara.R;
import com.example.batiknusantara.adapter.BannerAdapter;
import com.example.batiknusantara.adapter.BestSellerAdapter;
import com.example.batiknusantara.adapter.CategoryAdapter;
import com.example.batiknusantara.api.ApiClient;
import com.example.batiknusantara.api.ApiService;
import com.example.batiknusantara.api.response.CategoryListResponse;
import com.example.batiknusantara.api.response.ProductListResponse;
import com.example.batiknusantara.databinding.FragmentHomeBinding;
import com.example.batiknusantara.model.BannerModel;
import com.example.batiknusantara.model.CategoryModel;
import com.example.batiknusantara.model.Product;
import com.example.batiknusantara.ui.product.ProductFragment;
import com.example.batiknusantara.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;


import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private BannerAdapter bannerAdapter;
    private CategoryAdapter categoryAdapter;
    private BestSellerAdapter bestSellerAdapter;
    private final Handler sliderHandler = new Handler();
    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (binding.bannerViewPager.getCurrentItem() < bannerAdapter.getItemCount() - 1) {
                binding.bannerViewPager.setCurrentItem(binding.bannerViewPager.getCurrentItem() + 1);
            } else {
                binding.bannerViewPager.setCurrentItem(0);
            }
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Set nama user pada header kiri
        SessionManager sessionManager = new SessionManager(requireContext());
        String userName = sessionManager.getName();
        TextView tvUserName = root.findViewById(R.id.user_name);
        if (userName != null && !userName.isEmpty()) {
            tvUserName.setText(userName);
        } else {
            tvUserName.setText("-");
        }

        // Badge cart count
        updateCartBadge();
        ImageView btnCart = root.findViewById(R.id.btnCart);
        btnCart.setOnClickListener(v -> {
            // Navigasi ke OrderFragment menggunakan bottom navigation
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.nav_view);
            bottomNav.setSelectedItemId(R.id.navigation_order);
        });

        // Tambahkan CardView welcome secara dinamis
        LinearLayout rootLayout = (LinearLayout) binding.getRoot().findViewById(R.id.layoutHomeRoot);
        // Buat CardView
        androidx.cardview.widget.CardView cardView = new androidx.cardview.widget.CardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(16, 16, 16, 8); // margin bottom diubah dari 16 ke 8 agar tidak terlalu jauh
        cardView.setLayoutParams(cardParams);
        cardView.setCardElevation(4f);
        cardView.setRadius(16f);
        cardView.setUseCompatPadding(true);

        // Buat LinearLayout horizontal
        LinearLayout linear = new LinearLayout(requireContext());
        linear.setOrientation(LinearLayout.HORIZONTAL);
        linear.setPadding(24, 24, 24, 24);
        linear.setGravity(Gravity.CENTER_VERTICAL);

        // Icon
        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(R.drawable.ic_person);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(48, 48);
        iconParams.setMargins(0, 0, 16, 0);
        icon.setLayoutParams(iconParams);
        icon.setColorFilter(getResources().getColor(R.color.primary));

        // Text
//        TextView tv = new TextView(requireContext());
//        tv.setText("Selamat datang: " + (userName != null ? userName : "-"));
//        tv.setTextSize(14f);
//        tv.setTextColor(getResources().getColor(R.color.text_primary));
//        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
//        tv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

//        linear.addView(icon);
//        linear.addView(tv);
//        cardView.addView(linear);
        // Tambahkan CardView di posisi setelah banner (index 1)
//        rootLayout.addView(cardView, 1);

        // watermark
        TextView watermark = new TextView(requireContext());
        watermark.setText("By Nur Dwi Priyambodo\nCoreX");
        watermark.setTextSize(10f);
        watermark.setTextColor(getResources().getColor(R.color.dark_gray));
        watermark.setGravity(Gravity.CENTER);
        watermark.setPadding(0, 32, 0, 16);
        rootLayout.addView(watermark);

        setupBannerSlider();
        setupCategories();
        setupBestSellers();
        loadCategories();
        loadBestSellers();
        return root;
    }

    private void setupBannerSlider() {
        List<BannerModel> banners = new ArrayList<>();
        banners.add(new BannerModel(R.drawable.banner1));
        banners.add(new BannerModel(R.drawable.banner2));
        banners.add(new BannerModel(R.drawable.banner3));

        bannerAdapter = new BannerAdapter(banners);
        binding.bannerViewPager.setAdapter(bannerAdapter);
        binding.bannerViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, 3000);
            }
        });
    }

    private void setupCategories() {
        categoryAdapter = new CategoryAdapter(new ArrayList<>(), categoryName -> {
            // Navigasi ke ProductFragment dengan filter kategori menggunakan Navigation Component
            Bundle bundle = new Bundle();
            bundle.putString("filter_kategori", categoryName);
            // Gunakan bottom navigation NavController
            androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_product, bundle);
        });
        binding.rvCategories.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvCategories.setAdapter(categoryAdapter);
    }

    private void setupBestSellers() {
        bestSellerAdapter = new BestSellerAdapter(new ArrayList<>(), new BestSellerAdapter.OnItemClickListener() {
            @Override
            public void onCartClick(Product product) {
                // Tambahkan ke cart
                com.example.batiknusantara.utils.CartManager cartManager = new com.example.batiknusantara.utils.CartManager(requireContext());
                cartManager.addToCart(product);
                Toast.makeText(requireContext(), "Added to cart: " + product.getMerk(), Toast.LENGTH_SHORT).show();
                // Update badge secara reaktif
                updateCartBadge();
            }

            @Override
            public void onDetailClick(Product product) {
                // TODO: Implement detail click
                Toast.makeText(requireContext(), "View detail: " + product.getMerk(), Toast.LENGTH_SHORT).show();
            }
        });
        binding.rvBestSellers.setAdapter(bestSellerAdapter);
        binding.rvBestSellers.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
    }

    private void loadCategories() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getCategories().enqueue(new Callback<CategoryListResponse>() {
            @Override
            public void onResponse(@NonNull Call<CategoryListResponse> call, @NonNull Response<CategoryListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().status) {
                    List<CategoryModel> categories = new ArrayList<>();
                    for (String categoryName : response.body().data) {
                        int iconResource = getCategoryIcon(categoryName);
                        categories.add(new CategoryModel(categoryName, iconResource));
                    }
                    if (categoryAdapter != null) {
                        categoryAdapter.updateCategories(categories);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<CategoryListResponse> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBestSellers() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getProducts(1, 4).enqueue(new Callback<ProductListResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProductListResponse> call, @NonNull Response<ProductListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().status) {
                    if (bestSellerAdapter != null) {
                        bestSellerAdapter.updateProducts(response.body().data);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProductListResponse> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Failed to load products", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getCategoryIcon(String categoryName) {
        String resourceName = "ic_" + categoryName.toLowerCase()
                .replace(" ", "_")
                .replace("(", "")
                .replace(")", "");

        int resourceId = getResources().getIdentifier(resourceName, "drawable", requireContext().getPackageName());
        return resourceId != 0 ? resourceId : R.drawable.ic_category_default;
    }

    @Override
    public void onResume() {
        super.onResume();
        sliderHandler.postDelayed(sliderRunnable, 3000);
        updateCartBadge();
    }

    private void updateCartBadge() {
        if (binding == null) return;
        TextView badge = binding.getRoot().findViewById(R.id.badgeCartCount);
        com.example.batiknusantara.utils.CartManager cartManager = new com.example.batiknusantara.utils.CartManager(requireContext());
        int count = cartManager.getCartItemCount();
        if (badge != null) {
            if (count > 0) {
                badge.setText(String.valueOf(count));
                badge.setVisibility(View.VISIBLE);
            } else {
                badge.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}