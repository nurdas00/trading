package com.example.trading.service;

import com.example.trading.model.Candle;
import com.example.trading.model.Currency;
import com.example.trading.model.Response;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DataSupplierService {
    private final String UrlSample = "https://market-data.tavex.lv/v1/chart-data/xignite-currencies?symbol=tradingCurrency&currency=USD&from=dateFromZ&to=dateTo&interval=PT3M";
    private final WebClient webClient = WebClient.create();
    public List<Candle> getCandles(Currency currency, LocalDateTime dateTo) {
        LocalDateTime dateFrom = dateTo.minusHours(3);

        String URI = UrlSample;
        URI = URI.replace("dateFrom", dateFrom.toString());
        URI = URI.replace("dateTo", dateTo.toString());
        URI = URI.replace("tradingCurrency", currency.name());

        Response response = webClient.get().uri(URI).retrieve().bodyToMono(Response.class).block();

        assert response != null;
        return response.getData();
    }
}
