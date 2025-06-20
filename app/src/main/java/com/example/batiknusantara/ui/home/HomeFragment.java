package com.example.batiknusantara.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
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
import com.example.batiknusantara.utils.SessionManager;


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

        SessionManager sessionManager = new SessionManager(requireContext());
        String userName = sessionManager.getName();
        binding.tvWelcome.setText("Selamat datang: " + (userName != null ? userName : "-"));

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
        categoryAdapter = new CategoryAdapter(new ArrayList<>());
        binding.rvCategories.setLayoutManager(
            new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvCategories.setAdapter(categoryAdapter);
    }

    private void setupBestSellers() {
        bestSellerAdapter = new BestSellerAdapter(new ArrayList<>(), new BestSellerAdapter.OnItemClickListener() {
            @Override
            public void onCartClick(Product product) {
                // TODO: Implement cart click
                Toast.makeText(requireContext(), "Added to cart: " + product.getMerk(), Toast.LENGTH_SHORT).show();
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