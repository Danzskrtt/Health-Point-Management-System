package model;

import java.sql.SQLException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginModel {
	
Connection connection;

   public LoginModel() {
	   
	   connection = SqliteConnection.Connector();
	   if (connection == null) {
		   
		   System.out.println("Database Connection Failed");
		   System.exit(1);
	   
	   }
	   
   }
   
       public boolean isDbConnected() {
    	   try {
			 return !connection.isClosed();
		   } catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		   }
    	   
       }
       
       public boolean isLogin(String user, String pass) throws SQLException {
    	   PreparedStatement preparedStatement = null;
    	   ResultSet resultSet = null;
    	   String query = "SELECT * FROM user WHERE username = ? AND password = ?";
    	   try {
    		   preparedStatement = connection.prepareStatement(query);
    		   preparedStatement.setString(1, user);
    		   preparedStatement.setString(2, pass);
    		   
    		   resultSet =  preparedStatement.executeQuery();
    		   if(resultSet.next()) {
    			   return true;
    		   }else {
    			   return false;
    		   }
    	   } catch (Exception e) {
    		   e.printStackTrace();
    		   return false;
    	   }finally {
    		   try {
    			   if (resultSet != null) {
    				   resultSet.close();
    			   }
    		   } catch (SQLException e) {
    			   e.printStackTrace();
    		   }
    		   try {
    			   if (preparedStatement != null) {
    				   preparedStatement.close();
    			   }
    		   } catch (SQLException e) {
    			   e.printStackTrace();
    		   }
    	   }
       }
}