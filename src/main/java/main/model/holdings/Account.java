package model.holdings;

import java.util.Calendar;
import java.util.Date;

public abstract class Account extends Holding {

    public enum Type{
        BANK,
        MONEY_MARKET
    };

    private String bankName;
    private double balance;
    private Date opened;
    private Type type;

    /**
     * Creates a new Account object
     * @param bankName The name of the bank
     * @param balance The initial balance in the account
     * @param type The type of the account
     */
    public Account(String bankName, double balance, Type type) {
        this.bankName = bankName;
        this.balance = balance;
        this.type = type;
        opened = Calendar.getInstance().getTime();
    }

    /**
     * Get the name of the Bank
     * @return The name of the bank
     */
    public String getBankName() {
        return bankName;
    }

    /**
     * Get the account balance
     * @return The account balance
     */
    public double getBalance() {
        return balance;
    }

    /**
     * Withdraws the given ammount from the account and returns the remaining balance
     * @param ammount The ammount to withdraw
     * @return The remaining account balance
     * @throws NegativeValueException
     * @throws OverDrawException
     */
    public double withdraw(double ammount) throws NegativeValueException, OverDrawException{
        if(ammount < 0)
            throw new NegativeValueException("Please give a positive value");
        if(balance - ammount < 0)
            throw new OverDrawException("You cannot overdraw your account");
        return this.balance -= ammount;
    }

    /**
     * Deposits the given ammount from the account and returns the new balance
     * @param ammount The ammount to deposit
     * @return The new account balance
     * @throws NegativeValueException
     */
    public double deposit(double ammount) throws NegativeValueException{
        if(ammount < 0)
            throw new NegativeValueException("Please give a positive value");
        return this.balance += ammount;
    }

    /**
     * Transfers the given ammount to the new account
     * @param ammount The ammount to transfer
     * @param account The account to transfer the ammount to
     * @return The remaining account balance
     * @throws NullAccountException
     * @throws NegativeValueException
     * @throws OverDrawException
     */
    public double transfer(double ammount, Account account) throws NullAccountException, NegativeValueException, OverDrawException{
        if(account == null)
            throw new NullAccountException("Please give a valid account");
        if(ammount < 0)
            throw new NegativeValueException("Please give a positive value");
        if(balance - ammount < 0)
            throw new OverDrawException("You cannot overdraw your account");
        this.withdraw(ammount);
        account.deposit(ammount);
        return this.getBalance();
    }

    /**
     * Get the type of the account
     * @return The type of the account
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the date the account was opened
     * @return The date the account was opened
     */
    public Date getOpened() {
        return opened;
    }

    public class OverDrawException extends Exception{
        public OverDrawException(String message){
            super(message);
        }
    }

    public class NegativeValueException extends Exception{
        public NegativeValueException(String message){
            super(message);
        }
    }

    public class NullAccountException extends Exception{
        public NullAccountException(String message){
            super(message);
        }
    }
}