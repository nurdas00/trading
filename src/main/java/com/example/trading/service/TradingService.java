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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static java.lang.Math.abs;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradingService {

    private final DataSupplierService dataSupplierService;
    private final TelegramBotService telegramBotService;
    private final MetaTraderService metaTraderService;

    private boolean isUp;
    private Candle hl = new Candle();
    private Candle lh = new Candle();

    private List<Candle> zoneMax;
    private List<Candle> zoneMin;

    private List<Pair<Candle, Candle>> upImbalance;
    private List<Pair<Candle, Candle>> downImbalance;

    private Pair<Candle, Candle> currentDiapason;
    private Candle impulseBegin;
    private boolean isCorrection = false;
    private boolean justBoss = true;
    private final List<OrderBlock> emptyListOfOrderBlocks = new ArrayList<>();

    public Map<Currency, List<OrderBlock>> getOrderBlocks() {
        Map<Currency, List<OrderBlock>> resultMap = new HashMap<>();
        Currency[] currencies = Currency.values();

        LocalDateTime now = LocalDateTime.now();
        now = now.minusHours(3);
        String day = String.valueOf(now.getDayOfMonth());
        String month = String.valueOf(now.getMonthValue());
        String year = String.valueOf(now.getYear());
        String hour = String.valueOf(now.getHour());

        for (Currency currency : currencies) {
            List<OrderBlock> orderBlocks = calculateInitialTrend(currency, now.minusHours(3));
            String message;
            if (!orderBlocks.isEmpty()) {
                String transaction;
                if (orderBlocks.get(0).getOpenPrice() > orderBlocks.get(0).getStopLoss()) {
                    transaction = "Long";
                } else {
                    transaction = "Short";
                }
                message = transaction + "\n" + currency + "\n" +
                        day + "." + month + "." + year + " " + hour + ":00\n" +
                        orderBlockListToString(orderBlocks);

                resultMap.put(currency, orderBlocks);

                metaTraderService.openTransaction(currency, orderBlocks.get(0));
            } else {
                message = "Нет ордер блоков на " + day + "." + month + "." + year + " " + hour + ":00 для " + currency;
            }
            telegramBotService.sendMessage(message, currency);
        }

        return resultMap;
    }

    private List<OrderBlock> calculateInitialTrend(Currency currency, LocalDateTime dateTo) {

        log.info("Start method for " + currency.name());

        zoneMax = new ArrayList<>();
        zoneMin = new ArrayList<>();
        downImbalance = new ArrayList<>();
        upImbalance = new ArrayList<>();

        List<Candle> candles = dataSupplierService.getCandles(currency, dateTo);
        List<Pair<Boolean, Candle>> fractalCandles = getFractalCandles(candles);

        lh = hl = candles.get(0);
        currentDiapason = Pair.of(candles.get(0), candles.get(0));

        List<Pair<Candle, Candle>> orderBlocks = new ArrayList<>();

        calculateImbalances(candles, dateTo.minusHours(2).minusMinutes(1), dateTo.minusHours(1));

        if (isUp && candles.get(candles.size() - 1).getLow() < currentDiapason.getSecond().getLow()) {
            isCorrection = true;
        } else if (!isUp && candles.get(candles.size() - 1).getHigh() > currentDiapason.getSecond().getHigh()) {
            isCorrection = true;
        }

        if (isCorrection) {
            List<Pair<Candle, Candle>> imbalances;
            List<Candle> orderBlockFractalsList;

            orderBlockFractalsList = isUp ? zoneMin : zoneMax;
            imbalances = isUp ? downImbalance : upImbalance;

            List<OrderBlock> orderBlockWithImbalances = new ArrayList<>();

            List<Candle> toDelete = new ArrayList<>();
            for (int i = 0; i < orderBlockFractalsList.size() - 1; i++) {
                for (int j = i + 1; j < orderBlockFractalsList.size(); j++) {
                    if (isOneZoneInsideAnother(orderBlockFractalsList.get(i), orderBlockFractalsList.get(j)) ||
                            areTwoZonesIntercept(orderBlockFractalsList.get(i), orderBlockFractalsList.get(j))) {
                        toDelete.add(orderBlockFractalsList.get(j));
                    }
                }
            }

            orderBlockFractalsList.removeAll(toDelete);
            for (int i = 0; i < orderBlockFractalsList.size(); i++) {
                Candle orderBlockFractal = orderBlockFractalsList.get(i);
                if (orderBlockFractal.getHigh() - orderBlockFractal.getLow() > 0.0055) {
                    if (isUp) {
                        orderBlockFractal.setHigh((float) (orderBlockFractal.getLow() + 0.0055));
                    } else {
                        orderBlockFractal.setLow((float) (orderBlockFractal.getHigh() - 0.0055));
                    }
                }
                OrderBlock orderBlock;
                if (imbalances.size() == 0) {
                    if (isUp) {
                        orderBlock = prepareOrderBlock(orderBlockFractal.getHigh(), orderBlockFractal.getLow());
                        orderBlocks.add(Pair.of(orderBlockFractal, orderBlockFractal));
                        orderBlockWithImbalances.add(orderBlock);
                    } else {
                        orderBlock = prepareOrderBlock(orderBlockFractal.getLow(), orderBlockFractal.getHigh());
                        orderBlocks.add(Pair.of(orderBlockFractal, orderBlockFractal));
                        orderBlockWithImbalances.add(orderBlock);
                    }
                    continue;
                }

                while (!imbalances.isEmpty() && imbalances.get(0).getFirst().getStartDateTime().isBefore(orderBlockFractal.getStartDateTime())) {
                    imbalances.remove(0);
                }


                Pair<Candle, Candle> imbalance;

                if (!imbalances.isEmpty()) {
                    imbalance = imbalances.get(0);
                } else {
                    if (isUp) {
                        orderBlock = prepareOrderBlock(orderBlockFractal.getHigh(), orderBlockFractal.getLow());
                        orderBlocks.add(Pair.of(orderBlockFractal, orderBlockFractal));
                        orderBlockWithImbalances.add(orderBlock);
                    } else {
                        orderBlock = prepareOrderBlock(orderBlockFractal.getLow(), orderBlockFractal.getHigh());
                        orderBlocks.add(Pair.of(orderBlockFractal, orderBlockFractal));
                        orderBlockWithImbalances.add(orderBlock);
                    }
                    continue;
                }

                if (isUp) {
                    if ((i < orderBlockFractalsList.size() - 1 &&
                            imbalances.get(0).getFirst().getStartDateTime().isBefore(orderBlockFractalsList.get(i + 1).getStartDateTime()))
                            || i == orderBlockFractalsList.size() - 1) {
                        if (imbalance.getSecond().getLow() - orderBlockFractal.getHigh() <= orderBlockFractal.getHigh() - orderBlockFractal.getLow()) {
                            orderBlocks.add(Pair.of(imbalance.getSecond(), orderBlockFractal));
                            orderBlock = prepareOrderBlock(imbalance.getSecond().getLow(), orderBlockFractal.getLow());
                            orderBlockWithImbalances.add(orderBlock);
                        } else {
                            orderBlocks.add(Pair.of(orderBlockFractal, orderBlockFractal));
                            orderBlock = prepareOrderBlock(orderBlockFractal.getHigh(), orderBlockFractal.getLow());
                            orderBlockWithImbalances.add(orderBlock);
                        }
                        imbalances.remove(imbalance);
                    } else {
                        orderBlocks.add(Pair.of(orderBlockFractal, orderBlockFractal));
                        orderBlock = prepareOrderBlock(orderBlockFractal.getHigh(), orderBlockFractal.getLow());
                        orderBlockWithImbalances.add(orderBlock);
                    }
                } else {
                    if ((i < orderBlockFractalsList.size() - 1 &&
                            !imbalances.get(0).getFirst().getStartDateTime().isAfter(orderBlockFractalsList.get(i + 1).getStartDateTime())
                            || i == orderBlockFractalsList.size() - 1)) {
                        if (orderBlockFractal.getLow() - imbalance.getSecond().getHigh() <= orderBlockFractal.getHigh() - orderBlockFractal.getLow()) {
                            orderBlocks.add(Pair.of(imbalance.getSecond(), orderBlockFractal));
                            orderBlock = prepareOrderBlock(imbalance.getSecond().getHigh(), orderBlockFractal.getHigh());
                            orderBlockWithImbalances.add(orderBlock);
                        } else {
                            orderBlocks.add(Pair.of(orderBlockFractal, orderBlockFractal));
                            orderBlock = prepareOrderBlock(orderBlockFractal.getLow(), orderBlockFractal.getHigh());
                            orderBlockWithImbalances.add(orderBlock);
                        }
                        imbalances.remove(imbalance);

                    } else {
                        orderBlocks.add(Pair.of(orderBlockFractal, orderBlockFractal));
                        orderBlock = prepareOrderBlock(orderBlockFractal.getLow(), orderBlockFractal.getHigh());
                        orderBlockWithImbalances.add(orderBlock);
                    }
                }
            }

            if(!orderBlocks.isEmpty()) {
                log.info("OrderBlock first candle: " + orderBlocks.get(0).getFirst() +
                        "second candle: " + orderBlocks.get(0).getSecond());
            }
            return orderBlockWithImbalances;
        }

        return emptyListOfOrderBlocks;
    }

    private boolean areTwoZonesIntercept(Candle first, Candle second) {
        if (isUp) {
            return first.getHigh() >= second.getLow();
        } else {
            return first.getLow() <= second.getHigh();
        }
    }

    private boolean isOneZoneInsideAnother(Candle first, Candle second) {
        if (isUp) {
            return first.getHigh() >= second.getHigh();
        } else {
            return first.getLow() <= second.getLow();
        }
    }

    private String orderBlockListToString(List<OrderBlock> orderBlocksList) {
        StringBuilder result = new StringBuilder();
        for (OrderBlock orderBlock : orderBlocksList) {
            result.append(orderBlock.toString());
            result.append("\n");
        }

        return result.toString();
    }

    private OrderBlock prepareOrderBlock(Float price, Float sl) {
        float tp = price + (price - sl) * 6;
        if (isUp) {
            tp -= 0.0001;
        } else {
            tp += 0.0001;
        }

        tp = Float.parseFloat(BigDecimal.valueOf(tp).setScale(5, RoundingMode.HALF_UP).toString());
        OrderBlock orderBlock = new OrderBlock();
        orderBlock.setOpenPrice(price);
        orderBlock.setStopLoss(sl);
        orderBlock.setTakeProfit(tp);

        return orderBlock;
    }

    public void calculateImbalances(List<Candle> candles, LocalDateTime zoneBegin, LocalDateTime zoneEnd) {

        List<Pair<Boolean, Candle>> fractalCandles = getFractalCandles(candles);
        LocalDateTime firstZone = zoneBegin.plusMinutes(24);

        isUp = fractalCandles.get(0).getFirst();
        currentDiapason = Pair.of(candles.get(0), fractalCandles.get(0).getSecond());

        for (Pair<Boolean, Candle> currentFractal : fractalCandles) {
            calculateTrend(currentFractal);

            Candle currentCandle = currentFractal.getSecond();

            if (!zoneMin.isEmpty()) {
                zoneMin.removeIf(minCandle -> currentCandle.getLow() < minCandle.getLow());
            }
            if (!zoneMax.isEmpty()) {
                zoneMax.removeIf(maxCandle -> currentCandle.getHigh() > maxCandle.getHigh());
            }
            OffsetDateTime currentTime = currentCandle.getStartDateTime();
            if (!currentTime.isBefore(zoneBegin.atOffset(ZoneOffset.UTC)) &&
                    !currentTime.isAfter(zoneEnd.atOffset(ZoneOffset.UTC))) {
                if (currentFractal.getFirst()) {
                    if (zoneMax.isEmpty() || !currentCandle.getStartDateTime().isAfter(firstZone.atOffset(ZoneOffset.UTC))) {
                        zoneMax.add(currentCandle);
                    }
                } else {
                    if (zoneMin.isEmpty() || !currentCandle.getStartDateTime().isAfter(firstZone.atOffset(ZoneOffset.UTC))) {
                        zoneMin.add(currentCandle);
                    }
                }
            }
        }

        Candle lastCandle = candles.get(candles.size() - 1);
        zoneMin.removeIf(minCandle -> lastCandle.getLow() < minCandle.getLow());
        zoneMax.removeIf(maxCandle -> lastCandle.getHigh() > maxCandle.getHigh());

        if (!zoneMax.isEmpty()) {
            zoneMax = Collections.singletonList(zoneMax.get(0));
        }

        if (!zoneMin.isEmpty()) {
            zoneMin = Collections.singletonList(zoneMin.get(0));
        }

        for (int i = 2; i < candles.size(); i++) {
            Candle currentCandle = candles.get(i);
            Candle imbalanceCandle = candles.get(i - 2);

            if (!currentCandle.getStartDateTime().isAfter(zoneEnd.atOffset(ZoneOffset.UTC))) {
                if (currentCandle.getLow() > imbalanceCandle.getHigh()) {
                    downImbalance.add(Pair.of(imbalanceCandle, currentCandle));
                }
            }

            downImbalance.removeIf(downImbalanceCandle -> zoneMin.isEmpty() || currentCandle.getLow() <= downImbalanceCandle.getFirst().getHigh() ||
                    downImbalanceCandle.getFirst().getStartDateTime().isBefore(zoneMin.get(0).getStartDateTime()));

            if (!currentCandle.getStartDateTime().isAfter(zoneEnd.atOffset(ZoneOffset.UTC))) {
                if (currentCandle.getHigh() < imbalanceCandle.getLow()) {
                    upImbalance.add(Pair.of(imbalanceCandle, currentCandle));
                }
            }
            upImbalance.removeIf(upImbalanceCandle -> zoneMax.isEmpty() || currentCandle.getHigh() >= upImbalanceCandle.getFirst().getLow() ||
                    upImbalanceCandle.getFirst().getStartDateTime().isBefore(zoneMax.get(0).getStartDateTime()));

        }
    }

    private void calculateTrend(Pair<Boolean, Candle> currentFractal) {
        Candle currentCandle = currentFractal.getSecond();
        boolean currentUp = currentFractal.getFirst();

        if (isUp) {
            if (currentUp) {
                if (currentDiapason.getSecond().getHigh() < currentCandle.getHigh()) {
                    currentDiapason = Pair.of(lh, currentCandle);
                    impulseBegin = currentCandle;
                    isCorrection = false;
                    justBoss = true;
                } else {
                    isCorrection = true;
                    justBoss = false;
                }

            } else {
                if (currentDiapason.getFirst().getLow() > currentCandle.getLow()) {
                    isUp = false;
                    justBoss = true;
                    currentDiapason = Pair.of(currentDiapason.getSecond(), currentCandle);
                    isCorrection = false;
                } else {
                    if (justBoss) {
                        lh = currentCandle;
                    } else if (lh.getLow() > currentCandle.getLow()) {
                        lh = currentCandle;
                    }
                    justBoss = false;
                    isCorrection = true;
                }
            }
        } else {
            if (!currentUp) {
                if (currentDiapason.getSecond().getLow() > currentCandle.getLow()) {
                    currentDiapason = Pair.of(hl, currentCandle);
                    isCorrection = false;
                    impulseBegin = currentCandle;
                    justBoss = true;
                } else {
                    isCorrection = true;
                    justBoss = false;
                }
            } else {
                if (currentDiapason.getFirst().getHigh() < currentCandle.getHigh()) {
                    isUp = true;
                    justBoss = true;
                    currentDiapason = Pair.of(currentDiapason.getSecond(), currentCandle);
                    isCorrection = false;
                } else {
                    if (justBoss) {
                        hl = currentCandle;
                    } else if (hl.getHigh() < currentCandle.getHigh()) {
                        hl = currentCandle;
                    }
                    justBoss = false;
                    isCorrection = true;
                }
            }
        }

    }

    private List<Pair<Boolean, Candle>> getFractalCandles(List<Candle> candles) {
        List<Pair<Boolean, Candle>> fractalCandles = new ArrayList<>();
        for (int i = 0; i < candles.size() - 4; i++) {
            float difference = 0.0000001F;

            if ((candles.get(i + 2).getHigh() > candles.get(i).getHigh() || abs(candles.get(i + 2).getHigh() - candles.get(i).getHigh()) < difference) &&
                    (candles.get(i + 2).getHigh() > candles.get(i + 1).getHigh() || abs(candles.get(i + 2).getHigh() - candles.get(i + 1).getHigh()) < difference) &&
                    (candles.get(i + 2).getHigh() > candles.get(i + 3).getHigh() || abs(candles.get(i + 2).getHigh() - candles.get(i + 3).getHigh()) < difference) &&
                    (candles.get(i + 2).getHigh() > candles.get(i + 4).getHigh() || abs(candles.get(i + 2).getHigh() - candles.get(i + 4).getHigh()) < difference)) {
                if (!fractalCandles.isEmpty()) {
                    int pos = fractalCandles.size() - 1;
                    if ((fractalCandles.get(pos).getFirst()
                            && Objects.equals(fractalCandles.get(pos).getSecond().getHigh(), candles.get(i + 2).getHigh()))) {
                        continue;
                    }
                }
                fractalCandles.add(Pair.of(true, candles.get(i + 2)));
            } else if ((candles.get(i + 2).getLow() < candles.get(i).getLow() || abs(candles.get(i).getLow() - candles.get(i + 2).getLow()) < difference) &&
                    (candles.get(i + 2).getLow() < candles.get(i + 1).getLow() || abs(candles.get(i + 1).getLow() - candles.get(i + 2).getLow()) < difference) &&
                    (candles.get(i + 2).getLow() < candles.get(i + 3).getLow() || abs(candles.get(i + 3).getLow() - candles.get(i + 2).getLow()) < difference) &&
                    (candles.get(i + 2).getLow() < candles.get(i + 4).getLow() || abs(candles.get(i + 4).getLow() - candles.get(i + 2).getLow()) < difference)) {
                if (!fractalCandles.isEmpty()) {
                    int pos = fractalCandles.size() - 1;
                    if ((!fractalCandles.get(pos).getFirst()
                            && Objects.equals(fractalCandles.get(pos).getSecond().getLow(), candles.get(i + 2).getLow()))) {
                        continue;
                    }
                }
                fractalCandles.add(Pair.of(false, candles.get(i + 2)));
            }
        }

        return fractalCandles;
    }

}
