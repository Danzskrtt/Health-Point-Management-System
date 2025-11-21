package model;

import javafx.stage.FileChooser;


import javafx.stage.Stage;
import javafx.scene.control.Alert;
import java.io.FileWriter;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReceiptGenerator {
    
    private static final String PHARMACY_NAME = "HEALTH POINT";
    private static final String PHARMACY_ADDRESS_LINE1 = "Brgy. 123";
    private static final String PHARMACY_ADDRESS_LINE2 = "Ormoc City 6541, Leyte";
    private static final String PHARMACY_PHONE = "Phone: 0012-345-6789";
    
    public static boolean generateReceipt(Stage parentStage, String orderId, String customerName, 
                                        String paymentMethod, double totalAmount, 
                                        List<OrderItem> orderItems) {
        try {
            // File chooser to save receipt
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Receipt as Text File");
            fileChooser.setInitialFileName("Receipt_" + orderId + ".txt");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
            );
            
            File file = fileChooser.showSaveDialog(parentStage);
            
            if (file != null) {
                // Ensure .txt extension
                String filePath = file.getAbsolutePath();
                if (!filePath.endsWith(".txt")) {
                    filePath += ".txt";
                }
                
                // Create text receipt
                try (FileWriter writer = new FileWriter(filePath)) {
                    writeReceiptContent(writer, orderId, customerName, paymentMethod, totalAmount, orderItems);
                }
                
                // Show success message
                showAlert(Alert.AlertType.INFORMATION, "Receipt Generated", 
                         "Receipt saved successfully!\nLocation: " + filePath);
                
                // Optional: Open the text file automatically
                try {
                    java.awt.Desktop.getDesktop().open(new File(filePath));
                } catch (Exception e) {
                    System.out.println("Could not open text file automatically: " + e.getMessage());
                }
                
                return true;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Receipt Generation Error", 
                     "Error generating receipt: " + e.getMessage());
        }
        
        return false;
    }
    
    private static void writeReceiptContent(FileWriter writer, String orderId, String customerName, 
                                          String paymentMethod, double totalAmount, 
                                          List<OrderItem> orderItems) throws Exception {
        
        // Header
        writer.write("===============================================\n");
        writer.write(centerText(PHARMACY_NAME, 47) + "\n");
        writer.write("===============================================\n");
        writer.write(centerText(PHARMACY_ADDRESS_LINE1, 47) + "\n");
        writer.write(centerText(PHARMACY_ADDRESS_LINE2, 47) + "\n");
        writer.write(centerText(PHARMACY_PHONE, 47) + "\n");
        writer.write("===============================================\n");
        writer.write(centerText("SALES INVOICE", 47) + "\n");
        writer.write("===============================================\n\n");
      
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");
        
        writer.write("Order ID: " + orderId + "\n");
        writer.write("Payment Method: " + paymentMethod + "\n");
        writer.write("Date & Time: " + now.format(formatter) + "\n\n");
        
        // Items section
        writer.write("===============================================\n");
        writer.write(centerText("ITEMS", 47) + "\n");
        writer.write("===============================================\n");
        writer.write(String.format("%-20s %5s %9s %10s\n", "Item", "Qty", "Price", "Total"));
        writer.write("-----------------------------------------------\n");
        
        double subtotal = 0;
        for (OrderItem item : orderItems) {
            writer.write(String.format("%-20s %5d %9.2f %10.2f\n", 
                truncateString(item.getProductName(), 20),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()));
            subtotal += item.getTotalPrice();
        }
        
        writer.write("-----------------------------------------------\n");
        writer.write(String.format("%35s %10.2f\n", "Subtotal: ₱", subtotal));
        
        double tax = 0;
        if (tax > 0) {
            writer.write(String.format("%35s %10.2f\n", "Tax: ₱", tax));
        }
        
        writer.write("===============================================\n");
        writer.write(String.format("%35s %10.2f\n", "TOTAL: ₱", totalAmount));
        writer.write("===============================================\n\n");
    
        writer.write(centerText("Thank you for your purchase!", 47) + "\n");
        writer.write(centerText("Please visit us again!", 47) + "\n\n");
        writer.write("===============================================\n");
    }
    
    private static String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < padding; i++) {
            sb.append(" ");
        }
        sb.append(text);
        return sb.toString();
    }
    
    private static String truncateString(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
    
    private static void showAlert(Alert.AlertType alertType, String title, String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}