package com.example.springdemobot.service;

import com.example.springdemobot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    static final String HELP_TEXT = """
            Бот создан для обучения использования SpringBoot и telegramBots
            Краткое описание используемых команд:\s
            /start - персонализированное приветственное сообщение
            /mydata - получение сохраненной информации о текущем пользователе
            /editdata - очистка информации о текущем пользователе
            /help - показ данного сообщения снова\s
            """;
    final BotConfig config;

    public TelegramBot(BotConfig config) {
        super(config.getToken());
        this.config = config;
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("/start", "Начало работы с ботом"));
        commands.add(new BotCommand("/mydata", "Информация о пользователе"));
        commands.add(new BotCommand("/editdata", "Изменение данных о пользователе"));
        commands.add(new BotCommand("/help", "Как пользоваться этим ботом"));
        commands.add(new BotCommand("/settings", "Настройки"));
        try {
            this.execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            switch (messageText) {
                case "/start" -> startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                case "/help" -> helpCommandReceived(chatId);
                default -> unsupportedCommandReceived(chatId, messageText);
            }
        }
    }

    private void helpCommandReceived(long chatId) {
        sendMessage(chatId, HELP_TEXT);
    }

    private void startCommandReceived(long chatId, String userFirstName) {
        String answer = "Привет, " + userFirstName + ", рад видеть тебя!";
        log.info("Replied to user " + userFirstName + " on /start");
        sendMessage(chatId, answer);
    }

    private void unsupportedCommandReceived(long chatId, String command) {
        String answer = "Извините, данная команда не поддерживается";
        log.info("Unsupported command was called -" + command);
        sendMessage(chatId, answer);
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }
}
