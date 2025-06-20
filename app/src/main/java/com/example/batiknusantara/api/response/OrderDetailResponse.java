package com.example.batiknusantara.api.response;

import com.example.batiknusantara.model.Product;
import java.io.Serializable;
import java.util.List;

public class OrderDetailResponse implements Serializable {
    public boolean status;
    public List<Item> data;

    public static class Item implements Serializable {
        public int trans_id;
        public String kode_brg;
        public double harga_jual;
        public int qty;
        public double bayar;
        public Product product;
    }
}