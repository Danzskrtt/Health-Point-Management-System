package controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import model.Product;
import model.OrderItem;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.List;

/**
 * ProductCardController
 * 
 * Renders a single medication card in the Order page: displays medication image, 
 * name, price, category, stock status, and allows pharmacist to select quantity 
 * to add to prescription cart.
 */
public class ProductCardController implements Initializable {

    @FXML
    private ImageView medicationImageView;
    
    @FXML
    private Label medicationNameLabel;
    
    @FXML
    private Label medicationPriceLabel;
    
    @FXML
    private Label medicationCategoryLabel;
    
    @FXML
    private Label medicationStockLabel;
    
    @FXML
    private Spinner<Integer> quantitySpinner;
    
    @FXML
    private Button addToPrescriptionButton;

    private Product medication;
    private OrderController orderController;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize quantity spinner with pharmacy-appropriate range
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1);
        quantitySpinner.setValueFactory(valueFactory);
        quantitySpinner.setEditable(true);
    }

    public void setProduct(Product medication) {
        this.medication = medication;
        updateMedicationDisplay();
    }

    public void setOrderController(OrderController orderController) {
        this.orderController = orderController;
    }

    // Updates all UI elements with current medication information
    private void updateMedicationDisplay() {
        if (medication != null) {
            medicationNameLabel.setText(medication.getName());
            medicationPriceLabel.setText(String.format("₱%.2f", medication.getPrice()));
            medicationCategoryLabel.setText(medication.getCategory());
            medicationStockLabel.setText("Stock: " + medication.getStock());
            updateStockIndicator();
            
            try {
                String imagePath = medication.getImagePath();
                Image image = null;
                
                if (imagePath != null && !imagePath.isEmpty()) {
                    File imageFile = new File(imagePath);
                    if (imageFile.exists()) {
                        image = new Image(imageFile.toURI().toString());
                    }
                }
                
                if (image == null) {
                    image = new Image(getClass().getResource("/view/images/addimage.png").toExternalForm());
                }
                
                medicationImageView.setImage(image);
                medicationImageView.setPreserveRatio(true);
                medicationImageView.setSmooth(true);
                medicationImageView.setCache(true);
                medicationImageView.setFitWidth(180.0);
                medicationImageView.setFitHeight(180.0);
                
            } catch (Exception e) {
                System.err.println("Error loading medication image: " + e.getMessage());
                try {
                    Image fallbackImage = new Image(getClass().getResource("/view/images/addimage.png").toExternalForm());
                    medicationImageView.setImage(fallbackImage);
                    medicationImageView.setFitWidth(180.0);
                    medicationImageView.setFitHeight(180.0);
                } catch (Exception ex) {
                    System.err.println("Failed to load fallback image: " + ex.getMessage());
                }
            }
            
            // Enable/disable add button based on stock availability
            addToPrescriptionButton.setDisable(medication.getStock() <= 0);
        }
    }

    // Updates stock label styling based on availability
    private void updateStockIndicator() {
        if (medication != null) {
            String stockStyle;
            if (medication.getStock() <= 0) {
                // Out of stock - red styling
                stockStyle = "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; " +
                           "-fx-background-radius: 12px; -fx-font-weight: bold; -fx-padding: 4px 8px;";
            } else if (medication.getStock() <= 5) {
                // Low stock - orange styling
                stockStyle = "-fx-background-color: #fef3c7; -fx-text-fill: #d97706; " +
                           "-fx-background-radius: 12px; -fx-font-weight: bold; -fx-padding: 4px 8px;";
            } else {
                // In stock - green styling
                stockStyle = "-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; " +
                           "-fx-background-radius: 12px; -fx-font-weight: bold; -fx-padding: 4px 8px;";
            }
            medicationStockLabel.setStyle(stockStyle);
        }
    }

    /**
     * Handles adding medication to prescription cart.
     * Validates stock availability and quantity before adding.
     */
    @FXML
    private void handleAddToPrescription() {
        if (medication == null) {
            showAlert("Error", "No medication selected.", Alert.AlertType.ERROR);
            return;
        }

        if (medication.getStock() <= 0) {
            showAlert("Out of Stock", "This medication is currently out of stock.", Alert.AlertType.WARNING);
            return;
        }

        int quantity = quantitySpinner.getValue();
        if (quantity > medication.getStock()) {
            showAlert("Insufficient Stock", 
                     "Only " + medication.getStock() + " units available in stock.", 
                     Alert.AlertType.WARNING);
            return;
        }

        if (orderController != null) {
            orderController.addProductToCart(medication, quantity);
            showAlert("Added to Cart", 
                     quantity + "x " + medication.getName() + " added to prescription cart!", 
                     Alert.AlertType.INFORMATION);
            
            // Reset quantity to 1 after successful addition
            quantitySpinner.getValueFactory().setValue(1);
        }
    }

    // Button hover effects
    @FXML
    private void handleButtonHover(MouseEvent event) {
        if (addToPrescriptionButton != null && !addToPrescriptionButton.isDisabled()) {
            addToPrescriptionButton.setStyle(
                "-fx-background-color: #3b82f6; -fx-text-fill: white; " +
                "-fx-background-radius: 6px; -fx-font-weight: 600; -fx-font-size: 13px;"
            );
        }
    }

    @FXML
    private void handleButtonExit(MouseEvent event) {
        if (addToPrescriptionButton != null && !addToPrescriptionButton.isDisabled()) {
            addToPrescriptionButton.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; " +
                "-fx-background-radius: 6px; -fx-font-weight: 600; -fx-font-size: 13px;"
            );
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}