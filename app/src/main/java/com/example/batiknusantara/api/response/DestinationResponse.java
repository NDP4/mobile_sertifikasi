package com.example.batiknusantara.api.response;

import com.example.batiknusantara.model.Destination;
import java.util.List;

public class DestinationResponse {
    public Meta meta;
    public List<Destination> data;

    public static class Meta {
        public String message;
        public int code;
        public String status;
    }
}