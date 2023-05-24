package com.example.trading.controller;

import com.example.trading.model.Candle;
import com.example.trading.service.TradingService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@AllArgsConstructor
public class TradingController {

    private final TradingService tradingService;

    @GetMapping("/get-trading")
    public List<Candle> getTrading() throws IOException {
        return tradingService.getPage();
    }
}
