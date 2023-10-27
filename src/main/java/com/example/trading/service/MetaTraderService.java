package com.example.trading.service;

import cloud.metaapi.sdk.clients.meta_api.models.MetatraderAccountInformation;
import cloud.metaapi.sdk.meta_api.MetaApi;
import cloud.metaapi.sdk.meta_api.MetaApiConnection;
import cloud.metaapi.sdk.meta_api.MetatraderAccount;
import com.example.trading.entity.User;
import com.example.trading.model.Currency;
import com.example.trading.model.OrderBlock;
import com.example.trading.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static java.lang.Math.abs;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetaTraderService {

    private final UserRepository userRepository;
    //private final String token = "eyJhbGciOiJSUzUxMiIsInR5cCI6IkpXVCJ9.eyJfaWQiOiJlYmU4NDViMGI2MjA1MGQwMmNhODlhMTVmYzE2MjZlZSIsInBlcm1pc3Npb25zIjpbXSwiYWNjZXNzUnVsZXMiOlt7ImlkIjoidHJhZGluZy1hY2NvdW50LW1hbmFnZW1lbnQtYXBpIiwibWV0aG9kcyI6WyJ0cmFkaW5nLWFjY291bnQtbWFuYWdlbWVudC1hcGk6cmVzdDpwdWJsaWM6KjoqIl0sInJvbGVzIjpbInJlYWRlciIsIndyaXRlciJdLCJyZXNvdXJjZXMiOlsiKjokVVNFUl9JRCQ6KiJdfSx7ImlkIjoibWV0YWFwaS1yZXN0LWFwaSIsIm1ldGhvZHMiOlsibWV0YWFwaS1hcGk6cmVzdDpwdWJsaWM6KjoqIl0sInJvbGVzIjpbInJlYWRlciIsIndyaXRlciJdLCJyZXNvdXJjZXMiOlsiKjokVVNFUl9JRCQ6KiJdfSx7ImlkIjoibWV0YWFwaS1ycGMtYXBpIiwibWV0aG9kcyI6WyJtZXRhYXBpLWFwaTp3czpwdWJsaWM6KjoqIl0sInJvbGVzIjpbInJlYWRlciIsIndyaXRlciJdLCJyZXNvdXJjZXMiOlsiKjokVVNFUl9JRCQ6KiJdfSx7ImlkIjoibWV0YWFwaS1yZWFsLXRpbWUtc3RyZWFtaW5nLWFwaSIsIm1ldGhvZHMiOlsibWV0YWFwaS1hcGk6d3M6cHVibGljOio6KiJdLCJyb2xlcyI6WyJyZWFkZXIiLCJ3cml0ZXIiXSwicmVzb3VyY2VzIjpbIio6JFVTRVJfSUQkOioiXX0seyJpZCI6Im1ldGFzdGF0cy1hcGkiLCJtZXRob2RzIjpbIm1ldGFzdGF0cy1hcGk6cmVzdDpwdWJsaWM6KjoqIl0sInJvbGVzIjpbInJlYWRlciJdLCJyZXNvdXJjZXMiOlsiKjokVVNFUl9JRCQ6KiJdfSx7ImlkIjoicmlzay1tYW5hZ2VtZW50LWFwaSIsIm1ldGhvZHMiOlsicmlzay1tYW5hZ2VtZW50LWFwaTpyZXN0OnB1YmxpYzoqOioiXSwicm9sZXMiOlsicmVhZGVyIiwid3JpdGVyIl0sInJlc291cmNlcyI6WyIqOiRVU0VSX0lEJDoqIl19LHsiaWQiOiJjb3B5ZmFjdG9yeS1hcGkiLCJtZXRob2RzIjpbImNvcHlmYWN0b3J5LWFwaTpyZXN0OnB1YmxpYzoqOioiXSwicm9sZXMiOlsicmVhZGVyIiwid3JpdGVyIl0sInJlc291cmNlcyI6WyIqOiRVU0VSX0lEJDoqIl19LHsiaWQiOiJtdC1tYW5hZ2VyLWFwaSIsIm1ldGhvZHMiOlsibXQtbWFuYWdlci1hcGk6cmVzdDpkZWFsaW5nOio6KiIsIm10LW1hbmFnZXItYXBpOnJlc3Q6cHVibGljOio6KiJdLCJyb2xlcyI6WyJyZWFkZXIiLCJ3cml0ZXIiXSwicmVzb3VyY2VzIjpbIio6JFVTRVJfSUQkOioiXX1dLCJ0b2tlbklkIjoiMjAyMTAyMTMiLCJpbXBlcnNvbmF0ZWQiOmZhbHNlLCJyZWFsVXNlcklkIjoiZWJlODQ1YjBiNjIwNTBkMDJjYTg5YTE1ZmMxNjI2ZWUiLCJpYXQiOjE2OTU2MTMzMTIsImV4cCI6MTcwMzM4OTMxMn0.XZJEfLV3glFuuqkLhqkVahP3eZ4biqJ0v7VZDFahFovvY6INYNFFwhvlw26CIzPfglgzOWl-tXgWR2kTwScXSqyHUqIloypYXr4F80rOpsjhkQR12is7-N7iw5KhybYzxAYqkw1E5Z22M32Yw58eP65z5xoBvQaLlMSDROd0RS52taf3ZL9_OPGPuuJs1i8l6f2wBnZB_s9hvbLzpVrxu79S-YuNtRkjkiWzI4w_qH44NVROs6yUnDsxiP1cf5D-JCwBd5bBKn35iCQoyH09KJ0WsiG6zF2rI4EHN2l_g4DhscACiFSN7drVIWPknYKKVE0Iht6iPqYO8PffKCknikSgFV73cmkyed8EnhtTL2KJ4SsUiEfnsWuAlwIMdr-bKkoKuS-qY1TgPdI_8L4aoZEIsW52lxGX4a5NPILMCUMRnI2VDvmIOcVajrzSsBPH6t3u8grye279TIqGbahjHzomYNM_OfiDVp_kJrB3fZfJC2e3cjTMuwi1kGiqPVroUiC1GSjHJk7EwXl-OdCmonWYNb5wp7B8tiETy5gUrvDQ-CLaDyfcFhWiRGIZ_8bp52-_71ph29GWwpxZ7gDmsbvAm33ZtfJp6aa7fCJKyjcXkoCdgn3CuoznAqG_8AdMFM2InyPvfo4PPOXOAXa6kpxcWoDOnS5rDPe7d192YT0";
    //private String accountId = "58ed5578-18c3-4210-ac73-62f41f01a12f";

    public void openTransaction(Currency currency, OrderBlock orderBlock) {
        List<User> users = userRepository.findAll();

        for (User user : users) {
            if(user.getMetaToken()==null || user.getMetaToken().isEmpty()) {
                continue;
            }

            MetaApi api;
            try {
                api = new MetaApi(user.getMetaToken());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            MetatraderAccount account = api.getMetatraderAccountApi().getAccount(user.getMetaAccountId()).join();
            MetaApiConnection connection = account.connect().join();
            MetatraderAccountInformation accountInfo = connection.getAccountInformation().join();

            /*PendingTradeOptions options = new PendingTradeOptions() {{
            comment = "comment";
            clientId = "TE_GBPUSD_7hyINWqAl";
            }};*/

            orderBlock.setSymbol(currency.name() + "USD");
            BigDecimal volume = BigDecimal.valueOf((float) (accountInfo.balance / abs(orderBlock.getOpenPrice() - orderBlock.getStopLoss()) / 10000000 / 2))
                    .setScale(2, RoundingMode.HALF_UP);

            orderBlock.setVolume(volume.floatValue());
            if (orderBlock.getOpenPrice() > orderBlock.getStopLoss()) {
                orderBlock.setActionType("ORDER_TYPE_BUY_LIMIT");
            /*connection.createLimitBuyOrder(currency.name() + "USD", 0.07, Double.valueOf(orderBlock.getPrice()),
                    Double.valueOf(orderBlock.getSl()), Double.valueOf(orderBlock.getTp()), options).join();*/
            } else {
                orderBlock.setActionType("ORDER_TYPE_SELL_LIMIT");
            /*connection.createLimitSellOrder(currency.name() + "USD", 0.07, Double.valueOf(orderBlock.getPrice()),
                    Double.valueOf(orderBlock.getSl()), Double.valueOf(orderBlock.getTp()), options).join();*/
            }
            WebClient webClient = WebClient.builder()
                    .baseUrl("https://mt-client-api-v1.new-york.agiliumtrade.ai/users/current/accounts/")
                    .defaultHeader("auth-token", user.getMetaToken())
                    .build();

            log.info("Opening transaction : " + orderBlock
                    + "\nvolume: " + orderBlock.getVolume()
                    + "\nactionType: " + orderBlock.getActionType()
                    + "\nsymbol: " + orderBlock.getSymbol());

            String result = webClient.post()
                    .uri(user.getMetaAccountId() + "/trade")
                    .body(Mono.just(orderBlock), OrderBlock.class)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            assert result != null;
            if (result.contains("TRADE_RETCODE_INVALID_PRICE")) {
                log.info("Invalid action type, trying STOP transaction");
                orderBlock.setActionType(orderBlock.getActionType().replace("LIMIT", "STOP"));
                result = webClient.post()
                        .uri(user.getMetaAccountId() + "/trade")
                        .body(Mono.just(orderBlock), OrderBlock.class)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            }

            log.info("Response from MT: " + result);
        }
    }
}
