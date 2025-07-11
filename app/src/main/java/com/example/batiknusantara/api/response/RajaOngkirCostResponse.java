package com.example.batiknusantara.api.response;

import java.util.List;

public class RajaOngkirCostResponse {
    public Meta meta;
    public List<ShippingCost> data;

    public static class Meta {
        public String message;
        public int code;
        public String status;
    }

    public static class ShippingCost {
        public String name;
        public String code;
        public String service;
        public String description;
        public int cost;
        public String etd;
    }
}