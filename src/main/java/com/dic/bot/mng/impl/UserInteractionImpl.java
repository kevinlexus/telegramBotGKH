package com.dic.bot.mng.impl;

import com.dic.bot.message.SimpleMessage;
import com.dic.bot.message.TelegramMessage;
import com.dic.bot.message.UpdateMessage;
import com.dic.bot.mng.UserInteraction;
import com.ric.dto.KoAddress;
import com.ric.dto.MapKoAddress;
import com.ric.dto.MapMeter;
import com.ric.dto.SumMeterVolExt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserInteractionImpl implements UserInteraction {

    private final Env env = new Env();
    private final Map<Integer, MeterValSaveState> statusCode =
            Map.of(0, MeterValSaveState.SUCCESSFUL,
                    3, MeterValSaveState.VAL_SAME_OR_LOWER,
                    4, MeterValSaveState.METER_NOT_FOUND,
                    5, MeterValSaveState.VAL_TOO_BIG
            );

    @Value("${bot.billHost}")
    private String billHost;

    @Override
    public TelegramMessage selectAddress(Update update, long userId, Map<Long, MapKoAddress> registeredKo) {
        String msg;
        MapKoAddress mapKoAddress = registeredKo.get(userId);
        msg = "Выберите адрес:\r\n";
        msg += String.join("\r\n",
                mapKoAddress.getMapKoAddress().values().stream().map(t -> t.getOrd() + ". " + t.getAddress())
                        .collect(Collectors.toSet()));
        EditMessageText em = new EditMessageText();
        SendMessage sm = new SendMessage();

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        for (KoAddress koAddress : mapKoAddress.getMapKoAddress().values()) {
            buttons.add(new InlineKeyboardButton()
                    .setText(String.valueOf(koAddress.getOrd()))
                    .setCallbackData("selectedKlsk_" + koAddress.getKlskId()));
        }
        inlineKeyboardMarkup.getKeyboard().add(buttons);

/*
        if (env.getCurMessageId().get(userId) != null) {
            em.setText(msg);
            em.setChatId(env.getCurChatId().get(userId));
            em.setMessageId(env.getCurMessageId().get(userId));
            em.setReplyMarkup(inlineKeyboardMarkup);
            log.info("Edit message chatId={}, messageId={}", update.getMessage().getChatId(), update.getMessage().getMessageId());
            return new UpdateMessage(em);
        } else {
            sm.setText(msg);
            sm.setChatId(update.getMessage().getChatId().toString());
            sm.setReplyMarkup(inlineKeyboardMarkup);
            env.getCurChatId().put(userId, update.getMessage().getChatId());
            env.getCurMessageId().put(userId, update.getMessage().getMessageId());
            log.info("New message chatId={}, messageId={}", update.getMessage().getChatId(), update.getMessage().getMessageId());
            return new SimpleMessage(sm);
        }
*/


        if (update.getMessage() == null) {
            em.setText(msg);
            em.setChatId(update.getCallbackQuery().getMessage().getChatId().toString());
            em.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
            em.setReplyMarkup(inlineKeyboardMarkup);
            return new UpdateMessage(em);
        } else {
            sm.setText(msg);
            sm.setChatId(update.getMessage().getChatId().toString());
            sm.setReplyMarkup(inlineKeyboardMarkup);
            return new SimpleMessage(sm);
        }

    }

    @Override
    public TelegramMessage selectMeter(Update update, long userId) {
        StringBuilder msg = new StringBuilder();
        // присвоить адрес
        Long klskId = null;
        EditMessageText em = new EditMessageText();
        SendMessage sm = new SendMessage();
        if (update.getCallbackQuery() != null) {
            String callBackStr = update.getCallbackQuery().getData();
            if (callBackStr != null) {
                klskId = Long.parseLong(callBackStr.substring(13));
                updateMapMeterByCurrentKlskId(userId, klskId);
            }
        } else {
            klskId = env.getUserCurrentKo().get(userId).getKlskId();
        }
        msg.append("Адрес: ").append(env.getUserCurrentKo().get(userId).getAddress()).append("\r\n");
        msg.append("Выберите счетчик:\r\n");
        em.setText(msg.toString());

        // настройки, для выбора счетчиков
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        String msgKeyb = "Назад";

        if (klskId != null) {
            int i = 1;
            for (SumMeterVolExt sumMeterVol : env.getMetersByKlskId().get(klskId).getMapKoMeter().values()) {
                msg.append(i);
                msg.append(". ");
                msg.append(sumMeterVol.getServiceName());
                msg.append(", текущ.: показания=");
                msg.append(sumMeterVol.getN1());
                msg.append(", расход=");
                msg.append(sumMeterVol.getVol());
                msg.append("\r\n");
                buttons.add(new InlineKeyboardButton()
                        .setText(String.valueOf(i++))
                        .setCallbackData("selectedMeter_" + sumMeterVol.getMeterId()));
            }
            inlineKeyboardMarkup.getKeyboard().add(buttons);
            if (update.getMessage() == null) {
                em.setText(msg.toString());
                em.setChatId(update.getCallbackQuery().getMessage().getChatId().toString());
                em.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
                em.setReplyMarkup(inlineKeyboardMarkup);
            } else {
                sm.setText(msg.toString());
                sm.setChatId(update.getMessage().getChatId());
                sm.setReplyMarkup(inlineKeyboardMarkup);
            }
        } else {
            log.error("Не определен klskId");
            msgKeyb = "Не выбран адрес";
        }
        buttons.add(new InlineKeyboardButton()
                .setText(msgKeyb)
                .setCallbackData("selectedMeterBack"));

        if (update.getMessage() == null) {
            return new UpdateMessage(em);
        } else {
            return new SimpleMessage(sm);
        }
    }

    /**
     * Обновить показания счетчиков по klskId
     *
     * @param userId пользователь
     * @param klskId klskId фин.лиц.
     */
    @Override
    public void updateMapMeterByCurrentKlskId(long userId, long klskId) {
        MapKoAddress registeredKoByUser = env.getUserRegisteredKo().get(userId);
        env.getUserCurrentKo().put(userId, registeredKoByUser.getMapKoAddress().get(klskId));
        env.getMetersByKlskId().put(klskId, getMapMeterByKlskId(klskId));
    }

    @Override
    public TelegramMessage inputVol(Update update, long userId) {
        // присвоить счетчик
        Integer meterId = null;
        EditMessageText em = new EditMessageText();
        if (update.getCallbackQuery() != null) {
            String callBackStr = update.getCallbackQuery().getData();
            if (callBackStr != null) {
                meterId = Integer.parseInt((callBackStr.substring(14)));
            }
        } else {
            meterId = env.getUserCurrentMeter().get(userId).getMeterId();
        }

        // настройки для ввода показаний
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        String msgKeyb = "Назад";
        if (update.getMessage() == null) {
            em.setChatId(update.getCallbackQuery().getMessage().getChatId().toString());
            em.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
        } else {
            em.setChatId(update.getMessage().getChatId());
        }
        if (meterId != null) {
            Long currKlskId = env.getUserCurrentKo().get(userId).getKlskId();
            MapMeter mapMeter = env.getMetersByKlskId().get(currKlskId);
            SumMeterVolExt meter = mapMeter.getMapKoMeter().get(meterId);
            env.getUserCurrentMeter().put(userId, meter);
            Map<Integer, SumMeterVolExt> mapKoMeter = env.getMetersByKlskId().get(currKlskId)
                    .getMapKoMeter();
            SumMeterVolExt sumMeterVolExt = mapKoMeter.get(meterId);
            env.getMeterVolExtByMeterId().put(meterId, sumMeterVolExt);
            String msg = "Введите новое показание счетчика по услуге: " + sumMeterVolExt.getServiceName() + ", текущие показания="
                    + sumMeterVolExt.getN1() + ", расход=" + sumMeterVolExt.getVol();

            inlineKeyboardMarkup.getKeyboard().add(buttons);
            em.setText(msg);
        } else {
            log.error("Не определен meterId");
            msgKeyb = "Не выбран счетчик";
        }
        buttons.add(new InlineKeyboardButton()
                .setText(msgKeyb)
                .setCallbackData("selectedInputBack"));

        return new UpdateMessage(em);
    }

    /**
     * @param userId Id пользователя в Telegram
     * @return код для процедуры сопоставления Id пользователя с Директ klskId
     */
    @Override
    public int authenticateUser(long userId) {
        MapKoAddress mapKoAddress = getListKoAddressByTelegramUserId(userId);
        if (mapKoAddress.getMapKoAddress().size() == 0) {
            // временный код, для регистрации
            return env.getUserTemporalCode().computeIfAbsent(userId, t -> {
                        int randCode = -1;
                        while (randCode == -1 || !env.getIssuedCodes().add(randCode)) {
                            randCode = ThreadLocalRandom.current().nextInt(1000, 10000);
                        }
                        return randCode;
                    }
            );
        } else {
            env.getUserRegisteredKo().put(userId, mapKoAddress);
            return 0;
        }
    }

    private MapKoAddress getListKoAddressByTelegramUserId(long userId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Object> request = new HttpEntity<>(headers);
        Map<String, String> params = new HashMap<>();
        params.put("userId", String.valueOf(userId));
        ResponseEntity<MapKoAddress> result = restTemplate.exchange(
                "http://" + billHost + "/getMapKoAddressByTelegramUserId/{userId}",
                HttpMethod.GET,
                request,
                MapKoAddress.class,
                params
        );
        return result.getBody();
    }

    private MapMeter getMapMeterByKlskId(long klskId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Object> request = new HttpEntity<>(headers);
        Map<String, String> params = new HashMap<>();
        params.put("klskId", String.valueOf(klskId));
        ResponseEntity<MapMeter> result = restTemplate.exchange(
                "http://" + billHost + "/getMapMeterByKlskId/{klskId}",
                HttpMethod.GET,
                request,
                MapMeter.class,
                params
        );
        return result.getBody();
    }

    @Override
    public MeterValSaveState saveMeterValByMeterId(long meterId, String strVal) {
        try {
            double val = Double.parseDouble(strVal.replace(",", "."));
            if (val > 9999999 || val < -9999999) {
                return MeterValSaveState.VAL_TOO_BIG_OR_LOW;
            }
            Integer ret = setMeterValByKlskId(meterId, val);
            return statusCode.get(ret);
        } catch (NumberFormatException e) {
            return MeterValSaveState.WRONG_FORMAT;
        } catch (Exception e) {
            e.printStackTrace();
            return MeterValSaveState.ERROR_WHILE_SENDING;
        }
    }

    private Integer setMeterValByKlskId(long meterId, Double val) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Object> request = new HttpEntity<>(headers);
        Map<String, String> params = new HashMap<>();
        params.put("meterId", String.valueOf(meterId));
        params.put("val", String.valueOf(val));
        ResponseEntity<Integer> result = restTemplate.exchange(
                "http://" + billHost + "/setMeterValByMeterId/{meterId}/{val}",
                HttpMethod.GET,
                request,
                Integer.class,
                params
        );
        return result.getBody();
    }

    @Override
    public Env getEnv() {
        return env;
    }
}
