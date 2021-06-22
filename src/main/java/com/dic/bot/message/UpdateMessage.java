package com.dic.bot.message;

import lombok.Value;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Value
public class UpdateMessage implements TelegramMessage {
    EditMessageText em;

    public UpdateMessage(EditMessageText em) {

        this.em = em;
    }
}
