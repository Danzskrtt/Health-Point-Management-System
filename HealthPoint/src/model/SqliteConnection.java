package model;

import java.sql.Connection;


import java.sql.DriverManager;
import java.sql.SQLException;

public class SqliteConnection {
    private static final String DATABASE_URL = "jdbc:sqlite:Healthpoint.db";
    private static Connection connection;
    
    public static Connection Connector() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DATABASE_URL);
            System.out.println("Database connection successful!");
            return connection;
        } catch (Exception e) {
            System.err.println("Error connecting to database: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
}