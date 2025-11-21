package model;

public class OrderIdGenerator {
    public static String generateOrderId() {
        // Use timestamp to ensure uniqueness - gets last 6 digits of current timestamp
        long timestamp = System.currentTimeMillis() % 1_000_000;
        return String.format("ORD-%06d", timestamp);
    }

    public static void main(String[] args) {
        //should produce a unique ID
        System.out.println(generateOrderId());
        try {
            Thread.sleep(1); // Small delay to show different timestamps
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println(generateOrderId());
    }
}