package main.model;

import javafx.beans.property.SimpleStringProperty;
import main.model.holdings.Holding;

import java.io.Serializable;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Transaction implements Serializable{
    public String date;
    public String holding;
    public String amount = "";
    public String type;

    public enum Type {
        ADD,
        REMOVE,
        ACCOUNT_TRANSFER,
        ACCOUNT_DEPOSIT,
        ACCOUNT_WITHDRAW,
        EQUITY_BUY_SELL
    }

    public Transaction(Type type, Object[] args) {
        Holding h1 = null, h2 = null;
        double amount = 0;
        switch (type) {
            case ADD:
                h1 = (Holding) args[0];
                break;
            case REMOVE:
                h1 = (Holding) args[0];
                break;
            case ACCOUNT_DEPOSIT:
                h1 = (Holding) args[0];
                amount = (Double) args[1];
                break;
            case ACCOUNT_TRANSFER:
                h1 = (Holding) args[0];
                h2 = (Holding) args[1];
                amount = (Double) args[2];
                break;
            case ACCOUNT_WITHDRAW:
                h1 = (Holding) args[0];
                amount = (Double) args[1];
                break;
            case EQUITY_BUY_SELL:
                h1 = (Holding) args[0];
                amount = (Double) args[1];
                break;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy kk:mm");
        date = sdf.format(Calendar.getInstance().getTime());
        this.type = type.toString();
        if(amount != 0)
            this.amount = NumberFormat.getCurrencyInstance().format(amount);
        String hFinal = h1.toString();
        if (h2 != null)
            hFinal += (", " + h2.toString());
        this.holding = hFinal;
    }
}