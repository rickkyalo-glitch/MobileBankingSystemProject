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

public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    @FXML private TextField phoneField;
    @FXML private PasswordField pinField;
    @FXML private Button loginBtn;
    @FXML private Button registerBtn;
    @FXML private Button adminLoginBtn;

    @FXML
    private void handleLogin() {
        String phone = phoneField.getText().trim();
        String pin = pinField.getText();

        if (phone.isEmpty() || pin.isEmpty()) {
            SceneNavigator.showError("Error", "Please enter both phone number and PIN");
            return;
        }

        String sql = "SELECT * FROM users WHERE phone_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && PasswordUtil.verify(pin.toCharArray(), rs.getString("pin"))) {
                    User user = new User(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("id_number"),
                            rs.getString("phone_number"),
                            rs.getString("pin"),
                            rs.getDouble("balance"));
                    openDashboard(user);
                } else {
                    // Same message whether the phone number doesn't exist or the
                    // PIN was wrong, so a login attempt can't be used to check
                    // which phone numbers are registered.
                    SceneNavigator.showError("Error", "Invalid phone number or PIN");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Login query failed", e);
            SceneNavigator.showError("Database Error", "Error connecting to database");
        }
    }

    @FXML
    private void handleRegister() {
        SceneNavigator.switchScene(registerBtn, "register.fxml");
    }

    @FXML
    private void handleAdminLogin() {
        SceneNavigator.switchScene(adminLoginBtn, "admin_login.fxml");
    }

    private void openDashboard(User user) {
        DashboardController controller = SceneNavigator.switchScene(loginBtn, "dashboard.fxml");
        if (controller != null) {
            controller.setUser(user);
        }
    }
}
