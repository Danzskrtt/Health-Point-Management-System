module HealthPoint {
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.base;
	requires javafx.graphics;
	requires javafx.swing; 
	
	// SQLite database
	requires java.sql;
	
	// Desktop integration for opening PDFs
	requires java.desktop;
	
	// iTextPDF for PDF generation (non-modular JAR)
	requires itextpdf;
	
	// Ikonli for icons
	requires org.kordamp.ikonli.core;
	requires org.kordamp.ikonli.javafx;
	requires org.kordamp.ikonli.bootstrapicons;
	
	// Export packages
	exports model;
	exports controller;
	
	// Open packages to JavaFX for access
	opens model to javafx.base, javafx.fxml;
	opens controller to javafx.fxml;
}