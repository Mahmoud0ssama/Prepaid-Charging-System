package msc;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://ep-young-base-ah9o0a2g-pooler.c-3.us-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require";
    private static String USER = "neondb_owner";
    private static String PASSWORD = "npg_RzPrL5BNAga1";

    static {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(".env")) {
            props.load(fis);
            USER = props.getProperty("PGUSER", USER);
            PASSWORD = props.getProperty("PGPASSWORD", PASSWORD);
        } catch (IOException e) {
            if (System.getenv("PGUSER") != null) USER = System.getenv("PGUSER");
            if (System.getenv("PGPASSWORD") != null) PASSWORD = System.getenv("PGPASSWORD");
        }
    }

    protected static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    protected static boolean userExists(String msisdn) {
        String sql = "SELECT COUNT(*) FROM users WHERE msisdn = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, msisdn);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected static BigDecimal getBalance(String msisdn) {
        String sql = "SELECT balance FROM users WHERE msisdn = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, msisdn);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("balance");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    protected static void chargeMinute(String msisdn) {
        String sql = "UPDATE users SET balance = balance - 1 WHERE msisdn = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, msisdn);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected static void insertCDR(String msisdn, LocalDateTime startTime, LocalDateTime endTime,
                            int duration, String callResult, BigDecimal callCost,
                            BigDecimal balanceAfter, Connection conn) throws SQLException {
        String sql = "INSERT INTO cdrs (msisdn, start_time, end_time, duration, call_result, call_cost, balance_after_call) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, msisdn);
            stmt.setTimestamp(2, Timestamp.valueOf(startTime));
            stmt.setTimestamp(3, Timestamp.valueOf(endTime));
            stmt.setInt(4, duration);
            stmt.setString(5, callResult);
            stmt.setBigDecimal(6, callCost);
            stmt.setBigDecimal(7, balanceAfter);
            stmt.executeUpdate();
        }
    }
}