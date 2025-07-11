package com.example.batiknusantara.api;

import com.example.batiknusantara.api.request.LoginRequest;
import com.example.batiknusantara.api.request.RegisterRequest;
import com.example.batiknusantara.api.request.OrderCreateRequest;
import com.example.batiknusantara.api.response.*;
import com.example.batiknusantara.model.City;
import com.example.batiknusantara.model.Province;

import java.util.List;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {
//    @POST("login.php")
//    Call<LoginResponse> login(@Body LoginRequest request);
//
//    @POST("register.php")
//    Call<RegisterResponse> register(@Body RegisterRequest request);
    @POST("auth.php?action=login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("auth.php?action=register")
    Call<RegisterResponse> register(@Body RegisterRequest request);
    
    @GET("profile.php")
    Call<ProfileResponse> getProfile(@Query("id") int id);
    
    @PUT("profile.php")
    Call<ResponseBody> updateProfile(@Query("id") int id, @Body RequestBody body);

    @GET("products.php?action=categories")
    Call<CategoryListResponse> getCategories();

    @GET("products.php")
    Call<ProductListResponse> getProducts(@Query("page") int page, @Query("limit") int limit);

    @GET("products.php")
    Call<ProductDetailResponse> getProductDetail(@Query("kode") String kode);

    @GET("products.php")
    Call<BaseResponse> incrementProductView(@Query("action") String action, @Query("kode") String kode);

//    @GET("orders.php?action=provinces")
//    Call<ResponseBody> getProvinces();
//
//    @GET("orders.php?action=cities")
//    Call<ResponseBody> getCities(@Query("province") String provinceId);
//
//    @GET("orders.php?action=shipping")
//    Call<ResponseBody> getShipping(@Query("city_id") String cityId, @Query("weight") int weight);

//    @GET("helpers/RajaOngkir.php?action=provinces")
//    Call<RajaOngkirResponse<List<Province>>> getProvinces();

//    @GET("helpers/RajaOngkir.php?action=cities")
//    Call<RajaOngkirResponse<List<City>>> getCities();
//
//    @GET("helpers/RajaOngkir.php")
//    Call<RajaOngkirResponse<List<City>>> getCitiesByProvince(
//            @Query("action") String action,
//            @Query("province") String provinceId
//    );

    @POST("orders.php")
    Call<OrderCreateResponse> createOrder(@Body OrderCreateRequest request);

    @GET("orders.php")
    Call<OrderHistoryResponse> getOrderHistory(@Query("email") String email);

    @GET("orders.php")
    Call<OrderDetailResponse> getOrderDetail(@Query("trans_id") int transId, @Query("email") String email);
}
