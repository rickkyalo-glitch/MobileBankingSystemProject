package wire;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import wire.util.PasswordUtil;

public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static final String URL = "jdbc:sqlite:wire.db";

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        // SQLite does not enforce FK constraints unless told to, per connection.
        try (Statement pragma = conn.createStatement()) {
            pragma.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    public static void initializeDatabase() {
        LOGGER.info("Initializing database...");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    id_number TEXT UNIQUE NOT NULL,
                    phone_number TEXT UNIQUE NOT NULL,
                    pin TEXT NOT NULL,
                    balance REAL DEFAULT 0.00,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS admins (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    transaction_type TEXT NOT NULL,
                    amount REAL NOT NULL,
                    recipient_phone TEXT,
                    till_number TEXT,
                    transaction_fee REAL DEFAULT 0.00,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS savings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    interest_rate REAL DEFAULT 5.00,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS company_income (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    transaction_id INTEGER NOT NULL,
                    transaction_fee REAL NOT NULL,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (transaction_id) REFERENCES transactions(id)
                )
            """);

            seedDefaultAdmin(conn, stmt);

            LOGGER.info("Database initialized successfully.");
        } catch (SQLException e) {
            // The original code printed the stack trace and moved on, which
            // leaves the app running against a database that may not have its
            // tables. Fail loudly instead — nothing works correctly from here.
            LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
            throw new IllegalStateException("Could not initialize the database", e);
        }
    }

    private static void seedDefaultAdmin(Connection conn, Statement stmt) throws SQLException {
        // Bug fix: the original code called rs.getInt(1) without first calling
        // rs.next() — reading a ResultSet before advancing its cursor is
        // undefined per the JDBC contract and threw on some drivers.
        try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM admins WHERE username = 'admin'")) {
            rs.next();
            if (rs.getInt(1) == 0) {
                String hashedPassword = PasswordUtil.hash("admin123".toCharArray());
                try (PreparedStatement insert =
                        conn.prepareStatement("INSERT INTO admins (username, password) VALUES ('admin', ?)")) {
                    insert.setString(1, hashedPassword);
                    insert.executeUpdate();
                }
                LOGGER.info("Default admin created: username='admin', password='admin123' (change this after first login)");
            }
        }
    }
}
