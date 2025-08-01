package com.marcosoft.storageSoftware.application.controller;

import com.marcosoft.storageSoftware.application.dto.SellDataTable;
import com.marcosoft.storageSoftware.application.dto.UserLogged;
import com.marcosoft.storageSoftware.domain.model.*;
import com.marcosoft.storageSoftware.domain.service.WarehouseService;
import com.marcosoft.storageSoftware.infrastructure.service.impl.*;
import com.marcosoft.storageSoftware.infrastructure.util.DisplayAlerts;
import com.marcosoft.storageSoftware.infrastructure.util.ParseDataTypes;
import com.marcosoft.storageSoftware.infrastructure.util.SceneSwitcher;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Controller for the sell view.
 * Handles logic for selling products, assigning prices, filtering inventory, and navigation.
 */
@Lazy
@Controller
public class SellViewController {
    private Client client;

    // Service and utility dependencies
    private final UserLogged userLogged;
    private final ParseDataTypes parseDataTypes;
    private final SceneSwitcher sceneSwitcher;
    private final InventoryServiceImpl inventoryService;
    private final ProductServiceImpl productService;
    private final WarehouseService warehouseService;
    private final DisplayAlerts displayAlerts;
    private final ClientServiceImpl clientService;
    private final CurrencyServiceImpl currencyService;
    private final SellRegistryServiceImpl sellRegistryService;
    private final GeneralRegistryServiceImpl generalRegistryService;

    /**
     * Constructor for dependency injection.
     */
    @Lazy
    public SellViewController(
            CurrencyServiceImpl currencyService, ClientServiceImpl clientService, DisplayAlerts displayAlerts,
            UserLogged userLogged, ParseDataTypes parseDataTypes, SceneSwitcher sceneSwitcher,
            InventoryServiceImpl inventoryService, ProductServiceImpl productService, WarehouseService warehouseService,
            SellRegistryServiceImpl sellRegistryService, GeneralRegistryServiceImpl generalRegistryService
    ) {
        this.inventoryService = inventoryService;
        this.generalRegistryService = generalRegistryService;
        this.sellRegistryService = sellRegistryService;
        this.displayAlerts = displayAlerts;
        this.clientService = clientService;
        this.sceneSwitcher = sceneSwitcher;
        this.currencyService = currencyService;
        this.warehouseService = warehouseService;
        this.productService = productService;
        this.parseDataTypes = parseDataTypes;
        this.userLogged = userLogged;
    }

    // FXML UI components
    @FXML
    private TextField tfSellProductCurrency, tfMinFilterAmount, tfMaxFilterAmount,
            tfAssignPriceProductPrice, tfSellProductAmount, tfFilterProductName, tfSellProductPrice,
            tfMinFilterPrice, tfSellProductName, tfMaxFilterPrice, tfFilterWarehouseName,
            tfAssignPriceCurrency, tfAssignPriceProductName, tfSellWarehouse;
    @FXML
    private MenuButton mbAssignPriceCurrency, mbSellCurrency, mbSellWarehouse, mbSellProduct;
    @FXML
    private Label txtClientName, txtAssignPriceDebug, txtSellDebug;
    @FXML
    private DatePicker dpSellProductDate;
    @FXML
    private Pagination paginator;
    @FXML
    private TreeTableView<SellDataTable> ttvInventory;
    @FXML
    private TreeTableColumn<SellDataTable, String> ttcWarehouse, ttcProductName;
    @FXML
    private TreeTableColumn<SellDataTable, Double> ttcSellPrice;
    @FXML
    private TreeTableColumn<SellDataTable, Integer> ttcProductAmount;

    private final ObservableList<SellDataTable> inventoryList = FXCollections.observableArrayList();

    /**
     * Initializes the controller after its root element has been completely processed.
     * Sets up table columns, loads inventory, listeners, date picker, warehouse and currency menus.
     */
    @FXML
    public void initialize() {
        txtClientName.setText(userLogged.getName());
        client = clientService.getClientByName(userLogged.getName());
        Platform.runLater(() -> {
            setupTableColumns();
            loadProductTable();
            setupFilterListeners();
            initDatePicker();
            initMbWarehouse();
            initAllMbCurrency();
        });
    }

    /**
     * Clears all filter fields for the inventory table.
     */
    @FXML
    public void cleanFilters(ActionEvent actionEvent) {
        tfFilterProductName.setText("");
        tfFilterWarehouseName.setText("");
        tfMinFilterAmount.setText(String.valueOf(0));
        tfMinFilterPrice.setText(String.valueOf(0.00));
        tfMaxFilterAmount.setText(String.valueOf(0));
        tfMaxFilterPrice.setText(String.valueOf(0.00));
    }

    /**
     * Clears all input fields in the sell and assign price forms.
     */
    @FXML
    public void cleanForm(ActionEvent actionEvent) {
        tfAssignPriceCurrency.clear();
        tfAssignPriceProductName.clear();
        tfAssignPriceProductPrice.clear();
        tfSellProductAmount.clear();
        tfSellProductCurrency.clear();
        tfSellProductName.clear();
        tfSellProductPrice.clear();
        dpSellProductDate.setValue(LocalDate.now());
    }

    /**
     * Handles the selling of a product.
     * Validates input fields and updates inventory.
     */
    @FXML
    public void sellProduct(ActionEvent actionEvent) {
        if (!validateAllSellFields()) {
            return;
        }

        String warehouseName = tfSellWarehouse.getText();
        String productName = tfSellProductName.getText();
        Integer productAmount = parseDataTypes.parseInt(tfSellProductAmount.getText());
        Double productSellPrice = parseDataTypes.parseDouble(tfSellProductPrice.getText());
        String currencyName = tfSellProductCurrency.getText();
        LocalDate date = dpSellProductDate.getValue();

        try {
            Product product = productService.getByProductNameAndClient(productName, client);
            Warehouse warehouse = warehouseService.getWarehouseByWarehouseNameAndClient(warehouseName, client);
            Inventory inventory = inventoryService.getByProductAndWarehouseAndClient(product, warehouse, client);
            inventory.setAmount(inventory.getAmount() - productAmount);
            inventoryService.save(inventory);

            // TODO: Implement database logic for saving the sale record
            SellRegistry sellRegistry = new SellRegistry(
                    null, client, "Venta", LocalDateTime.now(), productName,
                    currencyName, productSellPrice, date, warehouseName, productAmount
            );
            sellRegistryService.save(sellRegistry);

            GeneralRegistry generalRegistry = new GeneralRegistry(
                    null, client, "Ventas", "Venta", LocalDateTime.now()
            );
            generalRegistryService.save(generalRegistry);
        } catch (Exception e) {
            displayAlerts.showAlert("Ha ocurrido un error: " + e.getMessage());
        }
    }

    /**
     * Handles the assignment of a price to a product.
     * Validates input fields and updates product price.
     */
    @FXML
    public void assignProductPrice(ActionEvent actionEvent) {
        if (!validateAllAssignPriceFields()) {
            return;
        }

        String productName = tfAssignPriceProductName.getText();
        Double productPrice = parseDataTypes.parseDouble(tfAssignPriceProductPrice.getText());
        String productCurrency = tfAssignPriceCurrency.getText();

        try {
            Currency currency;
            if (!currencyService.existsByCurrencyName(productCurrency)) {
                currency = new Currency(null, productCurrency, client);
            } else {
                currency = currencyService.getCurrencyByName(productCurrency);
            }

            Product product = productService.getByProductNameAndClient(productName, client);
            product.setSellPrice(productPrice);
            product.setCurrency(currency);
            productService.save(product);

            GeneralRegistry generalRegistry = new GeneralRegistry(
                    null, client, "Ventas", "Asignación Precio", LocalDateTime.now()
            );
            generalRegistryService.save(generalRegistry);

            displayAlerts.showAlert("El precio del producto ha sido guardado satisfactoriamente");
            cleanForm(null);
        } catch (Exception e) {
            displayAlerts.showAlert("Ha ocurrido un error: " + e.getMessage());
        }
    }

    /**
     * Validates all fields in the assign price form.
     */
    private boolean validateAllAssignPriceFields() {
        return validateTfAssignProduct() && validateTfPriceAssign() && validateTfAssignCurrency();
    }

    // ============================
    // VALIDATION METHODS FOR ASSIGN PRICE FORM
    // ============================

    /**
     * Validates the currency field in the assign price form.
     */
    private boolean validateTfAssignCurrency() {
        String currencyName = tfAssignPriceCurrency.getText();

        if (currencyName == null || currencyName.isEmpty()) {
            displayAlerts.showAlert("Debe seleccionar una moneda");
            return false;
        }

        if (!currencyName.matches("[a-zA-Z]+")) {
            displayAlerts.showAlert("La moneda solo puede contener letras");
            return false;
        }

        return true;
    }

    /**
     * Validates the product field in the assign price form.
     * Shows alerts in Spanish if validation fails.
     */
    private boolean validateTfAssignProduct() {
        String productName = tfAssignPriceProductName.getText();

        if (productName == null || productName.isEmpty()) {
            displayAlerts.showAlert("Debe asignar un producto");
            return false;
        }

        Product product = productService.getByProductNameAndClient(productName, client);
        if (product == null) {
            displayAlerts.showAlert("El producto no existe en la base de datos");
            return false;
        }

        return true;
    }

    /**
     * Validates the price field in the assign price form.
     * Shows alerts in Spanish if validation fails.
     */
    private boolean validateTfPriceAssign() {
        String priceText = tfAssignPriceProductPrice.getText();

        if (priceText == null || priceText.isEmpty()) {
            displayAlerts.showAlert("El precio no debe estar vacío");
            return false;
        }

        try {
            double price = Double.parseDouble(priceText);

            if (price <= 0) {
                displayAlerts.showAlert("El precio debe ser mayor que 0");
                return false;
            }

            if (priceText.split("\\.")[1].length() > 2) {
                displayAlerts.showAlert("El precio solo puede tener 2 lugar decimales");
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            displayAlerts.showAlert("El precio debe ser un número válido");
            return false;
        }
    }

    /**
     * Validates all fields in the sell form.
     * Shows alerts in Spanish if validation fails.
     */
    private boolean validateAllSellFields() {
        return validateTfSellProduct() && validateTfSellAmount() && validateSellPrice() && validateSellCurrency() && validateDatePicker();
    }

    // ============================
    // VALIDATION METHODS FOR SELL FORM
    // ============================

    /**
     * Validates the currency field in the sell form.
     * Shows alerts in Spanish if validation fails.
     */
    private boolean validateSellCurrency() {
        String currencyName = tfSellProductCurrency.getText();

        if (currencyName == null || currencyName.isEmpty()) {
            displayAlerts.showAlert("Debe seleccionar una moneda");
            return false;
        }

        if (!currencyService.existsByCurrencyName(currencyName)) {
            displayAlerts.showAlert("La moneda seleccionada no existe en la base de datos");
            return false;
        }

        return true;
    }

    /**
     * Validates the price field in the sell form.
     * Shows alerts in Spanish if validation fails.
     */
    private boolean validateSellPrice() {
        String priceText = tfSellProductPrice.getText();

        if (priceText == null || priceText.isEmpty()) {
            displayAlerts.showAlert("El precio no puede estar vacío");
            return false;
        }

        try {
            double price = Double.parseDouble(priceText);

            if (price <= 0) {
                displayAlerts.showAlert("El precio debe ser mayor que cero");
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            displayAlerts.showAlert("El precio debe ser un número válido");
            return false;
        }
    }

    /**
     * Validates the date picker field in the sell form.
     * Shows alerts in Spanish if validation fails.
     */
    private boolean validateDatePicker() {
        LocalDate selectedDate = dpSellProductDate.getValue();

        if (selectedDate == null) {
            displayAlerts.showAlert("Por favor selecciona una fecha válida");
            return false;
        }

        if (selectedDate.isAfter(LocalDate.now())) {
            displayAlerts.showAlert("La fecha de venta no puede ser en el futuro");
            return false;
        }

        return true;
    }

    /**
     * Validates the amount field in the sell form.
     * Shows alerts in Spanish if validation fails.
     */
    private boolean validateTfSellAmount() {
        String amountText = tfSellProductAmount.getText();

        if (amountText == null || amountText.isEmpty()) {
            displayAlerts.showAlert("La cantidad no pueda estar vacía");
            return false;
        }

        try {
            int amount = Integer.parseInt(amountText);

            if (amount <= 0) {
                displayAlerts.showAlert("La cantidad debe ser mayor que cero");
                return false;
            }

            if (validateTfSellProduct() && !tfSellWarehouse.getText().isEmpty()) {
                Product product = productService.getByProductNameAndClient(tfSellProductName.getText(), client);
                Warehouse warehouse = warehouseService.getWarehouseByWarehouseNameAndClient(tfSellWarehouse.getText(), client);
                Inventory inventory = inventoryService.getByProductAndWarehouseAndClient(product, warehouse, client);

                if (inventory.getAmount() < amount) {
                    displayAlerts.showAlert("No hay suficiente existencias disponibles");
                    return false;
                }
            }

            return true;
        } catch (NumberFormatException e) {
            displayAlerts.showAlert("La cantidad debe ser un número válido");
            return false;
        }
    }

    /**
     * Validates the product and warehouse fields in the sell form.
     * Shows alerts in Spanish if validation fails.
     */
    private boolean validateTfSellProduct() {
        String productName = tfSellProductName.getText();
        String warehouseName = tfSellWarehouse.getText();

        if (productName == null || productName.isEmpty()) {
            displayAlerts.showAlert("Debe seleccionar un producto");
            return false;
        }

        if (warehouseName == null || warehouseName.isEmpty()) {
            displayAlerts.showAlert("Debe seleccionar un almacén");
            return false;
        }

        Product product = productService.getByProductNameAndClient(productName, client);
        if (product == null) {
            displayAlerts.showAlert("El producto no existe");
            return false;
        }

        Warehouse warehouse = warehouseService.getWarehouseByWarehouseNameAndClient(warehouseName, client);
        if (warehouse == null) {
            displayAlerts.showAlert("El almacén no existe");
            return false;
        }

        if (!inventoryService.existsByProductAndWarehouseAndClient(product, warehouse, client)) {
            displayAlerts.showAlert("El producto no está disponible en el almacén seleccionado");
            return false;
        }

        return true;
    }

    // ============================
    // NAVIGATION METHODS
    // ============================

    /**
     * Navigates to the configuration view.
     */
    @FXML
    public void switchToConfiguration(ActionEvent actionEvent) {
        sceneSwitcher.switchView(actionEvent, "/configurationView.fxml");
    }

    /**
     * Navigates to the support view.
     */
    @FXML
    public void switchToSupport(ActionEvent actionEvent) {
        sceneSwitcher.switchView(actionEvent, "/supportView.fxml");
    }

    /**
     * Navigates to the registry view.
     */
    @FXML
    public void switchToRegistry(ActionEvent actionEvent) {
        sceneSwitcher.switchView(actionEvent, "/registryView.fxml");
    }

    /**
     * Navigates to the warehouse view.
     */
    @FXML
    public void switchToWarehouse(ActionEvent actionEvent) {
        sceneSwitcher.switchView(actionEvent, "/warehouseView.fxml");
    }

    /**
     * Navigates to the investment view.
     */
    @FXML
    public void switchToInvestment(ActionEvent actionEvent) {
        sceneSwitcher.switchView(actionEvent, "/investmentView.fxml");
    }

    /**
     * Navigates to the balance view.
     */
    @FXML
    public void switchToBalance(ActionEvent actionEvent) {
        sceneSwitcher.switchView(actionEvent, "/balanceView.fxml");
    }

    // ============================
    // UTILITIES
    // ============================

    /**
     * Sets up the columns for the inventory table.
     */
    private void setupTableColumns() {
        ttcWarehouse.setCellValueFactory(new TreeItemPropertyValueFactory<>("warehouseName"));
        ttcProductName.setCellValueFactory(new TreeItemPropertyValueFactory<>("productName"));
        ttcSellPrice.setCellValueFactory(new TreeItemPropertyValueFactory<>("sellPrice"));
        ttcProductAmount.setCellValueFactory(new TreeItemPropertyValueFactory<>("productAmount"));
    }

    /**
     * Loads inventory data and populates the product table.
     */
    private void loadProductTable() {
        inventoryList.clear();
        List<Inventory> inventories = inventoryService.getAllInventories();
        List<SellDataTable> inventoriesObs = new ArrayList<>();
        for (Inventory i : inventories) {
            SellDataTable sell = new SellDataTable(i.getProduct().getProductName(),
                    i.getProduct().getSellPrice(), i.getWarehouse().getWarehouseName(),
                    i.getAmount());
            inventoriesObs.add(sell);
        }
        inventoryList.addAll(inventoriesObs);

        TreeItem<SellDataTable> root = new TreeItem<>();
        for (SellDataTable item : inventoryList) {
            root.getChildren().add(new TreeItem<>(item));
        }
        ttvInventory.setRoot(root);
        ttvInventory.setShowRoot(false);
        filterProductTable();
    }

    /**
     * Sets up listeners for filter fields to trigger table filtering.
     */
    private void setupFilterListeners() {
        tfFilterProductName.textProperty().addListener((obs, oldVal, newVal) -> filterProductTable());
        tfFilterWarehouseName.textProperty().addListener((obs, oldVal, newVal) -> filterProductTable());
        tfMinFilterAmount.textProperty().addListener((obs, oldVal, newVal) -> filterProductTable());
        tfMaxFilterAmount.textProperty().addListener((obs, oldVal, newVal) -> filterProductTable());
        tfMinFilterPrice.textProperty().addListener((obs, oldVal, newVal) -> filterProductTable());
        tfMaxFilterPrice.textProperty().addListener((obs, oldVal, newVal) -> filterProductTable());
    }

    /**
     * Filters the product table based on filter field values.
     */
    private void filterProductTable() {
        String productName = tfFilterProductName.getText().trim().toLowerCase();
        String warehouseName = tfFilterWarehouseName.getText().trim().toLowerCase();
        Integer minAmount = parseDataTypes.parseInt(tfMinFilterAmount.getText());
        Integer maxAmount = parseDataTypes.parseInt(tfMaxFilterAmount.getText());
        Double minPrice = parseDataTypes.parseDouble(tfMinFilterPrice.getText());
        Double maxPrice = parseDataTypes.parseDouble(tfMaxFilterPrice.getText());

        Predicate<SellDataTable> filter = product -> {
            boolean matches = true;
            if (!productName.isEmpty())
                matches &= product.getProductName() != null && product.getProductName().toLowerCase().contains(productName);
            if (!warehouseName.isEmpty())
                matches &= product.getWarehouseName() != null && product.getWarehouseName().toLowerCase().contains(warehouseName);
            if (minAmount != null && minAmount > 0)
                matches &= product.getProductAmount() != null && product.getProductAmount() >= minAmount;
            if (maxAmount != null && maxAmount > 0)
                matches &= product.getProductAmount() != null && product.getProductAmount() <= maxAmount;
            if (minPrice != null && minPrice > 0.0)
                matches &= product.getSellPrice() != null && product.getSellPrice() >= minPrice;
            if (maxPrice != null && maxPrice > 0.0)
                matches &= product.getSellPrice() != null && product.getSellPrice() <= maxPrice;
            return matches;
        };

        List<SellDataTable> filtered = inventoryList.stream()
                .filter(filter)
                .toList();

        TreeItem<SellDataTable> root = new TreeItem<>();
        for (SellDataTable item : filtered) {
            root.getChildren().add(new TreeItem<>(item));
        }
        ttvInventory.setRoot(root);
        ttvInventory.setShowRoot(false);
    }

    /**
     * Initializes the date picker with the current date.
     */
    private void initDatePicker() {
        dpSellProductDate.setValue(LocalDate.now());
    }

    /**
     * Initializes the warehouse menu with all warehouses for the current client.
     */
    private void initMbWarehouse() {
        mbSellWarehouse.getItems().clear();
        List<Warehouse> warehouses = warehouseService.getWarehousesByClient(clientService.getClientByName(userLogged.getName()));

        for (Warehouse w : warehouses) {
            MenuItem item = new MenuItem(w.getWarehouseName());
            item.setOnAction(e_ -> {
                tfSellWarehouse.setText(item.getText());
                initMbSellProduct(w);
            });
            mbSellWarehouse.getItems().add(item);
        }
    }

    /**
     * Initializes the product menu for the selected warehouse.
     */
    private void initMbSellProduct(Warehouse w) {
        if (w == null || w.getWarehouseName() == null) return;

        mbSellProduct.getItems().clear();
        List<Inventory> inventory = inventoryService.getAllInventoriesByWarehouseAndClient(w, client);

        if (inventory.isEmpty()) {
            MenuItem item = new MenuItem("No hay productos disponibles");
            item.setDisable(true);
            mbSellProduct.getItems().add(item);
            return;
        }

        for (Inventory i : inventory) {
            if (i.getProduct() != null && i.getProduct().getProductName() != null) {
                MenuItem item = new MenuItem(i.getProduct().getProductName());
                item.setOnAction(e -> tfSellProductName.setText(item.getText()));
                mbSellProduct.getItems().add(item);
            }
        }
    }

    /**
     * Initializes both currency menus for selling and assigning price.
     */
    private void initAllMbCurrency() {
        initMbCurrency(mbSellCurrency, tfSellProductCurrency);
        initMbCurrency(mbAssignPriceCurrency, tfAssignPriceCurrency);
    }

    /**
     * Initializes a currency menu with all available currencies.
     */
    private void initMbCurrency(MenuButton mb, TextField tf) {
        mb.getItems().clear();
        List<Currency> currencies = currencyService.getAllCurrencies();

        for (Currency currency : currencies) {
            MenuItem item = new MenuItem(currency.getCurrencyName());
            item.setOnAction(e -> {
                tf.setText(item.getText());
            });
            mb.getItems().add(item);
        }
    }
}
