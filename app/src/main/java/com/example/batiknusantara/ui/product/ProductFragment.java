package com.example.batiknusantara.ui.product;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.batiknusantara.adapter.ProductGridAdapter;
import com.example.batiknusantara.adapter.ProductSuggestionAdapter;
import com.example.batiknusantara.api.ApiClient;
import com.example.batiknusantara.api.ApiService;
import com.example.batiknusantara.api.response.ProductListResponse;
import com.example.batiknusantara.databinding.FragmentProductBinding;
import com.example.batiknusantara.model.Product;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductFragment extends Fragment {

    private FragmentProductBinding binding;
    private ProductGridAdapter productAdapter;
    private ProductSuggestionAdapter suggestionAdapter;
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProductBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupRecyclerView();
        setupSuggestionRecyclerView();
        setupSearchView();
        loadProducts();

        return root;
    }

    private void setupRecyclerView() {
        productAdapter = new ProductGridAdapter(new ArrayList<>(), product ->
                Toast.makeText(requireContext(), "Added to cart: " + product.getMerk(), Toast.LENGTH_SHORT).show()
        );

        binding.rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvProducts.setAdapter(productAdapter);
    }

    private void setupSuggestionRecyclerView() {
        suggestionAdapter = new ProductSuggestionAdapter(new ArrayList<>(), product -> {
            binding.searchViewProduk.setQuery(product.getMerk(), false);
            filterProducts(product.getMerk());
            binding.rvSuggestionProduk.setVisibility(View.GONE);
        });
        binding.rvSuggestionProduk.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSuggestionProduk.setAdapter(suggestionAdapter);
    }

    private void setupSearchView() {
        binding.searchViewProduk.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterProducts(query);
                binding.rvSuggestionProduk.setVisibility(View.GONE);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    // Jika query kosong, tampilkan semua produk
                    filterProducts("");
                    binding.rvSuggestionProduk.setVisibility(View.GONE);
                } else {
                    showSuggestions(newText);
                }
                return true;
            }
        });
        binding.searchViewProduk.setOnCloseListener(() -> {
            filterProducts("");
            binding.rvSuggestionProduk.setVisibility(View.GONE);
            return false;
        });
    }

    private void showSuggestions(String query) {
        if (query.isEmpty()) {
            binding.rvSuggestionProduk.setVisibility(View.GONE);
            return;
        }
        List<Product> suggestions = new ArrayList<>();
        for (Product p : allProducts) {
            if (p.getMerk().toLowerCase().contains(query.toLowerCase())) {
                suggestions.add(p);
            }
        }
        suggestionAdapter.updateSuggestions(suggestions);
        binding.rvSuggestionProduk.setVisibility(suggestions.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void filterProducts(String query) {
        filteredProducts.clear();
        if (query.isEmpty()) {
            filteredProducts.addAll(allProducts);
        } else {
            for (Product p : allProducts) {
                if (p.getMerk().toLowerCase().contains(query.toLowerCase())) {
                    filteredProducts.add(p);
                }
            }
        }
        productAdapter.updateProducts(filteredProducts);
    }

    private void loadProducts() {
        binding.progressBar.setVisibility(View.VISIBLE);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.getProducts(1, 50).enqueue(new Callback<ProductListResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProductListResponse> call, @NonNull Response<ProductListResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().status) {
                    allProducts.clear();
                    allProducts.addAll(response.body().data);
                    filterProducts("");
                } else {
                    Toast.makeText(requireContext(), "Failed to load products", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProductListResponse> call, @NonNull Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}