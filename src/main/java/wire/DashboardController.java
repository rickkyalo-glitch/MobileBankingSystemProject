package wire;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import wire.util.PasswordUtil;
import wire.util.SceneNavigator;

public class DashboardController {

    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());
    private static final String PHONE_PATTERN = "^(?:\\+254|0)[17]\\d{8}$";

    @FXML private Label welcomeLabel;
    @FXML private Label balanceLabel;
    @FXML private Button depositBtn;
    @FXML private Button withdrawBtn;
    @FXML private Button sendMoneyBtn;
    @FXML private Button payBillBtn;
    @FXML private Button savingsBtn;
    @FXML private Button statementBtn;
    @FXML private Button changePinBtn;
    @FXML private Button logoutBtn;

    private User user;

    public void setUser(User user) {
        this.user = user;
        updateDashboard();
    }

    private void updateDashboard() {
        if (user != null) {
            welcomeLabel.setText("Welcome, " + user.getName());
            balanceLabel.setText(String.format("KSh %.2f", user.getBalance()));
        }
    }

    @FXML
    private void handleDeposit() {
        showAmountDialog("Deposit", "Enter amount to deposit:", "DEPOSIT", null, null);
    }

    @FXML
    private void handleWithdraw() {
        showAmountDialog("Withdraw", "Enter amount to withdraw:", "WITHDRAWAL", null, null);
    }

    @FXML
    private void handleSendMoney() {
        TextInputDialog phoneDialog = new TextInputDialog();
        phoneDialog.setTitle("Send Money");
        phoneDialog.setHeaderText("Enter recipient's phone number:");
        phoneDialog.setContentText("Phone:");

        phoneDialog.showAndWait().ifPresent(phone -> {
            String trimmed = phone.trim();
            if (!trimmed.matches(PHONE_PATTERN)) {
                SceneNavigator.showError("Error", "Enter a valid phone number, e.g. 0712345678");
                return;
            }
            showAmountDialog("Send Money", "Enter amount to send to " + trimmed + ":", "SEND_MONEY", trimmed, null);
        });
    }

    @FXML
    private void handlePayBill() {
        TextInputDialog tillDialog = new TextInputDialog();
        tillDialog.setTitle("Pay Bill");
        tillDialog.setHeaderText("Enter till number:");
        tillDialog.setContentText("Till Number:");

        tillDialog.showAndWait().ifPresent(till -> {
            if (till.trim().isEmpty()) {
                SceneNavigator.showError("Error", "Enter a till number");
                return;
            }
            showAmountDialog("Pay Bill", "Enter amount to pay to till " + till + ":", "PAY_BILL", null, till.trim());
        });
    }

    @FXML
    private void handleSavings() {
        showAmountDialog("Savings", "Enter amount to save:", "SAVINGS", null, null);
    }

    @FXML
    private void handleStatement() {
        showStatement();
    }

    @FXML
    private void handleChangePin() {
        TextInputDialog idDialog = new TextInputDialog();
        idDialog.setTitle("Change PIN");
        idDialog.setHeaderText("Enter your ID Number to verify your identity");
        idDialog.setContentText("ID Number:");

        Optional<String> idResult = idDialog.showAndWait();
        if (idResult.isEmpty()) {
            return;
        }

        if (!idResult.get().equals(user.getIdNumber())) {
            SceneNavigator.showError("Error", "ID Number does not match our records");
            return;
        }

        TextInputDialog newPinDialog = new TextInputDialog();
        newPinDialog.setTitle("Change PIN");
        newPinDialog.setHeaderText("Enter new 4-digit PIN");
        newPinDialog.setContentText("New PIN:");

        Optional<String> newPinResult = newPinDialog.showAndWait();
        if (newPinResult.isEmpty()) {
            return;
        }

        String newPin = newPinResult.get();
        if (!newPin.matches("\\d{4}")) {
            SceneNavigator.showError("Error", "PIN must be 4 digits (numbers only)");
            return;
        }

        TextInputDialog confirmPinDialog = new TextInputDialog();
        confirmPinDialog.setTitle("Change PIN");
        confirmPinDialog.setHeaderText("Confirm your new PIN");
        confirmPinDialog.setContentText("Confirm PIN:");

        Optional<String> confirmPinResult = confirmPinDialog.showAndWait();
        if (confirmPinResult.isEmpty()) {
            return;
        }

        if (!newPin.equals(confirmPinResult.get())) {
            SceneNavigator.showError("Error", "PINs do not match");
            return;
        }

        updatePinInDatabase(newPin);
    }

    private void updatePinInDatabase(String newPin) {
        String sql = "UPDATE users SET pin = ? WHERE id = ?";
        String newHash = PasswordUtil.hash(newPin.toCharArray());
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newHash);
            stmt.setInt(2, user.getId());

            if (stmt.executeUpdate() > 0) {
                user.setPinHash(newHash);
                SceneNavigator.showInfo("Success", "PIN changed successfully!");
            } else {
                SceneNavigator.showError("Error", "Failed to change PIN");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "PIN change failed", e);
            SceneNavigator.showError("Database Error", "Error changing PIN");
        }
    }

    @FXML
    private void handleLogout() {
        SceneNavigator.switchScene(logoutBtn, "login.fxml");
    }

    private void showAmountDialog(String title, String content, String transactionType,
                                   String recipientPhone, String tillNumber) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(content);
        dialog.setContentText("Amount:");

        dialog.showAndWait().ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr.trim());
                processTransaction(transactionType, amount, recipientPhone, tillNumber);
            } catch (NumberFormatException e) {
                SceneNavigator.showError("Error", "Please enter a valid amount");
            }
        });
    }

    private void processTransaction(String type, double amount, String recipientPhone, String tillNumber) {
        if (amount <= 0) {
            SceneNavigator.showError("Error", "Amount must be positive");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                switch (type) {
                    case "DEPOSIT" -> processDeposit(conn, amount);
                    case "WITHDRAWAL" -> processWithdrawal(conn, amount);
                    case "SEND_MONEY" -> processSendMoney(conn, amount, recipientPhone);
                    case "PAY_BILL" -> processPayBill(conn, amount, tillNumber);
                    case "SAVINGS" -> processSavings(conn, amount);
                    default -> throw new SQLException("Unknown transaction type: " + type);
                }
                conn.commit();
                updateDashboard();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Transaction failed: " + type, e);
            SceneNavigator.showError("Error", "Transaction failed: " + e.getMessage());
        }
    }

    private void processDeposit(Connection conn, double amount) throws SQLException {
        updateOwnBalance(conn, amount);
        recordTransaction(conn, "DEPOSIT", amount, null, null, 0);
        SceneNavigator.showInfo("Success", String.format("Deposited KSh %.2f successfully", amount));
    }

    private void processWithdrawal(Connection conn, double amount) throws SQLException {
        if (amount > user.getBalance()) {
            throw new SQLException("Insufficient balance");
        }
        updateOwnBalance(conn, -amount);
        recordTransaction(conn, "WITHDRAWAL", amount, null, null, 0);
        SceneNavigator.showInfo("Success", String.format("Withdrawn KSh %.2f successfully", amount));
    }

    /**
     * Bug fix: the original implementation deducted the sender's balance and
     * recorded a transaction row, but never located the recipient's account
     * or credited them — the money was simply deducted and gone. This now
     * looks the recipient up first (failing before anything is deducted if
     * they don't have a Wire account) and credits their balance for the real
     * transfer amount, same as the fee already went to company_income.
     */
    private void processSendMoney(Connection conn, double amount, String recipientPhone) throws SQLException {
        if (recipientPhone.equals(user.getPhoneNumber())) {
            throw new SQLException("You cannot send money to your own number");
        }

        int recipientId = findUserIdByPhone(conn, recipientPhone);
        if (recipientId == -1) {
            throw new SQLException("No Wire account found for " + recipientPhone);
        }

        double fee = amount > 500 ? amount * 0.001 : 0;
        double total = amount + fee;
        if (total > user.getBalance()) {
            throw new SQLException("Insufficient balance including transaction fee");
        }

        updateOwnBalance(conn, -total);
        creditBalance(conn, recipientId, amount);
        recordTransaction(conn, "SEND_MONEY", amount, recipientPhone, null, fee);

        if (fee > 0) {
            recordCompanyIncome(conn, getLastTransactionId(conn), fee);
        }

        SceneNavigator.showInfo("Success",
                String.format("Sent KSh %.2f to %s. Fee: KSh %.2f", amount, recipientPhone, fee));
    }

    private void processPayBill(Connection conn, double amount, String tillNumber) throws SQLException {
        double fee = amount > 500 ? amount * 0.001 : 0;
        double total = amount + fee;
        if (total > user.getBalance()) {
            throw new SQLException("Insufficient balance including transaction fee");
        }

        updateOwnBalance(conn, -total);
        recordTransaction(conn, "PAY_BILL", amount, null, tillNumber, fee);

        if (fee > 0) {
            recordCompanyIncome(conn, getLastTransactionId(conn), fee);
        }

        SceneNavigator.showInfo("Success",
                String.format("Paid KSh %.2f to till %s. Fee: KSh %.2f", amount, tillNumber, fee));
    }

    private void processSavings(Connection conn, double amount) throws SQLException {
        if (amount > user.getBalance()) {
            throw new SQLException("Insufficient balance");
        }

        updateOwnBalance(conn, -amount);
        recordTransaction(conn, "SAVINGS", amount, null, null, 0);

        String savingsSql = "INSERT INTO savings (user_id, amount) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(savingsSql)) {
            stmt.setInt(1, user.getId());
            stmt.setDouble(2, amount);
            stmt.executeUpdate();
        }

        SceneNavigator.showInfo("Success", String.format("Saved KSh %.2f successfully", amount));
    }

    /** Updates the logged-in user's row and keeps the in-memory User in sync. */
    private void updateOwnBalance(Connection conn, double delta) throws SQLException {
        creditBalance(conn, user.getId(), delta);
        user.setBalance(user.getBalance() + delta);
    }

    /** Updates any user's balance by id — used for crediting a Send Money recipient. */
    private void creditBalance(Connection conn, int userId, double delta) throws SQLException {
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, delta);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    private int findUserIdByPhone(Connection conn, String phone) throws SQLException {
        String sql = "SELECT id FROM users WHERE phone_number = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("id") : -1;
            }
        }
    }

    private void recordTransaction(Connection conn, String type, double amount, String recipient,
                                    String till, double fee) throws SQLException {
        String sql = """
            INSERT INTO transactions (user_id, transaction_type, amount, recipient_phone, till_number, transaction_fee)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            stmt.setString(2, type);
            stmt.setDouble(3, amount);
            stmt.setString(4, recipient);
            stmt.setString(5, till);
            stmt.setDouble(6, fee);
            stmt.executeUpdate();
        }
    }

    private int getLastTransactionId(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private void recordCompanyIncome(Connection conn, int transactionId, double fee) throws SQLException {
        String sql = "INSERT INTO company_income (transaction_id, transaction_fee) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, transactionId);
            stmt.setDouble(2, fee);
            stmt.executeUpdate();
        }
    }

    private void showStatement() {
        String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY timestamp DESC LIMIT 10";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());

            StringBuilder statement = new StringBuilder("Recent Transactions:\n\n");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("transaction_type");
                    double amount = rs.getDouble("amount");
                    String timestamp = sdf.format(rs.getTimestamp("timestamp"));
                    String recipient = rs.getString("recipient_phone");
                    String till = rs.getString("till_number");
                    double fee = rs.getDouble("transaction_fee");

                    statement.append(timestamp).append(" - ").append(type)
                            .append(": KSh ").append(String.format("%.2f", amount));
                    if (recipient != null) {
                        statement.append(" to ").append(recipient);
                    }
                    if (till != null) {
                        statement.append(" (Till: ").append(till).append(")");
                    }
                    if (fee > 0) {
                        statement.append(" [Fee: KSh ").append(String.format("%.2f", fee)).append("]");
                    }
                    statement.append("\n");
                }
            }

            TextArea textArea = new TextArea(statement.toString());
            textArea.setEditable(false);
            textArea.getStyleClass().add("report-area");

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Bank Statement");
            alert.setHeaderText("Your Recent Transactions");
            alert.getDialogPane().setContent(textArea);
            alert.showAndWait();

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not retrieve statement", e);
            SceneNavigator.showError("Error", "Could not retrieve statement");
        }
    }
}
