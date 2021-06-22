package com.dic.bot.message;

import lombok.Value;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Value
public class SimpleMessage implements TelegramMessage {
    SendMessage sm;

    public SimpleMessage(SendMessage sm) {

        this.sm = sm;
    }
}
