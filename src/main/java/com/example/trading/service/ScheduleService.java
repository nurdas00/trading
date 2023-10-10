package com.example.trading.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final TradingService tradingService;

    @Scheduled(cron = "0 1 1,7-19/3,22 * * MON-FRI")
    public void execute() {
        log.info("Started scheduled invoke");
        tradingService.getOrderBlocks(LocalDateTime.now().minusHours(3));
    }
}
