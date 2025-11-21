package controller;

import javafx.application.Platform;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Product;
import model.SqliteConnection;
import model.InventoryIdGenerator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

//InventoryController

//Controls the Inventory screen: loads products, formats the table, and
//handles add/update/delete actions, image selection, and page navigation.
 
public class InventoryController implements Initializable {
    
    // Navigation buttons
    @FXML private Button dashboardbutton;
    @FXML private Button inventorybutton;
    @FXML private Button orderbutton;
    @FXML private Button recentorderbutton;
    @FXML private Button logoutbutton;
    @FXML private Button refreshButton;
    
    // Form fields
    @FXML private TextField productIdField;
    @FXML private TextField productNameField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextField priceField;
    @FXML private TextField stockField;
    @FXML private ComboBox<String> statusComboBox;
    
    // Action buttons
    @FXML private Button addButton;
    @FXML private Button clearButton;
    @FXML private Button selectImageButton;
    @FXML private Button removeImageButton;
    
    // Image view
    @FXML private ImageView productImageView;
    
    // Table and columns
    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, Integer> idColumn;
    @FXML private TableColumn<Product, String> imageColumn;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, Double> priceColumn;
    @FXML private TableColumn<Product, Integer> stockColumn;
    @FXML private TableColumn<Product, String> statusColumn;
    @FXML private TableColumn<Product, String> dateColumn;
    @FXML private TableColumn<Product, Void> actionsColumn;
    
    // Database connection
    private Connection connection;
    
    // Data
    private ObservableList<Product> productsList = FXCollections.observableArrayList();
    private DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
    private String selectedImagePath = "";
    private Product selectedProduct = null;
    
    /*
     * Connects to DB, prepares combo boxes/table, loads products, and sets window behavior.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize database connection
        connection = SqliteConnection.Connector();
        
        // Setup combo boxes
        setupComboBoxes();
        
        // Setup table
        setupTable();
        
        // Load products
        loadProducts();
        
        // Setup window
        Platform.runLater(() -> {
            Stage stage = (Stage) inventorybutton.getScene().getWindow();
            stage.setResizable(true);
            stage.centerOnScreen();
        });
        
        // Add shutdown hook to close database connection
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            SqliteConnection.closeConnection();
        }));
    }
    
    // Navigation methods
   
    /* Refreshes the current Inventory list without changing page. */
    @FXML
    private void handleInventoryButton(ActionEvent event) {
        // Already on inventory page
        refreshProducts();
    }
    
    /* Navigates to the Order page. */
    @FXML
    private void handleOrderButton(ActionEvent event) {
        try {
            loadScene(event, "/view/fxml/Order.fxml");
        } catch (IOException e) {
            showAlert("Error", "Could not load Order page: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    /* Confirms and logs out to the Login page. */
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
                showAlert("Error", "Could not load Login page: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    /* Reloads products from DB and shows a short success notice. */
    @FXML
    private void handleRefreshButton(ActionEvent event) {
        refreshProducts();
        showAlert("Refresh", "Product list has been refreshed successfully!", Alert.AlertType.INFORMATION);
    }
    
    // CRUD Operations
    /*
     * Validates inputs and inserts a new product.
     * Generates category-based numeric ID and stores timestamp/image path.
     */
    @FXML
    private void handleAddProduct(ActionEvent event) {
        if (validateForm()) {
            String sql = "INSERT INTO meds_product (product_id, name, category, price, stock, status, image_path, date_added) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement prepare = null;
            try {
                prepare = connection.prepareStatement(sql);
                
                // Generate a numeric ID for the database (the table display will format it with category code)
                int numericId = InventoryIdGenerator.generateIdForCategory(connection, categoryComboBox.getValue());
                
                prepare.setInt(1, numericId);
                prepare.setString(2, productNameField.getText().trim());
                prepare.setString(3, categoryComboBox.getValue());
                prepare.setDouble(4, Double.parseDouble(priceField.getText().trim()));
                prepare.setInt(5, Integer.parseInt(stockField.getText().trim()));
                prepare.setString(6, statusComboBox.getValue());
                prepare.setString(7, selectedImagePath != null ? selectedImagePath : "");
                
                long currentTimestamp = System.currentTimeMillis() / 1000;
                prepare.setLong(8, currentTimestamp);
                
                int result = prepare.executeUpdate();
                if (result > 0) {
                    showAlert("Success", "Product added successfully!", Alert.AlertType.INFORMATION);
                    clearForm();
                    
                    // Force refresh the table data
                    Platform.runLater(() -> {
                        loadProducts();
                        productsTable.refresh();
                        System.out.println("Table refreshed after adding product");
                    });
                } else {
                    showAlert("Error", "Failed to add product!", Alert.AlertType.ERROR);
                }
                
            } catch (SQLException e) {
                System.err.println("SQL Error in handleAddProduct: " + e.getMessage());
                e.printStackTrace();
                showAlert("Database Error", "Error adding product: " + e.getMessage(), Alert.AlertType.ERROR);
            } catch (NumberFormatException e) {
                showAlert("Input Error", "Please enter valid numbers for price and stock!", Alert.AlertType.ERROR);
            } finally {
                // Always close PreparedStatement
                if (prepare != null) {
                    try {
                        prepare.close();
                    } catch (SQLException e) {
                        System.err.println("Error closing PreparedStatement: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /* Updates the selected product after validation. */
    @FXML
    private void handleUpdateProduct(ActionEvent event) {
        if (selectedProduct == null) {
            showAlert("No Selection", "Please select a product to update!", Alert.AlertType.WARNING);
            return;
        }
        
        if (validateForm()) {
            String sql = "UPDATE meds_product SET name=?, category=?, price=?, stock=?, status=?, image_path=? WHERE product_id=?";
            
            PreparedStatement prepare = null;
            try {
                prepare = connection.prepareStatement(sql);
                prepare.setString(1, productNameField.getText());
                prepare.setString(2, categoryComboBox.getValue());
                prepare.setDouble(3, Double.parseDouble(priceField.getText()));
                prepare.setInt(4, Integer.parseInt(stockField.getText()));
                prepare.setString(5, statusComboBox.getValue());
                prepare.setString(6, selectedImagePath);
                prepare.setInt(7, selectedProduct.getId());
                
                int result = prepare.executeUpdate();
                if (result > 0) {
                    showAlert("Success", "Product updated successfully!", Alert.AlertType.INFORMATION);
                    clearForm();
                    loadProducts();
                } else {
                    showAlert("Error", "Failed to update product!", Alert.AlertType.ERROR);
                }
                
            } catch (SQLException e) {
                showAlert("Database Error", "Error updating product: " + e.getMessage(), Alert.AlertType.ERROR);
            } catch (NumberFormatException e) {
                showAlert("Input Error", "Please enter valid numbers for price and stock!", Alert.AlertType.ERROR);
            } finally {
           
                if (prepare != null) {
                    try {
                        prepare.close();
                    } catch (SQLException e) {
                        System.err.println("Error closing PreparedStatement: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /* Confirms and deletes the selected product. */
    @FXML
    private void handleDeleteProduct(ActionEvent event) {
        if (selectedProduct == null) {
            showAlert("No Selection", "Please select a product to delete!", Alert.AlertType.WARNING);
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Confirmation");
        confirmAlert.setHeaderText("Are you sure you want to delete this product?");
        confirmAlert.setContentText("Product: " + selectedProduct.getName() + "\nThis action cannot be undone!");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM meds_product WHERE product_id=?";
            
            PreparedStatement prepare = null;
            try {
                prepare = connection.prepareStatement(sql);
                prepare.setInt(1, selectedProduct.getId());
                
                int deleteResult = prepare.executeUpdate();
                if (deleteResult > 0) {
                    showAlert("Success", "Product deleted successfully!", Alert.AlertType.INFORMATION);
                    clearForm();
                    loadProducts();
                } else {
                    showAlert("Error", "Failed to delete product!", Alert.AlertType.ERROR);
                }
                
            } catch (SQLException e) {
                showAlert("Database Error", "Error deleting product: " + e.getMessage(), Alert.AlertType.ERROR);
            } finally {
                // Always close PreparedStatement
                if (prepare != null) {
                    try {
                        prepare.close();
                    } catch (SQLException e) {
                        System.err.println("Error closing PreparedStatement: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /* Clears all form fields and current selection. */
    @FXML
    private void handleClearForm(ActionEvent event) {
        clearForm();
    }
    
    // Image handling
    /* Lets the user pick an image file and previews it. */
    @FXML
    private void handleSelectImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        
        Stage stage = (Stage) selectImageButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        
        if (selectedFile != null) {
            selectedImagePath = selectedFile.getAbsolutePath();
            
            try {
                Image image = new Image(selectedFile.toURI().toString());
                productImageView.setImage(image);
            } catch (Exception e) {
                showAlert("Error", "Could not load image: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    /* Removes custom image and restores the default placeholder. */
    @FXML
    private void handleRemoveImage(ActionEvent event) {
        selectedImagePath = "";
        productImageView.setImage(new Image(getClass().getResourceAsStream("/view/images/addimage.png")));
    }
    
    // Setup methods
    /*
     * Fills category/status options and auto-generates a readable product ID
     * whenever category changes.
     */
    private void setupComboBoxes() {
        // Category options - using pharmacy categories
        categoryComboBox.getItems().addAll(
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
        
        // Add listener to category selection to auto-generate product ID
        categoryComboBox.setOnAction(event -> {
            String selectedCategory = categoryComboBox.getValue();
            if (selectedCategory != null && !selectedCategory.isEmpty()) {
                // Generate category-based ID and display it in the productIdField
                String generatedId = InventoryIdGenerator.generateCategoryIdString(connection, selectedCategory);
                productIdField.setText(generatedId);
                System.out.println("Generated ID for " + selectedCategory + ": " + generatedId);
            }
        });
        
        // Status options
        statusComboBox.getItems().addAll(
            "Available", "Out of Stock", "Discontinued", "Low Stock"
        );
    }
    
    
    private void setupTable() {
        System.out.println("Setting up table columns...");
            
        // Setup table columns with explicit cell value factories
        idColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        nameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName()));
        categoryColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategory()));
        priceColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getPrice()).asObject());
        stockColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getStock()).asObject());
        statusColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus()));
        dateColumn.setCellValueFactory(cellData -> {
            LocalDateTime dateAdded = cellData.getValue().getDateAdded();
            String dateString = dateAdded != null ? dateAdded.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) : "";
            return new javafx.beans.property.SimpleStringProperty(dateString);
        });
        
        // Apply white background styling to all columns except status
        String whiteColumnStyle = "-fx-alignment: center;";
        
        // Format ID column to show category-based format (e.g., "CLA-001")
        idColumn.setCellFactory(column -> new TableCell<Product, Integer>() {
            @Override
            protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                if (empty || id == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // Get the product from the table row to access category
                    Product product = getTableView().getItems().get(getIndex());
                    if (product != null) {
                        String categoryCode = InventoryIdGenerator.getCategoryCode(product.getCategory());
                        setText(String.format("%s-%03d", categoryCode, id));
                    } else {
                        setText(String.valueOf(id));
                    }
                    setStyle(whiteColumnStyle);
                }
            }
        });
        
        // Format name column
        nameColumn.setCellFactory(column -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(name);
                    setStyle(whiteColumnStyle);
                }
            }
        });
        
        // Format category column with unique color coding for each category
        categoryColumn.setCellFactory(column -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String category, boolean empty) {
                super.updateItem(category, empty);
                if (empty || category == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(category);
                    
                    // Apply unique color coding for each pharmacy category
                    String style = "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5; -fx-alignment: center; ";
                    
                    switch (category.toLowerCase()) {
                        case "pain reliever":
                        case "fever reducer":
                        case "nsaids":
                            style += "-fx-background-color: #DC2626;"; // Red for pain/fever meds
                            break;
                        case "antibiotic":
                        case "antifungal":
                        case "antiviral":
                            style += "-fx-background-color: #2563EB;"; // Blue for antimicrobials
                            break;
                        case "cough expectorant":
                        case "cough suppressant":
                        case "cold & flu":
                        case "respiratory / asthma":
                            style += "-fx-background-color: #059669;"; // Green for respiratory
                            break;
                        case "allergy":
                            style += "-fx-background-color: #7C3AED;"; // Purple for allergy
                            break;
                        case "stomach care":
                        case "antacid":
                        case "anti-diarrheal":
                        case "laxative":
                        case "anti-emetic":
                            style += "-fx-background-color: #EA580C;"; // Orange for digestive
                            break;
                        case "vitamin c":
                        case "multivitamin":
                        case "b-complex":
                        case "minerals":
                        case "supplements":
                            style += "-fx-background-color: #10B981;"; // Emerald for vitamins/supplements
                            break;
                        case "herbal medicine":
                            style += "-fx-background-color: #16A34A;"; // Nature green for herbal
                            break;
                        case "skin ointment":
                        case "antifungal cream":
                        case "steroid cream":
                            style += "-fx-background-color: #F59E0B;"; // Amber for topical treatments
                            break;
                        case "eye care":
                        case "ear care":
                            style += "-fx-background-color: #0EA5E9;"; // Sky blue for sensory care
                            break;
                        case "first aid":
                        case "alcohol / disinfectant":
                            style += "-fx-background-color: #EF4444;"; // Bright red for emergency/disinfection
                            break;
                        case "baby care":
                            style += "-fx-background-color: #EC4899;"; // Pink for baby care
                            break;
                        case "women's health":
                            style += "-fx-background-color: #BE185D;"; // Deep pink for women's health
                            break;
                        case "hypertension meds":
                        case "diabetes meds":
                        case "cholesterol meds":
                            style += "-fx-background-color: #7C2D12;"; // Brown for chronic conditions
                            break;
                        case "urinary care":
                            style += "-fx-background-color: #0891B2;"; // Cyan for urinary
                            break;
                        case "mental health":
                            style += "-fx-background-color: #6366F1;"; // Indigo for mental health
                            break;
                        case "oral rehydration solution":
                            style += "-fx-background-color: #06B6D4;"; // Light blue for hydration
                            break;
                        default:
                            style += "-fx-background-color: #6B7280;"; // Gray for unknown categories
                            break;
                    }
                    
                    setStyle(style);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
        
        // Format stock column
        stockColumn.setCellFactory(column -> new TableCell<Product, Integer>() {
            @Override
            protected void updateItem(Integer stock, boolean empty) {
                super.updateItem(stock, empty);
                if (empty || stock == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(stock));
                    // Apply bold text styling
                    setStyle("-fx-alignment: center; -fx-font-weight: bold;");
                }
            }
        });
        
        // Format date column
        dateColumn.setCellFactory(column -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(date);
                    setStyle(whiteColumnStyle);
                }
            }
        });
        
        // Format price column
        priceColumn.setCellFactory(column -> new TableCell<Product, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText("₱ " + decimalFormat.format(price));
                    // Apply bold green text styling
                    setStyle("-fx-alignment: center; -fx-font-weight: bold; -fx-text-fill: #22C55E;");
                }
            }
        });
        
        // Keep the existing status column with color coding (unchanged)
        statusColumn.setCellFactory(column -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    
                    // Apply color coding based on status
                    String style = "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5; ";
                    
                    switch (status.toLowerCase()) {
                        case "available":
                            style += "-fx-background-color: #22C55E;"; // Green 
                            break;
                        case "out of stock":
                            style += "-fx-background-color: #F97316;"; // Orange 
                            break;
                        case "discontinued":
                            style += "-fx-background-color: #EF4444;"; // Red 
                            break;
                        case "low stock":
                            style += "-fx-background-color: #FFC107;"; //Amber
                            break;
                    }
                    
                    setStyle(style);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
        
        // Setup image column with explicit cell value factory
        imageColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getImagePath()));
        imageColumn.setCellFactory(column -> new TableCell<Product, String>() {
            private final ImageView imageView = new ImageView();
            
            {
                imageView.setFitHeight(40);
                imageView.setFitWidth(40);
                imageView.setPreserveRatio(true);
            }
            
            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    try {
                        if (imagePath != null && !imagePath.isEmpty()) {
                            File imageFile = new File(imagePath);
                            if (imageFile.exists()) {
                                imageView.setImage(new Image(imageFile.toURI().toString()));
                            } else {
                                // Use existing image or create a placeholder
                                setDefaultImage();
                            }
                        } else {
                            // Use existing image or create a placeholder
                            setDefaultImage();
                        }
                    } catch (Exception e) {
                        // Use existing image or create a placeholder
                        setDefaultImage();
                    }
                    setGraphic(imageView);
                    // Center the image in the cell
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
            
            private void setDefaultImage() {
                try {
                    InputStream imageStream = getClass().getResourceAsStream("/view/images/addimage.png");
                    if (imageStream != null) {
                        imageView.setImage(new Image(imageStream));
                    } else {
                        imageView.setImage(createPlaceholderImage());
                    }
                } catch (Exception e) {
                    imageView.setImage(createPlaceholderImage());
                }
            }
            
            private Image createPlaceholderImage() {
                javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(40, 40);
                javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
                gc.setFill(javafx.scene.paint.Color.LIGHTGRAY);
                gc.fillRect(0, 0, 40, 40);
                gc.setStroke(javafx.scene.paint.Color.GRAY);
                gc.strokeRect(1, 1, 38, 38);
                gc.setFill(javafx.scene.paint.Color.GRAY);
                gc.fillText("No Image", 5, 25);
                
                javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
                params.setFill(javafx.scene.paint.Color.TRANSPARENT);
                return canvas.snapshot(params, null);
            }
        });
        
        // Setup actions column with buttons
        actionsColumn.setCellFactory(column -> new TableCell<Product, Void>() {
            private final Button updateButton = new Button();
            private final Button deleteButton = new Button();
            private final javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(5);
            
            {
                // Create ImageView for update button
                ImageView updateIcon = new ImageView();
                try {
                    updateIcon.setImage(new Image(getClass().getResourceAsStream("/view/images/update_status.png")));
                    updateIcon.setFitHeight(30);
                    updateIcon.setFitWidth(30);
                    updateIcon.setPreserveRatio(true);
                } catch (Exception e) {
                    System.err.println("Could not load update_status.png: " + e.getMessage());
                }
                updateButton.setGraphic(updateIcon);
                
                // Create ImageView for delete button
                ImageView deleteIcon = new ImageView();
                try {
                    deleteIcon.setImage(new Image(getClass().getResourceAsStream("/view/images/delete.png")));
                    deleteIcon.setFitHeight(30);
                    deleteIcon.setFitWidth(30);
                    deleteIcon.setPreserveRatio(true);
                } catch (Exception e) {
                    System.err.println("Could not load delete.png: " + e.getMessage());
                }
                deleteButton.setGraphic(deleteIcon);
                
                // Style the update button
                updateButton.setStyle("-fx-background-color: #0ea5e9; -fx-background-radius: 4; -fx-cursor: hand; -fx-border-width: 0; -fx-padding: 4;");
                updateButton.setPrefHeight(25);
                updateButton.setPrefWidth(30);
                updateButton.setTooltip(new Tooltip("Update Product"));
                
                // Style the delete button
                deleteButton.setStyle("-fx-background-color: #ef4444; -fx-background-radius: 4; -fx-cursor: hand; -fx-border-width: 0; -fx-padding: 4;");
                deleteButton.setPrefHeight(25);
                deleteButton.setPrefWidth(30);
                deleteButton.setTooltip(new Tooltip("Delete Product"));
                
                // Set button actions
                updateButton.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    if (product != null) {
                        selectedProduct = product;
                        showUpdateDialog(product);
                    }
                });
                
                deleteButton.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    if (product != null) {
                        selectedProduct = product;
                        handleDeleteProduct(null);
                    }
                });
                
                buttonBox.getChildren().addAll(updateButton, deleteButton);
                buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttonBox);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
        
        // Set the items to the table
        productsTable.setItems(productsList);
        System.out.println("Table items set. ProductsList size: " + productsList.size()); // Debug
        
        // Allow scrolling but disable row selection
        productsTable.setMouseTransparent(false); // Enable mouse events for scrolling and buttons
        productsTable.setFocusTraversable(false); // Disable keyboard focus
        productsTable.getSelectionModel().clearSelection();
        
        // Remove selection mode to prevent any selection
        productsTable.getSelectionModel().setSelectionMode(null);
        
        // Override row selection but allow button clicks and scrolling
        productsTable.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            
            // Disable row selection but allow mouse events to pass through to buttons
            row.setOnMouseClicked(event -> {
                // Only consume row clicks, not button clicks
                if (event.getTarget() == row || event.getTarget().getClass().toString().contains("TableRow")) {
                    event.consume(); // Prevent row selection
                }
                // Let button clicks pass through by not consuming the event
            });
            
            row.setOnMousePressed(event -> {
                // Only consume row presses, not button presses
                if (event.getTarget() == row || event.getTarget().getClass().toString().contains("TableRow")) {
                    event.consume(); // Prevent row selection
                }
                // Let button presses pass through by not consuming the event
            });
            
            return row;
        });
        
        System.out.println("Table setup completed with row clicking disabled but buttons and scrolling enabled."); // Debug
    }
    
    /** Loads products from DB into the table's backing list and refreshes the view. */
    private void loadProducts() {
        productsList.clear();
        String sql = "SELECT * FROM meds_product ORDER BY product_id DESC";
        
        PreparedStatement prepare = null;
        ResultSet result = null;
        
        try {
            prepare = connection.prepareStatement(sql);
            result = prepare.executeQuery();
            
            int count = 0;
            while (result.next()) {
                Product product = new Product(
                    result.getInt("product_id"),
                    result.getString("name"),
                    result.getString("category"),
                    result.getDouble("price"),
                    result.getInt("stock"),
                    result.getString("status"),
                    result.getString("image_path"),
                    result.getLong("date_added") 
                );
                productsList.add(product);
                count++;
                System.out.println("Loaded product: " + product.getName()); 
            }
            
            System.out.println("Total products loaded: " + count); 
            System.out.println("ProductsList size: " + productsList.size()); 
            
            // Refresh the table view
            productsTable.refresh();
            
        } catch (SQLException e) {
            System.err.println("SQL Error in loadProducts: " + e.getMessage());
            e.printStackTrace();
            showAlert("Database Error", "Error loading products: " + e.getMessage(), Alert.AlertType.ERROR);
        } finally {
          
            if (result != null) {
                try {
                    result.close();
                } catch (SQLException e) {
                    System.err.println("Error closing ResultSet: " + e.getMessage());
                }
            }
            if (prepare != null) {
                try {
                    prepare.close();
                } catch (SQLException e) {
                    System.err.println("Error closing PreparedStatement: " + e.getMessage());
                }
            }
        }
    }
    
    /** Copies selected product to the form, formats its ID, and previews its image. */
    private void selectProductForEdit(Product product) {
        selectedProduct = product;
        // Display the category-based ID format in the field
        String categoryCode = InventoryIdGenerator.getCategoryCode(product.getCategory());
        String formattedId = String.format("%s-%03d", categoryCode, product.getId());
        productIdField.setText(formattedId);
        
        productNameField.setText(product.getName());
        categoryComboBox.setValue(product.getCategory());
        priceField.setText(String.valueOf(product.getPrice()));
        stockField.setText(String.valueOf(product.getStock()));
        statusComboBox.setValue(product.getStatus());
        
        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            selectedImagePath = product.getImagePath();
            
            try {
                File imageFile = new File(selectedImagePath);
                if (imageFile.exists()) {
                    productImageView.setImage(new Image(imageFile.toURI().toString()));
                } else {
                    productImageView.setImage(new Image(getClass().getResourceAsStream("/view/images/addimage.png")));
                }
            } catch (Exception e) {
                productImageView.setImage(new Image(getClass().getResourceAsStream("/view/images/addimage.png")));
            }
        } else {
            handleRemoveImage(null);
        }
    }
    
    /* Quick info dialog for a product (formatted ID, price, stock, date). */
    private void showProductDetails(Product product) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Product Details");
        alert.setHeaderText(product.getName());
        
        // Format the ID with category code
        String categoryCode = InventoryIdGenerator.getCategoryCode(product.getCategory());
        String formattedId = String.format("%s-%03d", categoryCode, product.getId());
        
        alert.setContentText(
            "ID: " + formattedId + "\n" +
            "Category: " + product.getCategory() + "\n" +
            "Price: ₱ " + decimalFormat.format(product.getPrice()) + "\n" +
            "Stock: " + product.getStock() + "\n" +
            "Status: " + product.getStatus() + "\n" +
            "Date Added: " + product.getDateAdded()
        );
        alert.showAndWait();
    }
    
    /* Validates required fields and numeric formats; shows targeted errors. */
    private boolean validateForm() {
        if (productNameField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Product name is required!", Alert.AlertType.ERROR);
            return false;
        }
        
        if (categoryComboBox.getValue() == null) {
            showAlert("Validation Error", "Please select a category!", Alert.AlertType.ERROR);
            return false;
        }
        
        if (priceField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Price is required!", Alert.AlertType.ERROR);
            return false;
        }
        
        if (stockField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Stock quantity is required!", Alert.AlertType.ERROR);
            return false;
        }
        
        if (statusComboBox.getValue() == null) {
            showAlert("Validation Error", "Please select a status!", Alert.AlertType.ERROR);
            return false;
        }
        
        try {
            Double.parseDouble(priceField.getText());
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Please enter a valid price!", Alert.AlertType.ERROR);
            return false;
        }
        
        try {
            Integer.parseInt(stockField.getText());
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Please enter a valid stock quantity!", Alert.AlertType.ERROR);
            return false;
        }
        
        return true;
    }
    
    /* Resets the form and clears table selection. */
    private void clearForm() {
        selectedProduct = null;
        productIdField.clear();
        productNameField.clear();
        
        // Clear category combobox selection and restore prompt text
        categoryComboBox.getSelectionModel().clearSelection();
        categoryComboBox.setPromptText("Select category");
        
        priceField.clear();
        stockField.clear();
        
        // Clear status combobox selection and restore prompt text  
        statusComboBox.getSelectionModel().clearSelection();
        statusComboBox.setPromptText("Select status");
        
        handleRemoveImage(null);
        productsTable.getSelectionModel().clearSelection();
    }
    
    /* Convenience wrapper to reload products. */
    private void refreshProducts() {
        loadProducts();
    }
    
    // Utility methods
    /* Loads another FXML view into the current stage. */
    private void loadScene(ActionEvent event, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setTitle("Health Point System");
        stage.setScene(scene);
        stage.show();
    }
    
    /* Shows an alert with title/message/type. */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /* Shows a dialog for updating product information. */
    private void showUpdateDialog(Product product) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Update Product");
        dialog.setHeaderText("Update the product information below:");
        
        // Create form fields
        TextField nameField = new TextField(product.getName());
        ComboBox<String> categoryField = new ComboBox<>(FXCollections.observableArrayList(
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
        ));
        categoryField.setValue(product.getCategory());
        
        TextField priceField = new TextField(String.valueOf(product.getPrice()));
        TextField stockField = new TextField(String.valueOf(product.getStock()));
        ComboBox<String> statusField = new ComboBox<>(FXCollections.observableArrayList(
            "Available", "Out of Stock", "Discontinued", "Low Stock"
        ));
        statusField.setValue(product.getStatus());
        
        // Image view for product image
        ImageView imageView = new ImageView();
        imageView.setFitHeight(100);
        imageView.setFitWidth(100);
        imageView.setPreserveRatio(true);
        
        // Load current image or set placeholder
        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            File imageFile = new File(product.getImagePath());
            if (imageFile.exists()) {
                imageView.setImage(new Image(imageFile.toURI().toString()));
            } else {
                imageView.setImage(new Image(getClass().getResourceAsStream("/view/images/addimage.png")));
            }
        } else {
            imageView.setImage(new Image(getClass().getResourceAsStream("/view/images/addimage.png")));
        }
        
        // Button to select new image
        Button selectImageButton = new Button("Select Image");
        selectImageButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Product Image");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
            );
            
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            File selectedFile = fileChooser.showOpenDialog(stage);
            
            if (selectedFile != null) {
                String selectedImagePath = selectedFile.getAbsolutePath();
                imageView.setImage(new Image(selectedFile.toURI().toString()));
                
                // Optionally, store the selected image path in the product object
                product.setImagePath(selectedImagePath);
            }
        });
        
        // Button to remove image
        Button removeImageButton = new Button("Remove Image");
        removeImageButton.setOnAction(event -> {
            imageView.setImage(new Image(getClass().getResourceAsStream("/view/images/addimage.png")));
            product.setImagePath("");
        });
        
        // Layout for image selection
        VBox imageBox = new VBox(10, imageView, selectImageButton, removeImageButton);
        imageBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        // Add fields to dialog pane
        dialog.getDialogPane().setContent(new VBox(10,
            new Label("Product Name:"),
            nameField,
            new Label("Category:"),
            categoryField,
            new Label("Price:"),
            priceField,
            new Label("Stock:"),
            stockField,
            new Label("Status:"),
            statusField,
            new Label("Image:"),
            imageBox
        ));
        
        // Add buttons to dialog
        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(updateButtonType, cancelButtonType);
        
        // Convert the result to a Product object when the dialog is closed
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                // Validate and return the updated product
                if (validateUpdateForm(nameField, categoryField, priceField, stockField, statusField)) {
                    product.setName(nameField.getText().trim());
                    product.setCategory(categoryField.getValue());
                    product.setPrice(Double.parseDouble(priceField.getText().trim()));
                    product.setStock(Integer.parseInt(stockField.getText().trim()));
                    product.setStatus(statusField.getValue());
                    
                    // Return the updated product
                    return product;
                } else {
                    return null;
                }
            }
            return null;
        });
        
        // Show the dialog and wait for result
        dialog.showAndWait().ifPresent(updatedProduct -> {
            // Update the product in the database
            String sql = "UPDATE meds_product SET name=?, category=?, price=?, stock=?, status=?, image_path=? WHERE product_id=?";
            
            PreparedStatement prepare = null;
            try {
                prepare = connection.prepareStatement(sql);
                prepare.setString(1, updatedProduct.getName());
                prepare.setString(2, updatedProduct.getCategory());
                prepare.setDouble(3, updatedProduct.getPrice());
                prepare.setInt(4, updatedProduct.getStock());
                prepare.setString(5, updatedProduct.getStatus());
                prepare.setString(6, updatedProduct.getImagePath());
                prepare.setInt(7, updatedProduct.getId());
                
                int result = prepare.executeUpdate();
                if (result > 0) {
                    showAlert("Success", "Product updated successfully!", Alert.AlertType.INFORMATION);
                    loadProducts();
                } else {
                    showAlert("Error", "Failed to update product!", Alert.AlertType.ERROR);
                }
                
            } catch (SQLException e) {
                showAlert("Database Error", "Error updating product: " + e.getMessage(), Alert.AlertType.ERROR);
            } finally {
                if (prepare != null) {
                    try {
                        prepare.close();
                    } catch (SQLException e) {
                        System.err.println("Error closing PreparedStatement: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    /* Validates the update form fields. */
    private boolean validateUpdateForm(TextField nameField, ComboBox<String> categoryField, TextField priceField, TextField stockField, ComboBox<String> statusField) {
        if (nameField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Product name is required!", Alert.AlertType.ERROR);
            return false;
        }
        
        if (categoryField.getValue() == null) {
            showAlert("Validation Error", "Please select a category!", Alert.AlertType.ERROR);
            return false;
        }
        
        if (priceField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Price is required!", Alert.AlertType.ERROR);
            return false;
        }
        
        if (stockField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Stock quantity is required!", Alert.AlertType.ERROR);
            return false;
        }
        
        if (statusField.getValue() == null) {
            showAlert("Validation Error", "Please select a status!", Alert.AlertType.ERROR);
            return false;
        }
        
        try {
            Double.parseDouble(priceField.getText());
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Please enter a valid price!", Alert.AlertType.ERROR);
            return false;
        }
        
        try {
            Integer.parseInt(stockField.getText());
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Please enter a valid stock quantity!", Alert.AlertType.ERROR);
            return false;
        }
        
        return true;
    }
}
