package com.example.batiknusantara.api.response;

public class ProfileResponse {
    public boolean status;
    public Data data;
    public String message;

    public static class Data {
        public int id;
        public String nama;
        public String alamat;
        public String kota;
        public String provinsi;
        public String kodepos;
        public String telp;
        public String email;
        public String foto;
    }
}