package com.example.trading.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class OrderBlock {
    private Float price;
    private Float sl;
    private Float tp;
}
