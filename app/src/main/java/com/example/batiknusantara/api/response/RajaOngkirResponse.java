package com.example.batiknusantara.api.response;

public class RajaOngkirResponse<T> {
    private RajaOngkirStatus<T> rajaongkir;

    public RajaOngkirStatus<T> getRajaongkir() {
        return rajaongkir;
    }

    // Helper untuk langsung ambil data
    public T getData() {
        return rajaongkir != null ? rajaongkir.getResults() : null;
    }

    public static class RajaOngkirStatus<T> {
        private Object query;
        private Status status;
        private T results;

        public Object getQuery() { return query; }
        public Status getStatus() { return status; }
        public T getResults() { return results; }
    }

    public static class Status {
        private int code;
        private String description;

        public int getCode() { return code; }
        public String getDescription() { return description; }
    }
}