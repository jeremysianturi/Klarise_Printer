package com.klarise.thermalprinter.model;

import com.google.gson.annotations.SerializedName;

public class ReceiptModel {
    @SerializedName("success")
    public Boolean success;
    @SerializedName("data")
    public DataReceipt data;
}



