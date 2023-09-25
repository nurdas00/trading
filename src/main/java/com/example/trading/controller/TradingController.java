package com.example.trading.controller;

import com.example.trading.service.MetaTraderService;
import com.example.trading.model.Currency;
import com.example.trading.model.OrderBlock;
import com.example.trading.service.TradingService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
public class TradingController {

    private final TradingService tradingService;
    private final MetaTraderService metaConfig;

    @GetMapping("/get-trading")
    public Map<Currency, List<OrderBlock>> getTrading() {
        return tradingService.getOrderBlocks();
    }
}
