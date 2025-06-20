package com.example.batiknusantara.api.response;
import java.io.Serializable;
import java.util.List;

public class OrderHistoryResponse {
    public boolean status;
    public List<OrderData> data;

    public static class OrderData implements Serializable {
        public int trans_id;
        public String email;
        public String tgl_order;
        public double subtotal;
        public double ongkir;
        public double total_bayar;
        public String alamat_kirim;
        public String telp_kirim;
        public String kota;
        public String provinsi;
        public String lamakirim;
        public String kodepos;
        public int metodebayar;
        public String buktipembayar;
        public int status;
        public String products;
    }
}
