package controller;

import javafx.collections.FXCollections;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import model.*;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

 /**
 * OrderController
 * 
 * Manages the pharmaceutical order workflow: loads available medications, 
 * handles search/filtering, manages medication cart, processes prescriptions,
 * updates inventory stock, and generates receipts.
 */
public class OrderController implements Initializable {

    // Navigation buttons
    @FXML private Button dashboardButton;
    @FXML private Button inventoryButton;
    @FXML private Button orderButton;
    @FXML private Button recentOrderButton;
    @FXML private Button logoutButton;

    // Medication Search and Filter Section
    @FXML private TextField medicationSearchField;
    @FXML private ComboBox<String> medicationCategoryFilter;
    @FXML private Button clearFilterButton;

    // Prescription Summary Section
    @FXML private TextField patientNameField;
    @FXML private ComboBox<String> paymentMethodComboBox;
    @FXML private TextField prescriptionTotalField;
    @FXML private Button processPrescriptionButton;
    @FXML private Button clearCartButton;

    // Medication Cards Container
    @FXML private ScrollPane medicationScrollPane;
    @FXML private FlowPane medicationCardsContainer;

    // Prescription Cart Table
    @FXML private TableView<OrderItem> prescriptionCartTable;
    @FXML private TableColumn<OrderItem, String> cartMedicationColumn;
    @FXML private TableColumn<OrderItem, Double> cartPriceColumn;
    @FXML private TableColumn<OrderItem, Integer> cartQuantityColumn;
    @FXML private TableColumn<OrderItem, Double> cartTotalColumn;
    @FXML private TableColumn<OrderItem, Button> cartActionColumn;

    // Data collections
    private ObservableList<Product> availableMedications = FXCollections.observableArrayList();
    private ObservableList<OrderItem> prescriptionCart = FXCollections.observableArrayList();
    private DecimalFormat currencyFormat = new DecimalFormat("#0.00");

    
     //Initializes the UI and data bindings for the Order screen.
     //Sets defaults, wires combo boxes and table, loads products, and prepares events
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set default patient name for walk-in customers
        patientNameField.setText("Walk-in Customer");
        
        initializeComboBoxes();
        initializePrescriptionCartTable();
        loadAvailableMedications();
        setupEventHandlers();
        clearPrescriptionForm();
    }
    
     
      //Persists the current cart as an order and its items, and updates inventory stock
      //Uses a transaction and rolls back on failure
     
    private boolean placeOrder() {
        Connection connection = null;
        PreparedStatement orderStmt = null;
        PreparedStatement orderItemsStmt = null;
        
        try {
            connection = SqliteConnection.Connector();
            connection.setAutoCommit(false);
            
            LocalDateTime now = LocalDateTime.now();
            String orderDate = now.toLocalDate().toString();
            String orderTime = now.toLocalTime().toString();
            
            String orderId = OrderIdGenerator.generateOrderId();
            
            String orderSql = "INSERT INTO orders (order_id, customer_name, payment_method, order_status, total_amount, order_date, order_time) VALUES (?, ?, ?, ?, ?, ?, ?)";
            orderStmt = connection.prepareStatement(orderSql);
            
            orderStmt.setString(1, orderId);
            orderStmt.setString(2, patientNameField.getText().trim());
            orderStmt.setString(3, paymentMethodComboBox.getValue());
            orderStmt.setString(4, "Completed");
            orderStmt.setDouble(5, prescriptionCart.stream().mapToDouble(OrderItem::getTotalPrice).sum());
            orderStmt.setString(6, orderDate);
            orderStmt.setString(7, orderTime);
            
            orderStmt.executeUpdate();
            
            String orderItemsSql = "INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price, total_price) VALUES (?, ?, ?, ?, ?, ?)";
            orderItemsStmt = connection.prepareStatement(orderItemsSql);
            
            String updateStockSql = "UPDATE meds_product SET stock = stock - ? WHERE product_id = ?";
            
            try (PreparedStatement updateStockStmt = connection.prepareStatement(updateStockSql)) {
                for (OrderItem item : prescriptionCart) {
                    // Insert order item
                    orderItemsStmt.setString(1, orderId);
                    orderItemsStmt.setInt(2, item.getProductId());
                    orderItemsStmt.setString(3, item.getProductName());
                    orderItemsStmt.setInt(4, item.getQuantity());
                    orderItemsStmt.setDouble(5, item.getUnitPrice());
                    orderItemsStmt.setDouble(6, item.getTotalPrice());
                    orderItemsStmt.executeUpdate();
                    
                    // Update medication stock
                    updateStockStmt.setInt(1, item.getQuantity());
                    updateStockStmt.setInt(2, item.getProductId());
                    updateStockStmt.executeUpdate();
                }
            }
            
            connection.commit();
            return true;
            
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to process prescription: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (orderStmt != null) orderStmt.close();
                if (orderItemsStmt != null) orderItemsStmt.close();
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Populates filter/order/payment combo boxes with defaults
    private void initializeComboBoxes() {
    	// Initialize product category filter
        medicationCategoryFilter.getItems().addAll(
        		"Pain Reliever",
        		"Fever Reducer",
        		"NSAIDs",
        		"Antibiotic",
        		"Antifungal",
        		"Antiviral",
        		"Cough Expectorant",
        		"Cough Suppressant",
        		"Cold & Flu",
        		"Allergy",
        		"Stomach Care",
        		"Antacid",
        		"Anti-diarrheal",
        		"Laxative",
        		"Anti-emetic",
        		"Vitamin C",
        		"Multivitamin",
        		"B-Complex",
        		"Minerals",
        		"Supplements",
        		"Herbal Medicine",
        		"Skin Ointment",
        		"Antifungal Cream",
        		"Steroid Cream",
        		"Eye Care",
        		"Ear Care",
        		"First Aid",
        		"Alcohol / Disinfectant",
        		"Baby Care",
        		"Women's Health",
        		"Hypertension Meds",
        		"Diabetes Meds",
        		"Cholesterol Meds",
        		"Respiratory / Asthma",
        		"Urinary Care",
        		"Mental Health",
        		"Oral Rehydration Solution"

        );
        medicationCategoryFilter.setValue("All Categories");

        // Initialize payment method
        paymentMethodComboBox.getItems().addAll("Cash", "Card", "GCash", "Gothyme");
        paymentMethodComboBox.setValue("Cash");
    }

    
     //Configures the shopping cart table columns, formatting, and the remove action
     
    @SuppressWarnings("unused")
    private void initializePrescriptionCartTable() {
        System.out.println("Initializing prescription cart table...");
        
        // Initialize cart table columns with proper property binding
        cartMedicationColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getProductName()));
        cartPriceColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getUnitPrice()).asObject());
        cartQuantityColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getQuantity()).asObject());
        cartTotalColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getTotalPrice()).asObject());

        // Format price columns in cart
        cartPriceColumn.setCellFactory(column -> new TableCell<OrderItem, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText("₱" + currencyFormat.format(price));
                }
            }
        });

        cartTotalColumn.setCellFactory(column -> new TableCell<OrderItem, Double>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                if (empty || total == null) {
                    setText(null);
                } else {
                    setText("₱" + currencyFormat.format(total));
                }
            }
        });

        // Add remove button in action column
        cartActionColumn.setCellFactory(column -> new TableCell<OrderItem, Button>() {
            private final Button removeButton = new Button("Remove");
            
            {
                removeButton.setStyle("-fx-background-color: linear-gradient(to bottom, #EF5350, #D32F2F); " +
                                    "-fx-background-radius: 8; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 12px;");
                removeButton.setPrefWidth(80);
                removeButton.setOnAction(event -> {
                    OrderItem item = getTableView().getItems().get(getIndex());
                    removeFromCart(item);
                });
            }

            @Override
            protected void updateItem(Button button, boolean empty) {
                super.updateItem(button, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(removeButton);
                }
            }
        });

        // Set the data source for the table
        prescriptionCartTable.setItems(prescriptionCart);
        
        // Enable table selection
        prescriptionCartTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
        System.out.println("Prescription cart table initialized successfully");
    }

    // Registers listeners for medication search, category filter, and cart changes
    @SuppressWarnings("unused")
    private void setupEventHandlers() {
        // Medication search functionality
        medicationSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterMedications();
        });

        // Category filter functionality
        medicationCategoryFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            filterMedications();
        });

        // Prescription cart change listener to update total
        prescriptionCart.addListener((javafx.collections.ListChangeListener.Change<? extends OrderItem> change) -> {
            updatePrescriptionTotal();
        });
    }

    // Loads only available, in-stock products from DB and renders product cards
    private void loadAvailableMedications() {
        availableMedications.clear();
        Connection connection = null;
        
        try {
            connection = SqliteConnection.Connector();
            String query = "SELECT * FROM meds_product WHERE status = 'Available' AND stock > 0";
            
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                // Handle date_added as TEXT instead of TIMESTAMP
                String dateAddedString = resultSet.getString("date_added");
                LocalDateTime dateAdded;
                
                try {
                    // Try to parse as LocalDate first, then convert to LocalDateTime
                    if (dateAddedString != null && !dateAddedString.isEmpty()) {
                        if (dateAddedString.contains("T") || dateAddedString.contains(" ")) {
                            // Full datetime string
                            dateAdded = LocalDateTime.parse(dateAddedString.replace(" ", "T"));
                        } else {
                            // Just date string, add time
                            dateAdded = LocalDate.parse(dateAddedString).atStartOfDay();
                        }
                    } else {
                        dateAdded = LocalDateTime.now();
                    }
                } catch (Exception e) {
                    // If parsing fails, use current time
                    dateAdded = LocalDateTime.now();
                }
                
                Product product = new Product(
                    resultSet.getInt("product_id"),
                    resultSet.getString("name"),
                    resultSet.getString("category"),
                    resultSet.getDouble("price"),
                    resultSet.getInt("stock"),
                    resultSet.getString("status"),
                    resultSet.getString("image_path"),
                    dateAdded
                );
                availableMedications.add(product);
            }
            
            // Load product cards into FlowPane
            loadProductCards();
            
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Error loading products: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // load the product card UI for the current product list.
    private void loadProductCards() {
        medicationCardsContainer.getChildren().clear();
        
        medicationCardsContainer.setHgap(15); 
        medicationCardsContainer.setVgap(15); 
        
        for (Product product : availableMedications) {
            // Show all pharmacy categories
            if (shouldDisplayProduct(product)) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/fxml/ProductCard.fxml"));
                    Node productCard = loader.load();
                    
                    ProductCardController cardController = loader.getController();
                    cardController.setProduct(product);
                    cardController.setOrderController(this);
                    
                    if (productCard instanceof javafx.scene.layout.Region) {
                        javafx.scene.layout.Region cardRegion = (javafx.scene.layout.Region) productCard;
                        cardRegion.setPrefSize(230, 330); 
                        cardRegion.setMaxSize(230, 330);
                        cardRegion.setMinSize(230, 330);
                    }
                    
                    medicationCardsContainer.getChildren().add(productCard);
                    
                } catch (IOException e) {
                    System.err.println("Error loading product card: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        System.out.println("Loaded " + medicationCardsContainer.getChildren().size() + " product cards");
    }
    
    // Returns whether a product should be shown on the Order page
    private boolean shouldDisplayProduct(Product product) {
        String category = product.getCategory();
        // Show all pharmacy categories - no need to filter specific categories
        return category != null && !category.trim().isEmpty();
    }

    // Applies search text and category filter to the visible product cards
    private void filterMedications() {
        String searchText = medicationSearchField.getText().toLowerCase();
        String selectedCategory = medicationCategoryFilter.getValue();
        
        medicationCardsContainer.getChildren().clear();
        
        medicationCardsContainer.setHgap(15); 
        medicationCardsContainer.setVgap(15); 
        
        for (Product product : availableMedications) {
            // Show all pharmacy categories
            if (shouldDisplayProduct(product)) {
                boolean matchesSearch = searchText.isEmpty() || 
                                      product.getName().toLowerCase().contains(searchText) ||
                                      product.getCategory().toLowerCase().contains(searchText);
                
                boolean matchesCategory = "All Categories".equals(selectedCategory) || 
                                        product.getCategory().equals(selectedCategory);
                
                if (matchesSearch && matchesCategory) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/fxml/ProductCard.fxml"));
                        Node productCard = loader.load();
                        
                        ProductCardController cardController = loader.getController();
                        cardController.setProduct(product);
                        cardController.setOrderController(this);
                        
                        // Use consistent sizing with loadProductCards
                        if (productCard instanceof javafx.scene.layout.Region) {
                            javafx.scene.layout.Region cardRegion = (javafx.scene.layout.Region) productCard;
                            cardRegion.setPrefSize(230, 330);
                            cardRegion.setMaxSize(230, 330);
                            cardRegion.setMinSize(230, 330);
                        }
                        
                        medicationCardsContainer.getChildren().add(productCard);
                        
                    } catch (IOException e) {
                        System.err.println("Error loading product card: " + e.getMessage());
                    }
                }
            }
        }
        
        System.out.println("Filtered " + medicationCardsContainer.getChildren().size() + " product cards");
    }

    // Adds a product to the cart
    public void addProductToCart(Product product, int quantity) {
        // Check if product already exists in cart
        for (OrderItem item : prescriptionCart) {
            if (item.getProductId() == product.getId()) {
                // Update quantity
                item.setQuantity(item.getQuantity() + quantity);
                item.setTotalPrice(item.getQuantity() * item.getUnitPrice());
                prescriptionCartTable.refresh();
                return;
            }
        }
        
        // Add new item to cart
        OrderItem newItem = new OrderItem(
            0, // orderId will be set when order is placed
            product.getId(),
            product.getName(),
            quantity,
            product.getPrice(),
            quantity * product.getPrice()
        );
        
        prescriptionCart.add(newItem);
    }

    // Removes an item from the shopping cart 
    private void removeFromCart(OrderItem item) {
        prescriptionCart.remove(item);
    }

    // cart total and updates the summary field
    private void updatePrescriptionTotal() {
        double total = prescriptionCart.stream()
                                 .mapToDouble(OrderItem::getTotalPrice)
                                 .sum();
        prescriptionTotalField.setText("₱" + currencyFormat.format(total));
    }

    // Clears customer info, cart items, and resets selectors to defaults
    private void clearPrescriptionForm() {
        patientNameField.setText("None"); // Set default value to "None"
        prescriptionCart.clear();
        prescriptionTotalField.setText("₱0.00");
        paymentMethodComboBox.setValue("Cash");
    }

    // Actions
    // Validates and places the order; clears the cart on success and generates receipt
    @FXML
    private void handlePlaceOrder(ActionEvent event) {
        if (!prescriptionCart.isEmpty()) {
            // Ensure customer name is set to "None" if empty
            if (patientNameField.getText().trim().isEmpty()) {
                patientNameField.setText("None");
            }
            
            if (placeOrder()) {
                generateReceiptAfterOrder(event);
                
                showAlert(Alert.AlertType.INFORMATION, "Success", "Order placed successfully!");
                clearPrescriptionForm();
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "Place Order First", "Please add items to cart before placing order.");
        }
    }

    @FXML
    private void handleClearFilter(ActionEvent event) {
        medicationSearchField.clear();
        medicationCategoryFilter.setValue("All Categories");
        filterMedications();
    }

    @FXML
    private void handleClearCart(ActionEvent event) {
        if (!prescriptionCart.isEmpty()) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Clear Cart");
            confirmAlert.setHeaderText("Are you sure you want to clear the cart?");
            confirmAlert.setContentText("All items will be removed from your cart.");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                prescriptionCart.clear();
                showAlert(Alert.AlertType.INFORMATION, "Cart Cleared", "All items have been removed from your cart.");
            }
        }
    }

    @FXML
    private void handlePrintReceipt(ActionEvent event) {
        if (prescriptionCart.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Items", "Please add items to cart before printing receipt.");
            return;
        }

        String customerName = patientNameField.getText().trim();
        if (customerName.isEmpty()) {
            customerName = "None";
            patientNameField.setText(customerName);
        }

        try {
            String orderId = OrderIdGenerator.generateOrderId();
            
            String paymentMethod = paymentMethodComboBox.getValue();
            double totalAmount = prescriptionCart.stream().mapToDouble(OrderItem::getTotalPrice).sum();
            
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            List<OrderItem> orderItems = new ArrayList<>(prescriptionCart);
            
            // Generate receipt using ReceiptGenerator 
            boolean success = ReceiptGenerator.generateReceipt(
                currentStage, 
                orderId,
                customerName,
                paymentMethod, 
                totalAmount, 
                orderItems
            );
            
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Receipt", "Receipt generated and saved successfully!");
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not generate receipt: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Generates receipt automatically after order is placed
    private void generateReceiptAfterOrder(ActionEvent event) {
        try {
            // Generate unique order ID using OrderIdGenerator
            String orderId = OrderIdGenerator.generateOrderId();
            
            String customerName = patientNameField.getText().trim();
            if (customerName.isEmpty()) {
                customerName = "None";
            }
            
            String paymentMethod = paymentMethodComboBox.getValue();
            
            double totalAmount = prescriptionCart.stream().mapToDouble(OrderItem::getTotalPrice).sum();
            
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            List<OrderItem> orderItems = new ArrayList<>(prescriptionCart);
            
            // Generate receipt using ReceiptGenerator (
            boolean success = ReceiptGenerator.generateReceipt(
                currentStage, 
                orderId,
                customerName,
                paymentMethod, 
                totalAmount, 
                orderItems
            );
            
            if (success) {
                System.out.println("Receipt generated automatically after order placement");
            }

        } catch (Exception e) {
            // Don't show error alert here since order was already successful
            // Just log the error
            System.err.println("Could not generate receipt automatically: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Navigation methods
    
    //Inventory page
    @FXML
    private void handleInventoryButton(ActionEvent event) {
        try {
            loadScene(event, "/view/fxml/Inventory.fxml");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load Inventory page: " + e.getMessage());
        }
    }

    // Refreshes Order page
    @FXML
    private void handleOrderButton(ActionEvent event) {
        // Already on order page
        loadAvailableMedications();
    }



    // Confirms and logs out to the Login page
    @FXML
    private void handleLogoutButton(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout Confirmation");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You will be redirected to the login page.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                loadScene(event, "/view/fxml/LoginPage.fxml");
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Could not load Login page: " + e.getMessage());
            }
        }
    }

    // Utility methods
    private void loadScene(ActionEvent event, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setTitle("Health Point System");
        stage.setScene(scene);
        stage.show();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
