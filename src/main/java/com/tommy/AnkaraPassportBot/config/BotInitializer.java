package com.tommy.AnkaraPassportBot.config;

import com.tommy.AnkaraPassportBot.service.TelegramBotService;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class BotInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BotInitializer.class);

    @Inject
    TelegramBotService botService;

    @EventListener({ContextRefreshedEvent.class})
    public void init() throws TelegramApiException{
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(botService);
        } catch (TelegramApiException e) {
            LOGGER.error("An error has occur: %s".formatted(e.getLocalizedMessage()));
            throw e;
        }
    }
}
