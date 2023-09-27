package com.example.trading.service;

import cloud.metaapi.sdk.clients.meta_api.models.PendingTradeOptions;
import cloud.metaapi.sdk.meta_api.MetaApi;
import cloud.metaapi.sdk.meta_api.MetaApiConnection;
import cloud.metaapi.sdk.meta_api.MetatraderAccount;
import com.example.trading.model.Currency;
import com.example.trading.model.OrderBlock;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Service
public class MetaTraderService {

    /*
    Name: nurdas sadyrbekov
Server: MetaQuotes-Demo
Type: Forex Hedged USD
Login: 74254588
Password: -sCpB1Nr
Investor: 4tSp@fWt
     */
    private WebClient webClient;
    private final String token = "eyJhbGciOiJSUzUxMiIsInR5cCI6IkpXVCJ9.eyJfaWQiOiJlYmU4NDViMGI2MjA1MGQwMmNhODlhMTVmYzE2MjZlZSIsInBlcm1pc3Npb25zIjpbXSwiYWNjZXNzUnVsZXMiOlt7ImlkIjoidHJhZGluZy1hY2NvdW50LW1hbmFnZW1lbnQtYXBpIiwibWV0aG9kcyI6WyJ0cmFkaW5nLWFjY291bnQtbWFuYWdlbWVudC1hcGk6cmVzdDpwdWJsaWM6KjoqIl0sInJvbGVzIjpbInJlYWRlciIsIndyaXRlciJdLCJyZXNvdXJjZXMiOlsiKjokVVNFUl9JRCQ6KiJdfSx7ImlkIjoibWV0YWFwaS1yZXN0LWFwaSIsIm1ldGhvZHMiOlsibWV0YWFwaS1hcGk6cmVzdDpwdWJsaWM6KjoqIl0sInJvbGVzIjpbInJlYWRlciIsIndyaXRlciJdLCJyZXNvdXJjZXMiOlsiKjokVVNFUl9JRCQ6KiJdfSx7ImlkIjoibWV0YWFwaS1ycGMtYXBpIiwibWV0aG9kcyI6WyJtZXRhYXBpLWFwaTp3czpwdWJsaWM6KjoqIl0sInJvbGVzIjpbInJlYWRlciIsIndyaXRlciJdLCJyZXNvdXJjZXMiOlsiKjokVVNFUl9JRCQ6KiJdfSx7ImlkIjoibWV0YWFwaS1yZWFsLXRpbWUtc3RyZWFtaW5nLWFwaSIsIm1ldGhvZHMiOlsibWV0YWFwaS1hcGk6d3M6cHVibGljOio6KiJdLCJyb2xlcyI6WyJyZWFkZXIiLCJ3cml0ZXIiXSwicmVzb3VyY2VzIjpbIio6JFVTRVJfSUQkOioiXX0seyJpZCI6Im1ldGFzdGF0cy1hcGkiLCJtZXRob2RzIjpbIm1ldGFzdGF0cy1hcGk6cmVzdDpwdWJsaWM6KjoqIl0sInJvbGVzIjpbInJlYWRlciJdLCJyZXNvdXJjZXMiOlsiKjokVVNFUl9JRCQ6KiJdfSx7ImlkIjoicmlzay1tYW5hZ2VtZW50LWFwaSIsIm1ldGhvZHMiOlsicmlzay1tYW5hZ2VtZW50LWFwaTpyZXN0OnB1YmxpYzoqOioiXSwicm9sZXMiOlsicmVhZGVyIiwid3JpdGVyIl0sInJlc291cmNlcyI6WyIqOiRVU0VSX0lEJDoqIl19LHsiaWQiOiJjb3B5ZmFjdG9yeS1hcGkiLCJtZXRob2RzIjpbImNvcHlmYWN0b3J5LWFwaTpyZXN0OnB1YmxpYzoqOioiXSwicm9sZXMiOlsicmVhZGVyIiwid3JpdGVyIl0sInJlc291cmNlcyI6WyIqOiRVU0VSX0lEJDoqIl19LHsiaWQiOiJtdC1tYW5hZ2VyLWFwaSIsIm1ldGhvZHMiOlsibXQtbWFuYWdlci1hcGk6cmVzdDpkZWFsaW5nOio6KiIsIm10LW1hbmFnZXItYXBpOnJlc3Q6cHVibGljOio6KiJdLCJyb2xlcyI6WyJyZWFkZXIiLCJ3cml0ZXIiXSwicmVzb3VyY2VzIjpbIio6JFVTRVJfSUQkOioiXX1dLCJ0b2tlbklkIjoiMjAyMTAyMTMiLCJpbXBlcnNvbmF0ZWQiOmZhbHNlLCJyZWFsVXNlcklkIjoiZWJlODQ1YjBiNjIwNTBkMDJjYTg5YTE1ZmMxNjI2ZWUiLCJpYXQiOjE2OTU2MTMzMTIsImV4cCI6MTcwMzM4OTMxMn0.XZJEfLV3glFuuqkLhqkVahP3eZ4biqJ0v7VZDFahFovvY6INYNFFwhvlw26CIzPfglgzOWl-tXgWR2kTwScXSqyHUqIloypYXr4F80rOpsjhkQR12is7-N7iw5KhybYzxAYqkw1E5Z22M32Yw58eP65z5xoBvQaLlMSDROd0RS52taf3ZL9_OPGPuuJs1i8l6f2wBnZB_s9hvbLzpVrxu79S-YuNtRkjkiWzI4w_qH44NVROs6yUnDsxiP1cf5D-JCwBd5bBKn35iCQoyH09KJ0WsiG6zF2rI4EHN2l_g4DhscACiFSN7drVIWPknYKKVE0Iht6iPqYO8PffKCknikSgFV73cmkyed8EnhtTL2KJ4SsUiEfnsWuAlwIMdr-bKkoKuS-qY1TgPdI_8L4aoZEIsW52lxGX4a5NPILMCUMRnI2VDvmIOcVajrzSsBPH6t3u8grye279TIqGbahjHzomYNM_OfiDVp_kJrB3fZfJC2e3cjTMuwi1kGiqPVroUiC1GSjHJk7EwXl-OdCmonWYNb5wp7B8tiETy5gUrvDQ-CLaDyfcFhWiRGIZ_8bp52-_71ph29GWwpxZ7gDmsbvAm33ZtfJp6aa7fCJKyjcXkoCdgn3CuoznAqG_8AdMFM2InyPvfo4PPOXOAXa6kpxcWoDOnS5rDPe7d192YT0";
    private MetaApi api;
    private String accountId = "58ed5578-18c3-4210-ac73-62f41f01a12f";

    public MetaTraderService() {
        try {
            api = new MetaApi(token);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String openTransaction(Currency currency, OrderBlock orderBlock) {
        webClient = WebClient.builder()
                .baseUrl("https://mt-client-api-v1.new-york.agiliumtrade.ai/users/current/accounts/")
                .defaultHeader("auth-token", token)
                .build();

        /*MetatraderAccount account = api.getMetatraderAccountApi().getAccount(accountId).join();
        MetaApiConnection connection = account.connect().join();*/

        /*PendingTradeOptions options = new PendingTradeOptions() {{
            comment = "comment";
            clientId = "TE_GBPUSD_7hyINWqAl";
        }};*/
        orderBlock.setSymbol(currency.name()+"USD");
        orderBlock.setVolume(0.07F);
        if (orderBlock.getOpenPrice() > orderBlock.getStopLoss()) {
            orderBlock.setActionType("ORDER_TYPE_BUY_STOP");
            /*connection.createLimitBuyOrder(currency.name() + "USD", 0.07, Double.valueOf(orderBlock.getPrice()),
                    Double.valueOf(orderBlock.getSl()), Double.valueOf(orderBlock.getTp()), options).join();*/
        } else {
            orderBlock.setActionType("ORDER_TYPE_SELL_STOP");
            /*connection.createLimitSellOrder(currency.name() + "USD", 0.07, Double.valueOf(orderBlock.getPrice()),
                    Double.valueOf(orderBlock.getSl()), Double.valueOf(orderBlock.getTp()), options).join();*/
        }
        String result = webClient.post()
                .uri(accountId +"/trade")
                .body(Mono.just(orderBlock), OrderBlock.class)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return result;
    }
}
