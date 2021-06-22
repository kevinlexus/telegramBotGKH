package com.dic.bot.mng.impl;

import java.util.Map;

public enum MeterValSaveState {
    SUCCESSFUL("Показания успешно переданы"),
    VAL_SAME_OR_LOWER("Показания те же или меньше текущих"),
    METER_NOT_FOUND("Счетчик не найден"),
    VAL_TOO_BIG("Показания слишком большие"),
    ERROR_WHILE_SENDING("Ошибка при передаче показаний"),
    WRONG_FORMAT("Некорректный формат показаний, используйте например: 1234.543"),
    VAL_TOO_BIG_OR_LOW("Показания вне допустимого диапазона");

    private final String name;

    MeterValSaveState(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }


}
