package com.example.trading.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OrderBlock {
    private Float openPrice;
    private Float stopLoss;
    private Float takeProfit;
    private Float volume;
    private String actionType;
    private String symbol;
    @Override
    public String toString() {
        return "price: " + openPrice + "\nstop loss: " + stopLoss + "\ntake profit: " + takeProfit;
    }
}
