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

public class RegisterController {

    private static final Logger LOGGER = Logger.getLogger(RegisterController.class.getName());
    // Kenyan mobile numbers: 07XXXXXXXX / 01XXXXXXXX, or the +254 equivalent.
    private static final String PHONE_PATTERN = "^(?:\\+254|0)[17]\\d{8}$";

    @FXML private TextField nameField;
    @FXML private TextField idNumberField;
    @FXML private TextField phoneField;
    @FXML private PasswordField pinField;
    @FXML private PasswordField confirmPinField;
    @FXML private Button registerBtn;
    @FXML private Button backBtn;

    @FXML
    private void handleRegister() {
        String name = nameField.getText().trim();
        String idNumber = idNumberField.getText().trim();
        String phone = phoneField.getText().trim();
        String pin = pinField.getText();
        String confirmPin = confirmPinField.getText();

        if (name.isEmpty() || idNumber.isEmpty() || phone.isEmpty() || pin.isEmpty()) {
            SceneNavigator.showError("Error", "Please fill in all fields");
            return;
        }

        if (!phone.matches(PHONE_PATTERN)) {
            SceneNavigator.showError("Error", "Enter a valid phone number, e.g. 0712345678");
            return;
        }

        if (!pin.matches("\\d{4}")) {
            SceneNavigator.showError("Error", "PIN must be exactly 4 digits");
            return;
        }

        if (!pin.equals(confirmPin)) {
            SceneNavigator.showError("Error", "PINs do not match");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (userAlreadyExists(conn, idNumber, phone)) {
                SceneNavigator.showError("Error", "ID Number or Phone Number already registered");
                return;
            }

            String insertSql = "INSERT INTO users (name, id_number, phone_number, pin) VALUES (?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, name);
                insertStmt.setString(2, idNumber);
                insertStmt.setString(3, phone);
                insertStmt.setString(4, PasswordUtil.hash(pin.toCharArray()));
                insertStmt.executeUpdate();
            }

            SceneNavigator.showInfo("Success", "Registration successful! Please login.");
            handleBack();

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Registration failed", e);
            SceneNavigator.showError("Database Error", "Error registering user");
        }
    }

    private boolean userAlreadyExists(Connection conn, String idNumber, String phone) throws SQLException {
        String checkSql = "SELECT 1 FROM users WHERE id_number = ? OR phone_number = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, idNumber);
            checkStmt.setString(2, phone);
            try (ResultSet rs = checkStmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @FXML
    private void handleBack() {
        SceneNavigator.switchScene(backBtn, "login.fxml");
    }
}
