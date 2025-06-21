package com.example.batiknusantara.ui.product;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SearchView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.batiknusantara.R;
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
    private String filterKategori = null;
    private String filterSort = null;
    private Boolean filterDiskon = null;
    private List<String> kategoriList = new ArrayList<>();
    private int selectedKategoriIndex = -1;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProductBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Ambil filter kategori dari argument jika ada
        if (getArguments() != null && getArguments().containsKey("filter_kategori")) {
            filterKategori = getArguments().getString("filter_kategori");
        }

        setupRecyclerView();
        setupSuggestionRecyclerView();
        setupSearchView();
        setupFilterButton();
        loadProducts();

        // Tambahkan tombol reset filter jika filterKategori tidak null
        binding.btnResetFilter.setOnClickListener(v -> {
            if (getArguments() != null && getArguments().containsKey("filter_kategori")) {
                requireActivity().onBackPressed();
            } else {
                filterKategori = null;
                filterSort = null;
                filterDiskon = null;
                filterProducts("");
                binding.btnResetFilter.setVisibility(View.GONE);
            }
        });
        if (filterKategori != null) {
            binding.btnResetFilter.setVisibility(View.VISIBLE);
        } else {
            binding.btnResetFilter.setVisibility(View.GONE);
        }


        return root;
    }

    private void setupRecyclerView() {
        productAdapter = new ProductGridAdapter(new ArrayList<>(), product ->
                Toast.makeText(requireContext(), "Added to cart: " + product.getMerk(), Toast.LENGTH_SHORT).show()
        );

        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (productAdapter.getItemViewType(position) == 1) { // VIEW_TYPE_WATERMARK
                    return 2;
                }
                return 1;
            }
        });
        binding.rvProducts.setLayoutManager(layoutManager);
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

    private void setupFilterButton() {
        binding.btnFilterProduk.setOnClickListener(v -> showFilterDialog());
    }

    private void showFilterDialog() {
        // Ambil kategori unik dari allProducts
        kategoriList.clear();
        kategoriList.add("Semua Kategori");
        for (Product p : allProducts) {
            String kat = p.getKategori();
            if (kat != null && !kat.isEmpty() && !kategoriList.contains(kat)) {
                kategoriList.add(kat);
            }
        }
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filter_produk, null);
        Spinner spinnerKategori = dialogView.findViewById(R.id.spinnerKategori);
        ArrayAdapter<String> kategoriAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, kategoriList);
        kategoriAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerKategori.setAdapter(kategoriAdapter);
        if (filterKategori != null) {
            int idx = kategoriList.indexOf(filterKategori);
            spinnerKategori.setSelection(idx >= 0 ? idx : 0);
        } else {
            spinnerKategori.setSelection(0);
        }
        // RadioGroup untuk sort/diskon
        RadioGroup rgSort = dialogView.findViewById(R.id.rgSort);
        rgSort.clearCheck();
        if (filterSort != null) {
            switch (filterSort) {
                case "terbaru": rgSort.check(R.id.rbTerbaru); break;
                case "harga_terendah": rgSort.check(R.id.rbHargaTerendah); break;
                case "harga_tertinggi": rgSort.check(R.id.rbHargaTertinggi); break;
                case "az": rgSort.check(R.id.rbAZ); break;
            }
        } else if (filterDiskon != null) {
            if (filterDiskon) rgSort.check(R.id.rbDiskon);
            else rgSort.check(R.id.rbTanpaDiskon);
        }
        rgSort.setOnCheckedChangeListener((group, checkedId) -> {
            filterSort = null;
            filterDiskon = null;
            if (checkedId == R.id.rbTerbaru) filterSort = "terbaru";
            else if (checkedId == R.id.rbHargaTerendah) filterSort = "harga_terendah";
            else if (checkedId == R.id.rbHargaTertinggi) filterSort = "harga_tertinggi";
            else if (checkedId == R.id.rbAZ) filterSort = "az";
            else if (checkedId == R.id.rbDiskon) filterDiskon = true;
            else if (checkedId == R.id.rbTanpaDiskon) filterDiskon = false;
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Filter Produk");
        builder.setView(dialogView);
        builder.setPositiveButton("Terapkan", (dialog, which) -> {
            int selectedIdx = spinnerKategori.getSelectedItemPosition();
            if (selectedIdx > 0) {
                filterKategori = kategoriList.get(selectedIdx);
            } else {
                filterKategori = null;
            }
            // Ambil pilihan sort/diskon dari RadioGroup (sudah di-set di listener)
            filterProducts(binding.searchViewProduk.getQuery().toString());
            if (filterKategori != null) {
                binding.btnResetFilter.setVisibility(View.VISIBLE);
            } else {
                binding.btnResetFilter.setVisibility(View.GONE);
            }
        });
        builder.setNegativeButton("Batal", (dialog, which) -> dialog.dismiss());
        builder.setNeutralButton("Reset", (dialog, which) -> {
            filterSort = null;
            filterDiskon = null;
            filterKategori = null;
            rgSort.clearCheck();
            filterProducts(binding.searchViewProduk.getQuery().toString());
            binding.btnResetFilter.setVisibility(View.GONE);
        });
        builder.show();
    }

    private void filterProducts(String query) {
        filteredProducts.clear();
        for (Product p : allProducts) {
            boolean matchKategori = (filterKategori == null) || (p.getKategori() != null && p.getKategori().equalsIgnoreCase(filterKategori));
            boolean matchQuery = query.isEmpty() || (p.getMerk().toLowerCase().contains(query.toLowerCase()));
            boolean matchDiskon = true;
            if (filterDiskon != null) {
                matchDiskon = filterDiskon ? (p.getHargapokok() > p.getHargajual()) : (p.getHargapokok() == p.getHargajual());
            }
            if (matchKategori && matchQuery && matchDiskon) {
                filteredProducts.add(p);
            }
        }
        // Sorting
        if (filterSort != null) {
            switch (filterSort) {
                case "harga_terendah":
                    filteredProducts.sort((a, b) -> Double.compare(a.getHargajual(), b.getHargajual()));
                    break;
                case "harga_tertinggi":
                    filteredProducts.sort((a, b) -> Double.compare(b.getHargajual(), a.getHargajual()));
                    break;
                case "az":
                    filteredProducts.sort((a, b) -> a.getMerk().compareToIgnoreCase(b.getMerk()));
                    break;
                // case "terbaru":
                //     // Implement jika ada field tanggal
                //     break;
            }
        }
        // Tambahkan watermark di akhir
        List<Product> displayList = new ArrayList<>(filteredProducts);
        displayList.add(null); // null sebagai watermark
        productAdapter.updateProducts(displayList);
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