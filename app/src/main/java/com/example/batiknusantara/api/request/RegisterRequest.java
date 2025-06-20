package com.example.batiknusantara.api.request;

public class RegisterRequest {
    public String nama;
    public String email;
    public String password;
    public String alamat = "";
    public String kota = "";
    public String provinsi = "";
    public String kodepos = "";
    public String telp = "";

    public RegisterRequest(String nama, String email, String password) {
        this.nama = nama;
        this.email = email;
        this.password = password;
    }
}
