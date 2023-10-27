package com.example.trading.service;

import com.example.trading.model.Candle;
import com.example.trading.model.Currency;
import com.example.trading.model.OrderBlock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.abs;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradingService {

    private final DataSupplierService dataSupplierService;
    private final TelegramBotService telegramBotService;
    private final MetaTraderService metaTraderService;

    private List<Pair<Candle, Candle>> upImbalance;
    private List<Pair<Candle, Candle>> downImbalance;

    private Pair<Candle, Candle> currentDiapason;
    private boolean isCorrection = false;

    public boolean getTrend(int month, int day, int hour, Currency currency) {
        LocalDateTime date = LocalDateTime.of(2023, month, day, hour, 1);
        List<Candle> candles = dataSupplierService.getCandles(currency, date.minusHours(6));

        return calculateTrend(candles);
    }

    public Map<Currency, OrderBlock> getOrderBlocks(int month, int day, int hour) {
        LocalDateTime date = LocalDateTime.of(2023, month, day, hour, 1);
        return getOrderBlocks(date.minusHours(3));
    }

    public Map<Currency, OrderBlock> getOrderBlocks(LocalDateTime now) {
        Map<Currency, OrderBlock> resultMap = new HashMap<>();
        Currency[] currencies = Currency.values();

        String day = String.valueOf(now.getDayOfMonth());
        String month = String.valueOf(now.getMonthValue());
        String year = String.valueOf(now.getYear());
        String hour = String.valueOf(now.getHour());

        for (Currency currency : currencies) {
            Optional<OrderBlock> optionalOrderBlock = getOrderBlockForCurrency(currency, now.minusHours(3));
            String message;
            if (optionalOrderBlock.isPresent()) {
                OrderBlock orderBlock = optionalOrderBlock.get();
                String transaction;
                if (orderBlock.getOpenPrice() > orderBlock.getStopLoss()) {
                    transaction = "Long";
                } else {
                    transaction = "Short";
                }
                message = transaction + "\n" +
                        currency + "\n" +
                        day + "." + month + "." + year + " " + hour + ":00\n" +
                        orderBlock + "\n";

                resultMap.put(currency, orderBlock);

                metaTraderService.openTransaction(currency, orderBlock);
            } else {
                message = "Нет ордер блоков на " + day + "." + month + "." + year + " " + hour + ":00 для " + currency;
            }
            telegramBotService.sendMessage(message, currency);
        }

        return resultMap;
    }

    private Optional<OrderBlock> getOrderBlockForCurrency(Currency currency, LocalDateTime dateTo) {

        log.info("Start method for " + currency.name());

        downImbalance = new ArrayList<>();
        upImbalance = new ArrayList<>();

        List<Candle> candles = dataSupplierService.getCandles(currency, dateTo);

        currentDiapason = Pair.of(candles.get(0), candles.get(0));

        boolean isUp = calculateTrend(candles);
        Candle obCandle = calculateImbalances(candles, dateTo.minusHours(2).minusMinutes(1), dateTo.minusHours(1), isUp);

        Optional<Candle> obClosingCandle = candles.stream().filter(ob ->
                        ob.getStartDateTime().isAfter(dateTo.minusHours(1).atOffset(ZoneOffset.UTC)))
                .filter(ob -> {
                    if (isUp) {
                        return ob.getLow() <= obCandle.getLow();
                    } else {
                        return ob.getHigh() >= obCandle.getHigh();
                    }
                })
                .findFirst();

        if (obClosingCandle.isPresent()) {
            return Optional.empty();
        }

        if (isCorrection) {
            List<Pair<Candle, Candle>> imbalances;
            imbalances = isUp ? downImbalance : upImbalance;

            Pair<Candle, Candle> obWithImbalance;

            float op, sl;
            if (!imbalances.isEmpty()) {
                Pair<Candle, Candle> imbalance = imbalances.get(0);
                if (isUp) {
                    op = imbalance.getSecond().getLow();
                    sl = obCandle.getLow();
                } else {
                    op = imbalance.getSecond().getHigh();
                    sl = obCandle.getHigh();
                }
                obWithImbalance = Pair.of(obCandle, imbalance.getSecond());
            } else {
                if (isUp) {
                    op = obCandle.getHigh();
                    sl = obCandle.getLow();
                } else {
                    op = obCandle.getLow();
                    sl = obCandle.getHigh();
                }
                obWithImbalance = Pair.of(obCandle, obCandle);
            }

            OrderBlock orderBlock = prepareOrderBlock(op, sl, isUp);

            log.info("OrderBlock first candle: " + obWithImbalance.getFirst() +
                    "second candle: " + obWithImbalance.getSecond());

            if (abs(sl - op) > 0.0005) {
                op = (float) (isUp ? sl + 0.0005 : sl - 0.0005);
                float tp = (float) (isUp ? op + 0.00275 : op - 0.00275);

                orderBlock.setOpenPrice(op);
                orderBlock.setTakeProfit(tp);
            }

            return Optional.of(orderBlock);
        }

        return Optional.empty();
    }

    private OrderBlock prepareOrderBlock(Float price, Float sl, boolean isUp) {
        float tp;
        if (isUp) {
            sl -= (float) 0.0001;
            tp = (float) abs((price - sl) * 5.5);
            tp = price + tp;
        } else {
            sl += (float) 0.0001;
            tp = (float) abs((price - sl) * 5.5);
            tp = price - tp;
        }

        tp = Float.parseFloat(BigDecimal.valueOf(tp).setScale(5, RoundingMode.HALF_UP).toString());
        OrderBlock orderBlock = new OrderBlock();
        orderBlock.setOpenPrice(price);
        orderBlock.setStopLoss(sl);
        orderBlock.setTakeProfit(tp);

        return orderBlock;
    }

    public Candle calculateImbalances(List<Candle> candles, LocalDateTime zoneBegin, LocalDateTime zoneEnd, boolean isUp) {
        candles = candles.stream().filter(candle ->
                        !candle.getStartDateTime().isBefore(zoneBegin.atOffset(ZoneOffset.UTC)) &&
                                candle.getStartDateTime().isBefore(zoneEnd.atOffset(ZoneOffset.UTC)))
                .collect(Collectors.toList());
        Candle max = candles.get(0);
        Candle min = candles.get(0);

        for (int i = 0; i < candles.size(); i++) {
            Candle currentCandle = candles.get(i);
            if (i > 1) {
                Candle beginOfImbalance = candles.get(i - 2);

                if (beginOfImbalance.getHigh() < currentCandle.getLow()) {
                    upImbalance.add(Pair.of(beginOfImbalance, currentCandle));
                }

                if (beginOfImbalance.getLow() > currentCandle.getHigh()) {
                    downImbalance.add(Pair.of(beginOfImbalance, currentCandle));
                }
            }

            if (max.getHigh() <= currentCandle.getHigh()) {
                max = currentCandle;
                upImbalance.clear();
            }

            if (min.getLow() >= currentCandle.getLow()) {
                min = currentCandle;
                downImbalance.clear();
            }
        }

        return isUp ? min : max;
    }

    private boolean calculateTrend(List<Candle> candles) {
        int i = 0, j = 1;

        while ((candles.get(i).getHigh() < candles.get(j).getHigh() &&
                candles.get(i).getLow() > candles.get(j).getLow()) ||
                (candles.get(i).getHigh() > candles.get(j).getHigh() &&
                        candles.get(i).getLow() < candles.get(j).getLow())) {
            i++;
            j++;
        }

        boolean isUp = candles.get(i).getHigh() < candles.get(j).getHigh();
        currentDiapason = Pair.of(candles.get(i), candles.get(j));

        Candle max = candles.get(i);
        Candle min = candles.get(i);

        for (i = j; i < candles.size(); i++) {
            Candle currentCandle = candles.get(i);

            if (hasTrendChanged(isUp, currentCandle)) {
                isCorrection = false;
                max = min = currentCandle;
                isUp = !isUp;
                currentDiapason = Pair.of(currentDiapason.getSecond(), currentCandle);
            } else if (isContinuingTrend(isUp, currentCandle)) {
                if (!isCorrection) {
                    currentDiapason = Pair.of(currentDiapason.getFirst(), currentCandle);
                } else {
                    currentDiapason = Pair.of(isUp ? min : max, currentCandle);
                    isCorrection = false;
                }
            } else if (isCorrectionCandle(isUp, currentCandle)) {
                if (currentCandle.getHigh() >= max.getHigh()) {
                    max = currentCandle;
                }

                if (currentCandle.getLow() <= min.getLow()) {
                    min = currentCandle;
                }

                isCorrection = true;
            }

            if (isCorrection) {
                if (max.getHigh() <= currentCandle.getHigh()) {
                    max = currentCandle;
                }

                if (min.getLow() >= currentCandle.getLow()) {
                    min = currentCandle;
                }
            }
        }

        return isUp;
    }

    private boolean isCorrectionCandle(boolean isUp, Candle currentCandle) {
        return (isUp && abs((currentDiapason.getSecond().getHigh() - currentCandle.getLow()) / (currentDiapason.getFirst().getLow() - currentDiapason.getSecond().getHigh())) > 0.38) ||
                (!isUp && abs((currentDiapason.getSecond().getLow() - currentCandle.getHigh()) / (currentDiapason.getFirst().getHigh() - currentDiapason.getSecond().getLow())) > 0.38);
    }

    private boolean hasTrendChanged(boolean isUp, Candle currentCandle) {
        return (isUp && currentCandle.getLow() < currentDiapason.getFirst().getLow()) ||
                (!isUp && currentCandle.getHigh() > currentDiapason.getFirst().getHigh());
    }

    private boolean isContinuingTrend(boolean isUp, Candle currentCandle) {
        return (isUp && currentCandle.getHigh() >= currentDiapason.getSecond().getHigh()) ||
                (!isUp && currentCandle.getLow() <= currentDiapason.getSecond().getLow());
    }
}
