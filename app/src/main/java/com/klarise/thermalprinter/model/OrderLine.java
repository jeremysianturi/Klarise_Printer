package com.klarise.thermalprinter.model;

import com.google.gson.annotations.SerializedName;

public class OrderLine {

    @SerializedName("name")
    public String name;

    @SerializedName("price_unit")
    public Double priceUnit;

    @SerializedName("ordered_qty")
    public Double orderedQty;

    @SerializedName("discount")
    public Double discount;

    @SerializedName("ordered_uom")
    public String orderedUom;

    @SerializedName("delivered_qty")
    public String deliveredQty;

    @SerializedName("delivered_uom")
    public String deliveredUom;

}
