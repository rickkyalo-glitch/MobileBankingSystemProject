package wire.util;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * Every controller in the original project repeated the same
 * FXMLLoader/Stage-swap boilerplate and the same four-line Alert builder.
 * This centralizes both so a screen change or a UI tweak (e.g. adding the
 * shared stylesheet) happens in one place.
 */
public final class SceneNavigator {

    private static final Logger LOGGER = Logger.getLogger(SceneNavigator.class.getName());
    private static final String FXML_BASE = "/wire/fxml/";
    private static final String STYLESHEET = FXML_BASE + "styles.css";
    public static final int WINDOW_WIDTH = 800;
    public static final int WINDOW_HEIGHT = 600;

    private SceneNavigator() {
    }

    /**
     * Loads {@code fxmlFile} (relative to the fxml resource folder) and swaps
     * it into the window that {@code source} currently belongs to. Returns the
     * new screen's controller so the caller can pass it data (e.g. the logged
     * in user) before the screen is shown.
     */
    public static <T> T switchScene(Node source, String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(FXML_BASE + fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            scene.getStylesheets().add(SceneNavigator.class.getResource(STYLESHEET).toExternalForm());
            Stage stage = (Stage) source.getScene().getWindow();
            stage.setScene(scene);
            return loader.getController();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load screen: " + fxmlFile, e);
            showError("Navigation Error", "Could not load the requested screen.");
            return null;
        }
    }

    /** Applies the shared stylesheet to the very first scene shown at startup. */
    public static void applyStylesheet(Scene scene) {
        scene.getStylesheets().add(SceneNavigator.class.getResource(STYLESHEET).toExternalForm());
    }

    public static void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    public static void showInfo(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
