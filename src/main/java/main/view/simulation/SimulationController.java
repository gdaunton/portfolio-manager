package main.view.simulation;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Callback;
import main.model.Portfolio;
import main.model.holdings.Equity;
import main.model.holdings.Holding;
import main.model.simulation.Bear;
import main.model.simulation.Bull;
import main.model.simulation.NoGrowth;
import main.model.simulation.Simulation;
import main.view.MainController;
import main.view.elements.DoubleTextField;

import java.net.URL;
import java.text.NumberFormat;
import java.util.*;


public class SimulationController implements Initializable, OnNodeSelectedListener, Observer {

    @FXML
    private Button step;
    @FXML
    private Button new_sim;
    @FXML
    private Button reset;
    @FXML
    private ComboBox<Simulation.STEP_SIZE> step_size;
    @FXML
    private DoubleTextField rate;
    @FXML
    private LineChart<Date, Number> chart;
    @FXML
    private TableView<Holding> port_table;
    @FXML
    private Text total;

    private ClickablePortfolioNode selected;
    private Portfolio op;
    private Simulation s;
    private ObservableList<XYChart.Data<Date, Portfolio>> data;
    private MainController controller;

    /**
     * Sets the record.
     *
     * @param controller The controller.
     */
    public void setSimulation(MainController controller, Simulation sim, Portfolio p) {
        this.s = sim;
        this.controller = controller;
        this.op = p == null ? controller.getPortfolio() : p;
        controller.addObserver(this);
        data = FXCollections.observableArrayList();
        initValues();
        if (sim instanceof NoGrowth)
            rate.setDisable(true);
        else {
            rate.setValue(Simulation.DEFAULT_RATE);
        }
    }

    /**
     * Initializes the record controller.
     *
     * @param location  The location.
     * @param resources The resources.
     */
    public void initialize(URL location, ResourceBundle resources) {
        step.setOnAction(event -> do_simulate());
        reset.setOnAction(event -> resetView());
        new_sim.setOnAction(event -> showNewSim());
        step_size.setItems(FXCollections.observableArrayList(Simulation.STEP_SIZE.values()));
        rate.setOnEnterPressedListener(() -> {
            updateData();
            rate.getParent().requestFocus();
        });
    }

    /**
     * Do the simulation
     */
    public void do_simulate() {
        data.add(new XYChart.Data<>(getNextDate(data.get(data.size() - 1).getXValue()), s.simulate(steps(), step_size(), data.get(data.size() - 1).getYValue().clone(), rate())));
        updateView();
    }

    /**
     * Get the next data from the given date
     *
     * @param date the previous date
     * @return the next date
     */
    private Date getNextDate(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        switch (step_size()) {
            case DAY:
                c.add(Calendar.DATE, 1);
                break;
            case MONTH:
                c.add(Calendar.MONTH, 1);
                break;
            case YEAR:
                c.add(Calendar.YEAR, 1);
                break;
        }
        return c.getTime();
    }

    /**
     * Get the step size
     *
     * @return the size of the step
     */
    private Simulation.STEP_SIZE step_size() {
        return step_size.getValue();
    }

    /**
     * Get the current number of steps
     *
     * @return the number of steps
     */
    private int steps() {
        return data.size();
    }

    /**
     * Get the rate of change
     *
     * @return the rate of change
     */
    private double rate() {
        return rate.getDouble();
    }

    /**
     * Sets the initial values of the components.
     */
    private void initValues() {
        step_size.setValue(Simulation.STEP_SIZE.DAY);
        data.add(new XYChart.Data<>(Calendar.getInstance().getTime(), op.clone()));

        data.addListener((ListChangeListener<XYChart.Data<Date, Portfolio>>) c -> {
            if (selected != null) {
                Node n = chart.getData().get(0).getData().get(selected.getIndex()).getNode();
                ((ClickablePortfolioNode) n).select();
            }
        });

        TableColumn name = new TableColumn("Holding Name");
        name.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Holding, String>, ObservableValue<String>>() {
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Holding, String> t) {
                return new SimpleStringProperty(t.getValue().toString());
            }
        });
        TableColumn shares = new TableColumn("Shares");
        shares.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Holding, String>, ObservableValue<String>>() {
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Holding, String> t) {
                if (t.getValue() instanceof Equity)
                    return new SimpleStringProperty(Double.toString(((Equity) t.getValue()).getShares()));
                else
                    return new SimpleStringProperty("");
            }
        });
        TableColumn ppshare = new TableColumn("Price Per Share");
        ppshare.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Holding, String>, ObservableValue<String>>() {
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Holding, String> t) {
                if (t.getValue() instanceof Equity)
                    return new SimpleStringProperty(Double.toString(((Equity) t.getValue()).getPrice_per_share()));
                else
                    return new SimpleStringProperty("");
            }
        });
        TableColumn value = new TableColumn("Value");
        value.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Holding, String>, ObservableValue<String>>() {
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Holding, String> t) {
                return new SimpleStringProperty(NumberFormat.getCurrencyInstance().format(t.getValue().getValue()));
            }
        });
        port_table.getColumns().addAll(name, shares, ppshare, value);
        updateView();
    }

    /**
     * Reset the values of the components to match their original state - stored in "op"
     */
    public void resetView() {
        Portfolio temp = data.get(0).getYValue();
        data.clear();
        data.add(new XYChart.Data<>(Calendar.getInstance().getTime(), temp));
        updateView();
    }

    /**
     * Update the chart data
     */
    public void updateData() {
        ObservableList<XYChart.Data<Date, Portfolio>> temp = FXCollections.observableArrayList();
        temp.addAll(data);
        data.clear();
        data.add(new XYChart.Data<>(temp.get(0).getXValue(), op.clone()));
        for (int i = 1; i < temp.size(); i++)
            data.add(new XYChart.Data<>(temp.get(i).getXValue(), s.simulate(steps(), step_size(), temp.get(i - 1).getYValue().clone(), rate())));
        updateView();
    }

    public void showNewSim() {
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("UtOh!");
            alert.setHeaderText(null);
            alert.setContentText("Please select a portfolio to make a new simulation!!");
            alert.showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(null);
        alert.setTitle("Choose simulation type");
        ButtonType bull = new ButtonType("Bull");
        ButtonType bear = new ButtonType("Bear");
        ButtonType no_grow = new ButtonType("NoGrow");
        alert.getButtonTypes().setAll(bull, bear, no_grow);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == bull)
            controller.openSimulation(new Bull(), selected.getPortfolio().clone());
        else if (result.get() == bear)
            controller.openSimulation(new Bear(), selected.getPortfolio().clone());
        else if (result.get() == no_grow)
            controller.openSimulation(new NoGrowth(), selected.getPortfolio().clone());
    }

    /**
     * Reset the values of the components.
     */
    public void updateView() {
        ObservableList<XYChart.Series<Date, Number>> series = FXCollections.observableArrayList();
        ObservableList<XYChart.Data<Date, Number>> series1 = FXCollections.observableArrayList();
        for (XYChart.Data<Date, Portfolio> d : data) {
            XYChart.Data<Date, Number> node = new XYChart.Data<>(d.getXValue(), d.getYValue().eval());
            ClickablePortfolioNode point = new ClickablePortfolioNode(d.getYValue(), data.indexOf(d));
            point.setOnSelectedListener(this);
            node.setNode(point);
            series1.add(node);
        }
        series.add(new XYChart.Series<>("Portfolio Value", series1));
        chart.setData(series);
        if (selected != null)
            showPortfolioData(selected.getPortfolio());
    }

    /**
     * Set the table data
     *
     * @param portfolio the portfolio data to grab
     */
    private void showPortfolioData(Portfolio portfolio) {
        if (portfolio != null) {
            port_table.setItems(FXCollections.observableArrayList(portfolio.getHoldings()));
            total.setText(NumberFormat.getCurrencyInstance().format(portfolio.eval()));
        }
    }

    @Override
    public void handle(ClickablePortfolioNode node) {
        if (selected != null && selected != node)
            selected.deselect();
        selected = node;
        showPortfolioData(node.getPortfolio());
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof MainController) {
            this.op = diffPortfolio(((MainController) o).getPortfolio().clone());
            updateData();
        }
    }

    /**
     * Calculate the new price_per_share value for the Original Portfolio's equities
     *
     * @param p the new portfolio to update the price_per_shares of
     * @return the new portfolio
     */
    private Portfolio diffPortfolio(Portfolio p) {
        for (int i = 0; i < p.getHoldings().size(); i++) {
            if (p.getHoldings().get(i) instanceof Equity) {
                Equity pTemp = (Equity) p.getHoldings().get(i);
                Equity opTemp = (Equity) op.getHoldings().get(i);
                pTemp.setPrice_per_share(opTemp.getPrice_per_share() + (pTemp.getPrice_per_share() - pTemp.get_old_price()));
            }
        }
        return p;
    }
}

class ClickablePortfolioNode extends StackPane {
    private OnNodeSelectedListener listener;
    private Portfolio port;
    private Circle label;
    private int index;

    /**
     * Creates a new clickable node
     *
     * @param portfolio the portfolio for the node
     * @param index     the index of the node
     */
    public ClickablePortfolioNode(Portfolio portfolio, int index) {
        label = new Circle();
        label.setStroke(Color.GREEN);
        label.setFill(Color.LIGHTGREEN);
        label.setRadius(5f);
        this.index = index;
        this.port = portfolio;

        setOnMouseEntered(mouseEvent -> setCursor(Cursor.HAND));
        setOnMouseExited(mouseEvent -> setCursor(Cursor.CROSSHAIR));
        setOnMouseClicked(mouseEvent -> {
            if (label != null && !getChildren().contains(label))
                getChildren().add(label);
            listener.handle(this);
        });
    }

    /**
     * Get the index
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Get the portfolio
     *
     * @return the portfolio
     */
    public Portfolio getPortfolio() {
        return port;
    }

    /**
     * Set the listener for this node
     *
     * @param listener the listener
     */
    public void setOnSelectedListener(OnNodeSelectedListener listener) {
        this.listener = listener;
    }

    /**
     * Select this node
     */
    public void select() {
        if (label != null && !getChildren().contains(label))
            getChildren().add(label);
        listener.handle(this);
    }

    /**
     * deselect this node
     */
    public void deselect() {
        getChildren().clear();
    }
}

interface OnNodeSelectedListener {
    void handle(ClickablePortfolioNode node);
}