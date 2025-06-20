package com.example.batiknusantara.api.response;

public class LoginResponse {
    public boolean status;
    public String message;
    public UserData data;

    public static class UserData {
        public int id;
        public String nama;
        public String email;
    }
}