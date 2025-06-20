package com.example.batiknusantara.api.response;

import com.example.batiknusantara.model.Product;
import java.util.List;

public class ProductListResponse {
    public boolean status;
    public List<Product> data;
    public String message;
    public Pagination pagination;

    public static class Pagination {
        public int page;
        public int limit;
        public int total_records;
        public int total_pages;
    }
}