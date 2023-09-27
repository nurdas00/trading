package com.example.trading.service;

import com.example.trading.entity.User;
import com.example.trading.model.Currency;
import com.example.trading.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService extends TelegramLongPollingBot {

    private final UserRepository userRepository;

    private String botToken = "6497420230:AAF7VdbtD-Iz4q6Q3_Q8PQ2OOF4XNYaZmDg";
    private String botUsername = "nulTradingBot";

    private String textOnStart = "Добро пожаловать. Ордер блоки появляются каждые три часа начиная с 02:00 по МСК";
    private String textOnError = "Дождитесь появления оптимальных ордер блоков. " +
            "Они появляются каждые три часа начиная с 02:00 по МСК";

    @Override
    public void onUpdateReceived(Update update) {
        log.info("Received message from: " + update.getMessage().getFrom().getUserName());

        List<User> usersList = userRepository.findAll();
        Long chatId = update.getMessage().getChatId();
        User user = usersList.stream().filter(u -> Objects.equals(u.getChatId(), chatId))
                .findFirst().orElse(null);

        if (user == null) {
            user = new User();
            user.setChatId(chatId);
            user.setUserName(update.getMessage().getFrom().getUserName());
            user.setGbp(false);
            user.setEur(false);
            userRepository.save(user);
        }

        if (Objects.equals(update.getMessage().getText(), "/start")) {
            sendMessage(chatId, textOnStart);
        } else if (Objects.equals(update.getMessage().getText(), "/gbp")) {
            if(user.getGbp() == null || !user.getGbp()) {
                user.setGbp(true);
                userRepository.save(user);
                log.info("User " + user.getUserName() + " subscribed to GBP");
                sendMessage(chatId, "Вы подписались на рассылку по фунтам");
            } else {
                sendMessage(chatId, "Вы уже получаете рассылку по фунтам");
            }

        } else if (Objects.equals(update.getMessage().getText(), "/eur")) {
            if(user.getEur() == null || !user.getEur()) {
                user.setEur(true);
                userRepository.save(user);
                log.info("User " + user.getUserName() + " subscribed to EUR");
                sendMessage(chatId, "Вы подписались на рассылку по евро");
            } else {
                sendMessage(chatId, "Вы уже получаете рассылку по евро");
            }
        } else {
            sendMessage(update.getMessage().getChatId(), textOnError);
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String text, Currency currency) {
        List<User> usersList = userRepository.findAll();

        for (User user : usersList) {
            if (currency == Currency.EUR && (user.getEur() == null || !user.getEur())) {
                continue;
            }
            if (currency == Currency.GBP && (user.getGbp() == null || !user.getGbp())) {
                continue;
            }

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(user.getChatId());
            sendMessage.setText(text);

            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
