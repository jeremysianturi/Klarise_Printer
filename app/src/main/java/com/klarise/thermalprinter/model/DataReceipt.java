package com.klarise.thermalprinter.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DataReceipt {
    @SerializedName("agent_name")
    public String agenName;

    @SerializedName("agent_address")
    public String agentAddress;

    @SerializedName("name")
    public String name;

    @SerializedName("cashier")
    public String cashier;

    @SerializedName("receive_date")
    public String receiveDate;

    @SerializedName("delivery_date")
    public String deliveryDate;

    @SerializedName("customer")
    public Customer customer;

    @SerializedName("order_line")
    public List<OrderLine> orderLine;

    @SerializedName("amount_total")
    public String amountTotal;

    @SerializedName("currency")
    public String currency;

    @SerializedName("payment")
    public List<Payment> payment;


}
