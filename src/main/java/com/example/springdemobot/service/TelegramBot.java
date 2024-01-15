package com.example.springdemobot.service;

import com.example.springdemobot.config.BotConfig;
import com.example.springdemobot.model.User;
import com.example.springdemobot.model.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    static final String YES_BUTTON = "YES_BUTTON";
    static final String NO_BUTTON = "NO_BUTTON";
    static final String ERROR_TEXT = "Error occurred: ";
    static final String START = "/start";
    static final String HELP = "/help";
    static final String SETTINGS = "/settings";
    static final String SEND = "/send";
    static final String REGISTER = "register";
    static final String CHECK_MY_DATA = "check my data";
    static final String DELETE_MY_DATA = "delete my data";
    static final String USER_NOT_FOUND = "Такого пользователя не существует";
    static final String HELP_TEXT = """
            Бот создан для обучения использования SpringBoot и telegramBots
            Краткое описание используемых команд:\s
            /start - персонализированное приветственное сообщение
            register - сохранение данных о пользователе
            check my data - получение сохраненной информации о текущем пользователе
            delete my data - очистка информации о текущем пользователе
            /help - показ данного сообщения снова\s
            """;
    final BotConfig config;
    @Autowired
    private UserRepository userRepository;

    public TelegramBot(BotConfig config) {
        super(config.getToken());
        this.config = config;
        setBotCommands();
    }



    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            if (messageText.contains(SEND)) {
                sendMassMessage(messageText);
            }
            switch (messageText) {
                case START -> startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                case HELP -> helpCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                case REGISTER -> register(update.getMessage());
                case CHECK_MY_DATA -> getUserById(chatId);
                case DELETE_MY_DATA -> deleteUser(chatId);
                default -> unsupportedCommandReceived(chatId, messageText);
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            if (callbackData.equals(YES_BUTTON)) {
                String text = "Сохраняю данные";
                executeEditMessageText(text, chatId, messageId);
                log.info("YES button tapped");
                saveUser(update.getCallbackQuery().getMessage());

            } else if (callbackData.equals(NO_BUTTON)) {
                String text = "Отказ от сохранения данных";
                executeEditMessageText(text, chatId, messageId);
                log.info("NO button tapped");
            }
        }
    }

    private void setBotCommands() {
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand(START, "Начало работы с ботом"));
        commands.add(new BotCommand(HELP, "Как пользоваться ботом"));
        commands.add(new BotCommand(SETTINGS, "Настройки"));
        try {
            this.execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    private void sendMassMessage(String messageText) {
        var text = messageText.substring(messageText.indexOf(" "));
        var users = userRepository.findAll();
        users.forEach(u -> prepareAndSendMessage(u.getChatId(), text));
    }

    private void deleteUser(long chatId) {
        if (userRepository.existsById(chatId)) {
            userRepository.deleteById(chatId);
            prepareAndSendMessage(chatId,"Пользователь успешно удален");
            log.info("Successfully delete user " + chatId);
        } else {
            prepareAndSendMessage(chatId, USER_NOT_FOUND);
            log.info("Try to delete non created user: " + chatId);
        }
    }

    private void getUserById(long chatId) {
        Optional<User> user = userRepository.findById(chatId);
        if(user.isPresent()){
            String userInfo =  user.get().toString();
            String text = "Вот что мы нашли о тебе: ";
            prepareAndSendMessage(chatId, text + "\n" + userInfo);
        } else {
            prepareAndSendMessage(chatId,USER_NOT_FOUND);
        }

    }

    private void register(Message msg) {
        SendMessage message = new SendMessage();
        message.setChatId(msg.getChatId());
        message.setText("Ты точно хочешь зарегистрироваться?");
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> line = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();
        yesButton.setText("Да");
        yesButton.setCallbackData(YES_BUTTON);
        var noButton = new InlineKeyboardButton();
        noButton.setText("Нет");
        noButton.setCallbackData(NO_BUTTON);
        line.add(yesButton);
        line.add(noButton);
        rows.add(line);

        inlineKeyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(inlineKeyboardMarkup);

        executeMessage(message);
        log.info("Command register was called");
    }

    private void saveUser(Message message) {
        var chatId = message.getChatId();
        if (!userRepository.existsById(chatId)) {
            var chat = message.getChat();
            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(user);
            log.info("user saved: " + user);
            prepareAndSendMessage(chatId, "Пользователь " + user.getUserName() + " сохранен");
        } else {
            prepareAndSendMessage(chatId, "Такой пользователь уже сохранен");
            log.info("User with charId " + chatId + " has already been created");
        }

    }

    private void helpCommandReceived(long chatId, String userFirstName) {
        log.info("Help was called " + userFirstName);
        prepareAndSendMessage(chatId, HELP_TEXT);
    }

    private void startCommandReceived(long chatId, String userFirstName) {
        String answer = EmojiParser.parseToUnicode("Привет, " + userFirstName + "! рад видеть тебя! :call_me_hand:"); //from https://emojipedia.org/
        log.info("Replied to user " + userFirstName + " on /start");
        sendMessage(chatId, answer);
    }

    private void unsupportedCommandReceived(long chatId, String command) {
        if (!command.contains(SEND)) {
            String answer = "Извините, данная команда не поддерживается";
            log.info("Unsupported command was called -" + command);
            prepareAndSendMessage(chatId, answer);
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add(REGISTER);
        keyboardRow.add(CHECK_MY_DATA);
        keyboardRow.add(DELETE_MY_DATA);
        keyboardRows.add(keyboardRow);
        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
        executeMessage(message);
    }

    private void prepareAndSendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        executeMessage(message);
    }

    private void executeEditMessageText(String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(text);
        message.setMessageId((int) messageId);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }
}
