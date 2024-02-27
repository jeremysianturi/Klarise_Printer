package com.klarise.thermalprinter.model;

import com.google.gson.annotations.SerializedName;

public class Customer {

    @SerializedName("name")
    public String name;

    @SerializedName("address")
    public String address;

    @SerializedName("phone")
    public String phone;
}
