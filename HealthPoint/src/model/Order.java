package model;

import java.time.LocalDateTime;
import java.util.List;

public class Order {
    private String id; 
    private String customerName;
    private String paymentMethod; 
    private String status;
    private double totalAmount;
    private LocalDateTime orderDate;
    private List<OrderItem> orderItems;
    private String notes;

    // Default constructor
    public Order() {}

    // Constructor for database operations 
    public Order(String id, String customerName, String paymentMethod, 
                 double totalAmount, LocalDateTime orderDate, String status) {
        this.id = id;
        this.customerName = customerName;
        this.paymentMethod = paymentMethod;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        this.status = status;
    }

    // Full constructor with all parameters
    public Order(String id, String customerName, String paymentMethod, 
                 String status, double totalAmount, LocalDateTime orderDate, 
                 List<OrderItem> orderItems, String notes) {
        this.id = id;
        this.customerName = customerName;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        this.orderItems = orderItems;
        this.notes = notes;
    }

    // Getters
    public String getId() { 
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public String getNotes() {
        return notes;
    }

    // Setters
    public void setId(String id) { 
        this.id = id;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' + 
                ", customerName='" + customerName + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", status='" + status + '\'' +
                ", totalAmount=" + totalAmount +
                ", orderDate=" + orderDate +
                ", notes='" + notes + '\'' +
                '}';
    }
}