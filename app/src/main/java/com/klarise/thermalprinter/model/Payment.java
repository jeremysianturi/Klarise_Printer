package com.klarise.thermalprinter.model;

import com.google.gson.annotations.SerializedName;

public class Payment {

    @SerializedName("payment_method")
    public String paymentMethod;

    @SerializedName("amount")
    public double amount;

}
