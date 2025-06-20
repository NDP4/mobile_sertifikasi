package com.example.batiknusantara.api.response;

import java.util.List;

public class RajaOngkirCostResponse {
    public boolean status;
    public Data data;

    public static class Data {
        public List<Result> results;
    }

    public static class Result {
        public String code;
        public String name;
        public List<CostItem> costs;
    }

    public static class CostItem {
        public String service;
        public String description;
        public List<CostDetail> cost;
    }

    public static class CostDetail {
        public double value;
        public String etd;
        public String note;
    }
}