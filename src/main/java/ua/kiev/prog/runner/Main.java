package ua.kiev.prog.runner;

import ua.kiev.prog.entity.*;
import ua.kiev.prog.entity.enums.Currency;
import ua.kiev.prog.util.BankDbService;
import ua.kiev.prog.util.DbManager;

import java.util.List;


/*
Создать базу данных «Банк» с таблицами «Пользователи»,
«Транзакции», «Счета» и «Курсы валют». Счет бывает 3-х видов:
USD, EUR, UAH. Написать запросы для пополнения счета в нужной
валюте, перевода средств с одного счета на другой, конвертации
валюты по курсу в рамках счетов одного пользователя. Написать
запрос для получения суммарных средств на счету одного
пользователя в UAH (расчет по курсу).
 */

public class Main {

    public static void main(String[] args) {
        BankDbService db = new BankDbService(DbManager.getEntityManager());

        db.putTestDataIntoTables();
        List<User> users = db.getAllUsers();

        for(User user: users) {
            db.refill(10000d, Currency.UAH, user.getAccounts().get(0));
        }

        db.convert(5000d, Currency.UAH, users.get(0).getAccount(Currency.UAH), users.get(0).getAccount(Currency.USD));
        db.transfer(10d, users.get(0).getAccount(Currency.USD), users.get(1).getAccount(Currency.USD));
        db.convert(5000d, Currency.UAH, users.get(0).getAccount(Currency.UAH), users.get(0).getAccount(Currency.EUR));

        db.getAllMoneyInUAH(users.get(0));
        db.getAllMoneyInUAH(users.get(1));
        db.getAllMoneyInUAH(users.get(2));

        DbManager.closeEntityManager();
    }
}
