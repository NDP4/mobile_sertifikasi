package com.example.batiknusantara.model;

import java.io.Serializable;

public class Product implements Serializable {
    private String kode;
    private String merk;
    private String kategori;
    private String satuan;
    private double hargabeli;
    private int diskonbeli;
    private double hargapokok;
    private double hargajual;
    private int diskonjual;
    private int stok;
    private String foto;
    private String deskripsi;
    private String foto_url;

    // Getters
    public String getKode() { return kode; }
    public String getMerk() { return merk; }
    public String getKategori() { return kategori; }
    public String getSatuan() { return satuan; }
    public double getHargabeli() { return hargabeli; }
    public int getDiskonbeli() { return diskonbeli; }
    public double getHargapokok() { return hargapokok; }
    public double getHargajual() { return hargajual; }
    public int getDiskonjual() { return diskonjual; }
    public int getStok() { return stok; }
    public String getFoto() { return foto; }
    public String getDeskripsi() { return deskripsi; }
    public String getFoto_url() { return foto_url; }

    // Setters
    public void setKode(String kode) { this.kode = kode; }
    public void setMerk(String merk) { this.merk = merk; }
    public void setKategori(String kategori) { this.kategori = kategori; }
    public void setSatuan(String satuan) { this.satuan = satuan; }
    public void setHargabeli(double hargabeli) { this.hargabeli = hargabeli; }
    public void setDiskonbeli(int diskonbeli) { this.diskonbeli = diskonbeli; }
    public void setHargapokok(double hargapokok) { this.hargapokok = hargapokok; }
    public void setHargajual(double hargajual) { this.hargajual = hargajual; }
    public void setDiskonjual(int diskonjual) { this.diskonjual = diskonjual; }
    public void setStok(int stok) { this.stok = stok; }
    public void setFoto(String foto) { this.foto = foto; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }
    public void setFoto_url(String foto_url) { this.foto_url = foto_url; }
}
