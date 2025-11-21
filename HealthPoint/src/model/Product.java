package model;

import java.time.LocalDateTime;

public class Product {
    private int id;
    private String name;
    private String category;
    private double price;
    private int stock;
    private String status;
    private String imagePath;
    private LocalDateTime dateAdded;

    public Product() {}

    // Constructor that matches OrderController expectations
    public Product(int id, String name, String category, double price, int stock, String status, String imagePath, LocalDateTime dateAdded) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.status = status;
        this.imagePath = imagePath;
        this.dateAdded = dateAdded;
    }

    // Alternative constructor with String dateAdded (for backward compatibility)
    public Product(int id, String name, String category, double price, int stock, String status, String imagePath, String dateAdded) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.status = status;
        this.imagePath = imagePath;
        this.dateAdded = LocalDateTime.now(); // Default to current time
    }

    // Constructor with Unix timestamp (long) for date_added field
    public Product(int id, String name, String category, double price, int stock, String status, String imagePath, long unixTimestamp) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.status = status;
        this.imagePath = imagePath;
        this.dateAdded = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochSecond(unixTimestamp), 
            java.time.ZoneId.systemDefault()
        );
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public double getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public String getStatus() {
        return status;
    }

    public String getImagePath() {
        return imagePath;
    }

    public LocalDateTime getDateAdded() {
        return dateAdded;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setDateAdded(LocalDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", price=" + price +
                ", stock=" + stock +
                ", status='" + status + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", dateAdded=" + dateAdded +
                '}';
    }
}