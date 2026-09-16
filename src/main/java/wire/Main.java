package wire;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import wire.util.SceneNavigator;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            DatabaseConnection.initializeDatabase();

            Parent root = new FXMLLoader(getClass().getResource("fxml/login.fxml")).load();
            Scene scene = new Scene(root, SceneNavigator.WINDOW_WIDTH, SceneNavigator.WINDOW_HEIGHT);
            SceneNavigator.applyStylesheet(scene);

            primaryStage.setTitle("Wire - Mobile Money");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();
        } catch (Exception e) {
            // The original silently printStackTrace()'d and left a blank
            // window on any startup failure. Tell the user something is wrong.
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Startup Error");
            alert.setHeaderText("Wire could not start");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            throw new IllegalStateException("Application failed to start", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
