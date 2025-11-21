package model;
	
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;

public class Main extends Application {
	
	public static void main(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		
		Parent root = FXMLLoader.load(getClass().getResource("/view/fxml/LoginPage.fxml"));
		Scene scene = new Scene(root, 800, 600);

		Image icon = new Image(getClass().getResourceAsStream("/view/images/healthPoint.png"));
		primaryStage.getIcons().add(icon);
		primaryStage.setTitle("Health Point System");
		primaryStage.setScene(scene);
		primaryStage.setMaximized(false);
		primaryStage.setResizable(false);
	    primaryStage.centerOnScreen();
		primaryStage.show();
	}
}