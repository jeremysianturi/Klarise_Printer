package com.klarise.thermalprinter.model;

import com.google.gson.annotations.SerializedName;

public class Agent {

    @SerializedName("name")
    public String agentName;

    @SerializedName("address")
    public String agentAddress;

    @SerializedName("phone")
    public String agentPhone;
}
