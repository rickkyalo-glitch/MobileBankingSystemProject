package wire;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import wire.util.SceneNavigator;

public class AdminDashboardController {

    private static final Logger LOGGER = Logger.getLogger(AdminDashboardController.class.getName());

    @FXML private Button viewIncomeBtn;
    @FXML private Button registerUserBtn;
    @FXML private Button viewUsersBtn;
    @FXML private Button viewTransactionsBtn;
    @FXML private Button logoutBtn;
    @FXML private TextArea reportArea;

    @FXML
    private void handleViewIncome() {
        String incomeSql = "SELECT SUM(transaction_fee) AS total_income FROM company_income";
        String monthlySql = """
            SELECT strftime('%Y-%m', timestamp) AS month, SUM(transaction_fee) AS monthly_income
            FROM company_income GROUP BY month ORDER BY month DESC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement incomeStmt = conn.prepareStatement(incomeSql);
             PreparedStatement monthlyStmt = conn.prepareStatement(monthlySql)) {

            double totalIncome;
            try (ResultSet incomeRs = incomeStmt.executeQuery()) {
                totalIncome = incomeRs.next() ? incomeRs.getDouble("total_income") : 0;
            }

            StringBuilder report = new StringBuilder();
            report.append("=== COMPANY INCOME REPORT ===\n\n");
            report.append(String.format("Total Income: KSh %.2f\n\n", totalIncome));
            report.append("Monthly Breakdown:\n");

            try (ResultSet monthlyRs = monthlyStmt.executeQuery()) {
                while (monthlyRs.next()) {
                    report.append(String.format("%s: KSh %.2f%n",
                            monthlyRs.getString("month"), monthlyRs.getDouble("monthly_income")));
                }
            }

            reportArea.setText(report.toString());
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not generate income report", e);
            SceneNavigator.showError("Error", "Could not generate income report");
        }
    }

    @FXML
    private void handleRegisterUser() {
        SceneNavigator.switchScene(registerUserBtn, "register.fxml");
    }

    @FXML
    private void handleViewUsers() {
        String sql = "SELECT name, id_number, phone_number, balance, created_at FROM users ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            StringBuilder usersReport = new StringBuilder();
            usersReport.append("=== REGISTERED USERS ===\n\n");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            int count = 0;
            double totalBalance = 0;

            while (rs.next()) {
                count++;
                double balance = rs.getDouble("balance");
                totalBalance += balance;

                usersReport.append(String.format("User %d:%n", count));
                usersReport.append(String.format("  Name: %s%n", rs.getString("name")));
                usersReport.append(String.format("  ID: %s%n", rs.getString("id_number")));
                usersReport.append(String.format("  Phone: %s%n", rs.getString("phone_number")));
                usersReport.append(String.format("  Balance: KSh %.2f%n", balance));
                usersReport.append(String.format("  Registered: %s%n%n", sdf.format(rs.getTimestamp("created_at"))));
            }

            usersReport.append(String.format("Total Users: %d%n", count));
            usersReport.append(String.format("Total System Balance: KSh %.2f%n", totalBalance));

            reportArea.setText(usersReport.toString());
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not retrieve users list", e);
            SceneNavigator.showError("Error", "Could not retrieve users list");
        }
    }

    @FXML
    private void handleViewTransactions() {
        String sql = """
            SELECT t.transaction_type, t.amount, t.transaction_fee, t.timestamp,
                   u.name, u.phone_number, t.recipient_phone, t.till_number
            FROM transactions t
            JOIN users u ON t.user_id = u.id
            ORDER BY t.timestamp DESC
            LIMIT 20
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            StringBuilder report = new StringBuilder();
            report.append("=== RECENT TRANSACTIONS ===\n\n");

            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
            double totalVolume = 0;
            double totalFees = 0;

            while (rs.next()) {
                double amount = rs.getDouble("amount");
                double fee = rs.getDouble("transaction_fee");
                totalVolume += amount;
                totalFees += fee;

                report.append(String.format("[%s] %s: KSh %.2f",
                        sdf.format(rs.getTimestamp("timestamp")), rs.getString("transaction_type"), amount));
                report.append(String.format(" (User: %s - %s)", rs.getString("name"), rs.getString("phone_number")));

                String recipient = rs.getString("recipient_phone");
                String till = rs.getString("till_number");
                if (recipient != null) {
                    report.append(String.format(" \u2192 %s", recipient));
                }
                if (till != null) {
                    report.append(String.format(" [Till: %s]", till));
                }
                if (fee > 0) {
                    report.append(String.format(" [Fee: KSh %.2f]", fee));
                }
                report.append("\n");
            }

            report.append(String.format("%nTotal Volume: KSh %.2f%n", totalVolume));
            report.append(String.format("Total Fees: KSh %.2f%n", totalFees));

            reportArea.setText(report.toString());
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not retrieve transactions", e);
            SceneNavigator.showError("Error", "Could not retrieve transactions");
        }
    }

    @FXML
    private void handleLogout() {
        SceneNavigator.switchScene(logoutBtn, "login.fxml");
    }
}
