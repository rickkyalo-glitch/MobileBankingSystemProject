package wire;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import wire.util.PasswordUtil;
import wire.util.SceneNavigator;

public class AdminLoginController {

    private static final Logger LOGGER = Logger.getLogger(AdminLoginController.class.getName());

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Button backBtn;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            SceneNavigator.showError("Error", "Please enter both username and password");
            return;
        }

        String sql = "SELECT password FROM admins WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && PasswordUtil.verify(password.toCharArray(), rs.getString("password"))) {
                    SceneNavigator.switchScene(loginBtn, "admin_dashboard.fxml");
                } else {
                    SceneNavigator.showError("Error", "Invalid admin credentials");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Admin login failed", e);
            SceneNavigator.showError("Database Error", "Error connecting to database");
        }
    }

    @FXML
    private void handleBack() {
        SceneNavigator.switchScene(backBtn, "login.fxml");
    }
}
