package com.example.batiknusantara.api.response;

public class BaseResponse<T> {
    public boolean status;
    public T data;
    public String message;
    public Pagination pagination;

    public static class Pagination {
        public int page;
        public int limit;
        public int total_records;
        public int total_pages;
    }
}