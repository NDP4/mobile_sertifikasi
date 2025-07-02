package com.example.batiknusantara.api.request;

import java.util.List;

public class OrderCreateRequest {
    public String email;
    public String nama_penerima;
    public double subtotal;
    public double ongkir;
    public double total_bayar;
    public String alamat_kirim;
    public String telp_kirim;
    public String kota;
    public String provinsi;
    public String lamakirim;
    public String kodepos;
    public String metodebayar;
    public List<Item> items;

    public static class Item {
        public String kode_brg;
        public double harga_jual;
        public int qty;
        public double bayar;
    }
}
