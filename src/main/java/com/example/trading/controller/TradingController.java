package com.example.trading.controller;

import com.example.trading.service.MetaTraderService;
import com.example.trading.model.Currency;
import com.example.trading.model.OrderBlock;
import com.example.trading.service.TradingService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
public class TradingController {

    private final TradingService tradingService;
    private final MetaTraderService metaConfig;

    @GetMapping("/get-trading/{month}/{day}/{hour}")
    public Map<Currency, OrderBlock> getTrading(@PathVariable int month,
                                                      @PathVariable int day,
                                                      @PathVariable int hour) {
        return tradingService.getOrderBlocks(month, day, hour);
    }

    @GetMapping("/get-trend/{month}/{day}/{hour}/{currency}")
    public String getTrend(@PathVariable int month,
                           @PathVariable int day,
                           @PathVariable int hour,
                           @PathVariable Currency currency) {
        boolean isUp = tradingService.getTrend(month, day, hour, currency);

        return isUp ? "Up" : "Down";
    }
}
