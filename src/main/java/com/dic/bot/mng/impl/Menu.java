package com.dic.bot.mng.impl;

enum Menu {
    UNDEFINED("Неопределено"),
    MAIN("Основное меню"),
    //SELECTED_ADDRESS("Выбран адрес"),
    SELECT_ADDRESS("Выбор адреса"),
    SELECT_METER("Выбор счетчика"),
    INPUT_VOL("Введите показания"),
    ENTERED_VOL("Введены показания");

    private final String name;

    Menu(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
