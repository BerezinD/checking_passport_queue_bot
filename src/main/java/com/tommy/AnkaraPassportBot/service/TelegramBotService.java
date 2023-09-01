package com.tommy.AnkaraPassportBot.service;

import com.tommy.AnkaraPassportBot.config.BotConfig;
import com.tommy.AnkaraPassportBot.database.UserRepository;
import com.tommy.AnkaraPassportBot.model.UserForAnkara;
import com.tommy.AnkaraPassportBot.service.selenium.SiteAccessService;
import jakarta.inject.Inject;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class TelegramBotService extends TelegramLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramBotService.class);

    final private BotConfig config;

    @Inject
    SiteAccessService accessService;

    @Inject
    UserRepository userRepository;

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
                case "/calendar" -> calendarCommandReceived(chatId);
                case "/check_passport_readiness" -> checkPassportReadinessCommandReceived(chatId);
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

    private void calendarCommandReceived(Long chatId) {
        UserForAnkara currentUser = getUserForAnkaraFromDB(chatId);
        accessService.parseAnkaraPage(currentUser, new FirefoxDriver());
    }

    private void checkPassportReadinessCommandReceived(Long chatId) {
        UserForAnkara currentUser = getUserForAnkaraFromDB(chatId);
        boolean isNameExists = accessService.isNameInThePassportPage(currentUser, new FirefoxDriver());
        if (isNameExists) {
            sendMessage(chatId, "Passport is ready, your surname is on the list!");
        } else {
            sendMessage(chatId, "No sight of you surname on the list of new passports :-(");
        }
    }

    private UserForAnkara getUserForAnkaraFromDB(Long chatId) {
        // fetching a user from DB by chat ID. In case of new user, create a new one in DB
        Optional<UserForAnkara> currentUser = userRepository.findById(chatId);
        if (currentUser.isEmpty()) {
            // TODO change mock user to a user from DB polluted manually
            currentUser = Optional.of(userRepository.save(new UserForAnkara(chatId, "Иван", "Иванов", "Иванович",
                    "mock@email.com", "+905551234567", LocalDate.of(1990, 1, 1))));
        }
        return currentUser.get();
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

    private void sendPhoto(Long chatId, InputFile photo) {
        SendPhoto photoToSend = new SendPhoto(String.valueOf(chatId), photo);
        try {
            execute(photoToSend);
        } catch (TelegramApiException e) {
            LOGGER.error("An error has occur during photo transfer.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }
}
