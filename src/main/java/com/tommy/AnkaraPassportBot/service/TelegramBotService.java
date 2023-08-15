package com.tommy.AnkaraPassportBot.service;

import com.tommy.AnkaraPassportBot.config.BotConfig;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBotService extends TelegramLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramBotService.class);

    final private BotConfig config;

    @Inject
    TelegramBotService(BotConfig config) {
        super(config.getBotToken());
        this.config = config;
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message receivedMessage = update.getMessage();
        if (update.hasMessage() && receivedMessage.hasText()) {
            String messageText = receivedMessage.getText();
            Long chatId = receivedMessage.getChatId();
            switch (messageText) {
                case "/start" -> startCommandReceived(chatId, receivedMessage.getChat().getFirstName());
                default -> {
                    sendMessage(chatId, "Not supported operation");
                    LOGGER.info("No supported operation was found. User input: %s".formatted(messageText));
                }
            }
        }
    }

    private void startCommandReceived(Long chatId, String firstName) {
        sendMessage(chatId, "Hello, %s and welcome".formatted(firstName));
    }

    private void sendMessage(Long chatId, String messageText) {
        SendMessage message = new SendMessage(String.valueOf(chatId), messageText);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            LOGGER.error("An error has occur. Message to execute: %s".formatted(messageText));
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }
}
