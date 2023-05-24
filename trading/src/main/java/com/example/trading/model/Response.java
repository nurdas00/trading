package com.example.trading.model;

import lombok.Data;

import java.util.List;

@Data
public class Response {
    private String statusCode;
    private List<Candle> data;
}
