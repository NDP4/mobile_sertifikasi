package com.example.batiknusantara.api;

import com.example.batiknusantara.api.response.DestinationResponse;
import com.example.batiknusantara.api.response.RajaOngkirCostResponse;
import com.example.batiknusantara.api.response.RajaOngkirResponse;
import com.example.batiknusantara.model.City;
import com.example.batiknusantara.model.Province;

import java.util.List;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiServiceRajaOngkir {
    @GET("helpers/RajaOngkir.php?action=provinces")
    Call<RajaOngkirResponse<List<Province>>> getProvinces();

    @GET("helpers/RajaOngkir.php")
    Call<RajaOngkirResponse<List<City>>> getCitiesByProvince(
            @Query("action") String action,
            @Query("province") String provinceId
    );

    @POST("helpers/RajaOngkir.php?action=cost")
    @Headers("Content-Type: application/json")
    Call<RajaOngkirCostResponse> getCost(@Body RequestBody body);

    @GET("helpers/RajaOngkir.php")
    Call<DestinationResponse> searchDestinations(@Query("action") String action, @Query("search") String search);
}