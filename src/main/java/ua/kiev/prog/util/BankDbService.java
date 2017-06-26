package ua.kiev.prog.util;

import ua.kiev.prog.entity.*;
import ua.kiev.prog.entity.enums.Currency;
import ua.kiev.prog.entity.enums.TransactionType;

import javax.persistence.EntityManager;
import javax.persistence.Query;
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
public class BankDbService {
    private static EntityManager em;

    public BankDbService(EntityManager em) {
        this.em = em;
    }

    public void putTestDataIntoTables() {
        User user1 = new User("Name of User-1");
        User user2 = new User("Name of User-2");
        User user3 = new User("Name of User-3");
        user1.addAccount(new Account(Currency.UAH));
        user1.addAccount(new Account(Currency.USD));
        user1.addAccount(new Account(Currency.EUR));
        user2.addAccount(new Account(Currency.UAH));
        user2.addAccount(new Account(Currency.USD));
        user3.addAccount(new Account(Currency.UAH));

        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setEUR(29d);
        exchangeRate.setUSD(26d);
        exchangeRate.setUAH(1d);

        em.getTransaction().begin();
        try {
            em.persist(user1);
            em.persist(user2);
            em.persist(user3);
            em.persist(exchangeRate);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
            throw new RuntimeException("Exception in putTestDataIntoTables() method ");
        }
    }

    public List<User> getAllUsers() {
        return em.createQuery("select u from users u", User.class).getResultList();
    }

    public List<Account> getAccountsOfUser(User user) {
        Query query = em.createQuery("select a from accounts a where a.user_id=:user_id", Account.class);
        query.setParameter("user_id", user.getId());
        return query.getResultList();
    }

    public void refill(Double amountFrom, Currency currencyFrom, Account toAccount) {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.Refill);
        transaction.setAmmountFrom(amountFrom);
        transaction.setCurrencyFrom(currencyFrom);
        transaction.setToAccount(toAccount);
        Currency currencyTo = toAccount.getCurrency();
        transaction.setCurrencyTo(currencyTo);
        if(currencyTo == currencyFrom) {
            transaction.setAmmountTo(amountFrom);
            toAccount.putMoney(amountFrom);
        } else {
            ExchangeRate exchangeRate = getTodayExchangeRate();
            transaction.setExchangeRate(exchangeRate);
            Double amountTo = amountFrom*exchangeRate.get(currencyFrom)/exchangeRate.get(currencyTo);
            transaction.setAmmountTo(amountTo);
            toAccount.putMoney(amountTo );
        }
        em.getTransaction().begin();
        try {
            em.persist(transaction);
            em.persist(toAccount);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
            throw new RuntimeException("Exception in refill() method");
        }
    }

    public void transfer(Double amount, Account fromAccount, Account toAccount) {
        if(fromAccount.getCurrency() != toAccount.getCurrency()) {
            throw new UnsupportedOperationException();
        }

        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.Transfer);
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setCurrencyFrom(fromAccount.getCurrency());
        transaction.setCurrencyTo(toAccount.getCurrency());
        transaction.setAmmountFrom(amount);
        transaction.setAmmountTo(amount);
        fromAccount.withdrawMoney(amount);
        toAccount.putMoney(amount);
        em.getTransaction().begin();
        try {
            em.persist(transaction);
            em.persist(fromAccount);
            em.persist(toAccount);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
            throw new RuntimeException("Exception in transfer() method");
        }
    }

    public void convert(Double amountConvertFrom, Currency convertFrom, Account fromAccount, Account toAccount) {
        Currency convertTo = toAccount.getCurrency();
        if(fromAccount.getUser().getId() != fromAccount.getUser().getId()
                || fromAccount.getCurrency() != convertFrom || convertFrom == convertTo) {
            throw new IllegalArgumentException();
        }
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.Convert);
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setCurrencyFrom(convertFrom);
        transaction.setCurrencyTo(convertTo);
        ExchangeRate exchangeRate = getTodayExchangeRate();
        transaction.setExchangeRate(exchangeRate);
        Double amountConvertTo = amountConvertFrom*exchangeRate.get(convertFrom)/exchangeRate.get(convertTo);
        transaction.setAmmountFrom(amountConvertFrom);
        transaction.setAmmountTo(amountConvertTo);
        fromAccount.withdrawMoney(amountConvertFrom);
        toAccount.putMoney(amountConvertTo);
        em.getTransaction().begin();
        try {
            em.persist(transaction);
            em.persist(fromAccount);
            em.persist(toAccount);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
            throw new RuntimeException("Exception in convert() method");
        }
    }

    public Double getAllMoneyInUAH(User user) {
        Double total = 0d;
        ExchangeRate exchangeRate = getTodayExchangeRate();
        List<Account> accounts = user.getAccounts();
        for(Account account : accounts) {
            Currency currency = account.getCurrency();
            total += account.getAmount() * exchangeRate.get(currency);
        }
        System.out.println("On all accounts of " + user.getName() + "'s there is " + total + " UAH");
        return total;
    }

    public ExchangeRate getTodayExchangeRate() {
        Query query = em.createQuery("select er from exchange_rates er where er.date=:date", ExchangeRate.class);
        query.setParameter("date", new java.util.Date(System.currentTimeMillis()));
        ExchangeRate exchangeRate = (ExchangeRate) query.getSingleResult();
        return exchangeRate;
    }

}
