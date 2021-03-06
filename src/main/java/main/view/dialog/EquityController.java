package main.view.dialog;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;
import main.controller.command.Command;
import main.controller.command.HoldingCommand;
import main.controller.command.TransactionCommand;
import main.model.holdings.Account;
import main.model.holdings.Equity;
import main.model.holdings.HoldingManager;
import main.view.MainController;
import main.view.elements.IntegerTextField;

import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class EquityController implements Initializable, DialogController {

    @FXML
    private IntegerTextField shares;
    @FXML
    private ListView<Account> accounts;
    @FXML
    private CheckBox outside;
    @FXML
    private Button cancel;
    @FXML
    private Button apply;
    @FXML
    private TableView<Equity> equity_list;
    @FXML
    private TextField transaction_total;
    @FXML
    private TextField search;

    private Stage stage;
    private MainController controller;
    private ArrayList<OnTransactionListener> transactionListeners;

    /**
     * Sets the main controller.
     *
     * @param controller The controller.
     */
    public void setController(MainController controller) {
        this.controller = controller;
        initValues();
    }

    /**
     * Initializes the equity controller.
     *
     * @param location  The location.
     * @param resources The resources.
     */
    public void initialize(URL location, ResourceBundle resources) {
        transactionListeners = new ArrayList<OnTransactionListener>();
        setDisabledElements(true);
        shares.textProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!newValue.equals("")) {
                    cancel.setDisable(false);
                    showTransaction(Integer.parseInt(newValue));
                } else {
                    cancel.setDisable(true);
                }
            }
        });

        outside.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                if (outside.isSelected()) {
                    apply.setDisable(false);
                    accounts.setDisable(true);
                    accounts.getSelectionModel().clearSelection();
                } else {
                    apply.setDisable(true);
                    accounts.setDisable(false);
                }
            }
        });
        cancel.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                setDisabledElements(true);
                shares.setText("");
                stage.close();
            }
        });

        apply.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                setDisabledElements(true);
                if (!equity_list.getSelectionModel().isEmpty()
                        && (!accounts.getSelectionModel().isEmpty() || outside.isSelected())) {
                    Equity equity = equity_list.getSelectionModel().getSelectedItem();
                    Equity temp = new Equity(equity.type, equity.getTickerSymbol(), equity.getName(),
                            shares.getInteger(), equity.getPrice_per_share(), equity.getMarketSectors());
                    Command holdingCommand = new HoldingCommand(HoldingCommand.Action.ADD, controller.getPortfolio(), temp);
                    Command accountCommand = null;
                    if (!outside.isSelected()) {
                        double transaction = equity.getValue()
                                - (equity.getPrice_per_share() * Integer.parseInt(shares.getText()));
                        if (transaction < 0)
                            accountCommand = new HoldingCommand(HoldingCommand.Action.MODIFY, controller.getPortfolio(), getSelectedAccount(), HoldingCommand.Modification.WITHDRAW, Math.abs(transaction));
                        else
                            accountCommand = new HoldingCommand(HoldingCommand.Action.MODIFY, controller.getPortfolio(), getSelectedAccount(), HoldingCommand.Modification.DEPOSIT, transaction);
                    }
                    if (accountCommand != null)
                        controller.sendCommand(new TransactionCommand(holdingCommand, accountCommand));
                }
                stage.close();
            }
        });
        search.textProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                Platform.runLater(new Runnable() {
                    public void run() {
                        equity_list.getSelectionModel().clearSelection();
                    }
                });
                try {
                    equity_list.setItems(FXCollections.observableArrayList(HoldingManager.searchAll(newValue, "")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Sets up the initial values for the view.
     */
    private void initValues() {
        TableColumn ticker = new TableColumn("Ticker");
        ticker.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Equity, String>, ObservableValue<String>>() {
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Equity, String> e) {
                return new SimpleStringProperty(e.getValue().getTickerSymbol());
            }
        });
        TableColumn name = new TableColumn("Name");
        name.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Equity, String>, ObservableValue<String>>() {
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Equity, String> e) {
                return new SimpleStringProperty(e.getValue().getName());
            }
        });
        TableColumn ppshare = new TableColumn("Price Per Share");
        ppshare.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Equity, String>, ObservableValue<String>>() {
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Equity, String> e) {
                return new SimpleStringProperty(Double.toString(e.getValue().getPrice_per_share()));
            }
        });
        equity_list.setItems(FXCollections.observableArrayList(HoldingManager.equities_list));
        equity_list.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                setDisabledElements(false);
                if (!shares.getText().equals(""))
                    showTransaction(shares.getInteger());
            }
        });
        equity_list.getColumns().addAll(ticker, name, ppshare);

        accounts.setItems(FXCollections.observableArrayList(controller.getAccounts()));
        accounts.setCellFactory(new Callback<ListView<Account>, ListCell<Account>>() {
            public ListCell<Account> call(ListView<Account> list) {
                return new AccountCell(EquityController.this);
            }
        });
        accounts.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Account>() {
            public void changed(ObservableValue observable, Account oldValue, Account newValue) {
                if (newValue != null) {
                    Equity equity = equity_list.getSelectionModel().getSelectedItem();
                    double transaction = equity.getValue() - (equity.getPrice_per_share() * shares.getInteger());
                    if (newValue.getBalance() + transaction > 0 && !shares.getText().equals("")) {
                        apply.setDisable(false);
                    } else
                        apply.setDisable(true);
                }
            }
        });
    }

    /**
     * Get the selected account.
     *
     * @return The account.
     */
    private Account getSelectedAccount() {
        return accounts.getSelectionModel().getSelectedItem();
    }

    /**
     * Shows the transaction.
     *
     * @param value Index of the transaction.
     */
    private void showTransaction(int value) {
        setDisabledElements(false);
        Equity equity = equity_list.getSelectionModel().getSelectedItem();
        double transaction_value = equity.getValue() - (equity.getPrice_per_share() * value);
        if (transaction_value < 0)
            transaction_total.setText("-" + NumberFormat.getCurrencyInstance().format(transaction_value * (-1)));
        else
            transaction_total.setText(NumberFormat.getCurrencyInstance().format(transaction_value));
        if (!accounts.getSelectionModel().isEmpty()) {
            if (accounts.getSelectionModel().getSelectedItem().getBalance() + transaction_value > 0) {
                apply.setDisable(false);
            } else
                apply.setDisable(true);
        }
        for (OnTransactionListener l : transactionListeners)
            l.update(transaction_value);
    }

    /**
     * Set disabled elements.
     *
     * @param disabled True to disable; false otherwise.
     */
    private void setDisabledElements(boolean disabled) {
        shares.setDisable(disabled);
        accounts.setDisable(disabled);
        outside.setDisable(disabled);
    }

    /**
     * Register a transaction listener.
     *
     * @param transactionListener The listener.
     */
    public void registerCellTransactionListener(OnTransactionListener transactionListener) {
        transactionListeners.add(transactionListener);
    }

    /**
     * Sets the stage.
     *
     * @param stage The stage.
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private class AccountCell extends ListCell<Account> implements OnTransactionListener {
        private Account item;

        public AccountCell(EquityController parent) {
            parent.registerCellTransactionListener(this);
        }

        public void update(double value) {
            setText(item == null ? ""
                    : item.getName() + "    " + NumberFormat.getCurrencyInstance().format(item.getBalance()));
            if (item != null) {
                double transaction_value = item.getBalance() + value;
                setTextFill(transaction_value < 0 ? Color.RED : Color.BLACK);
            }
        }

        @Override
        protected void updateItem(Account item, boolean empty) {
            super.updateItem(item, empty);
            this.item = item;
            if (!empty) {
                setText(item == null ? ""
                        : item.getName() + "    " + NumberFormat.getCurrencyInstance().format(item.getBalance()));
                if (item != null) {
                    double value = item.getBalance();
                    setTextFill(value < 0 ? Color.RED : Color.BLACK);
                }
            }
        }
    }

    private interface OnTransactionListener {
        void update(double value);
    }
}
