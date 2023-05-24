package com.example.trading.service;

import com.example.trading.model.Candle;
import com.example.trading.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class TradingService {
    private final WebClient webClient = WebClient.create();

    private String URI = "https://market-data.tavex.lv/v1/chart-data/xignite-currencies?symbol=EUR&currency=USD&from=dateFromZ&to=dateTo&interval=PT1M";

    public List<Candle> getPage() throws IOException {

        log.info("Start method");

        LocalDateTime time = LocalDateTime.of(2023, 5, 23, 22, 22);
        time = time.minus(6, ChronoUnit.HOURS);

        URI = URI.replace("dateFrom", time.minus(83, ChronoUnit.MINUTES).toString());
        URI = URI.replace("dateTo", time.minus(23, ChronoUnit.MINUTES).toString());

        Response response = webClient.get().uri(URI).retrieve().bodyToMono(Response.class).block();

        assert response != null;
        return response.getData();
    }

}
