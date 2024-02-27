package com.klarise.thermalprinter;

import com.klarise.thermalprinter.model.ReceiptModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface APIInterface {

//    @GET("/my/api/ereceipt/")
//    Call<ReceiptModel> doGetListResources(
//            @Query("number") String number
//    );

//    @GET("/my/api/ereceipt/{number}")
//    void doGetListResources(@Path("number") String number, Callback<ReceiptModel> callback);

    @GET("/api/ereceipt/{number}")
    public Call<ReceiptModel> getList(@Path("number") String number);
}
