package msc;

import java.math.BigDecimal;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://ep-young-base-ah9o0a2g-pooler.c-3.us-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require";
    private static String USER = "neondb_owner";
    private static String PASSWORD = "npg_RzPrL5BNAga1";
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());
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

    public static BigDecimal queryBalance(String msisdn) { 
        
        String sql  = "SELECT balance FROM users WHERE msisdn = ? " ; 
        try (Connection con = getConnection()){
            PreparedStatement stmt = con.prepareStatement(sql); 
            stmt.setString(1,msisdn);
            ResultSet rs = stmt.executeQuery(); 

            if(rs.next()){
                BigDecimal balance= rs.getBigDecimal("balance") ;
                logger.info("[DatabaseMAnager] balance = " + balance);
                return balance ;
            } else { 

                logger.warning( "[DatabaseMAnager] msisdn not found , returning null");

                return null ; 
            }
        } catch (Exception e){
            e.printStackTrace();
            return null ;
        }
    }

    public static boolean userExists(String msisdn) {
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

    public static BigDecimal getBalance(String msisdn) {
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

    public static void chargeMinute(String msisdn) {
        String sql = "UPDATE users SET balance = balance - 1 WHERE msisdn = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, msisdn);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void insertCDR(String msisdn, LocalDateTime startTime, LocalDateTime endTime,
                                 int duration, String callResult, BigDecimal callCost,
                                 BigDecimal balanceAfter, Connection conn) throws SQLException {
        // Detect whether the cdrs table uses `duration` or `duration_minutes` column
        String durCol = "duration";
        try (ResultSet cols = conn.getMetaData().getColumns(null, null, "cdrs", "%")) {
            boolean hasDurationMinutes = false;
            while (cols.next()) {
                String colName = cols.getString("COLUMN_NAME");
                if ("duration_minutes".equalsIgnoreCase(colName)) { hasDurationMinutes = true; break; }
            }
            if (hasDurationMinutes) durCol = "duration_minutes";
        }

        String sql = String.format(
                "INSERT INTO cdrs (msisdn, start_time, end_time, %s, call_result, call_cost, balance_after_call) VALUES (?, ?, ?, ?, ?, ?, ?)",
                durCol);
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

    public static class CDR {
        public String msisdn;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public int duration;
        public String callResult;
        public BigDecimal callCost;
        public BigDecimal balanceAfterCall;

        public CDR(String msisdn, LocalDateTime startTime, LocalDateTime endTime, int duration,
                   String callResult, BigDecimal callCost, BigDecimal balanceAfterCall) {
            this.msisdn = msisdn;
            this.startTime = startTime;
            this.endTime = endTime;
            this.duration = duration;
            this.callResult = callResult;
            this.callCost = callCost;
            this.balanceAfterCall = balanceAfterCall;
        }
    }

    public static class User {
        public int id;
        public String msisdn;
        public BigDecimal balance;

        public User(int id, String msisdn, BigDecimal balance) {
            this.id = id;
            this.msisdn = msisdn;
            this.balance = balance;
        }
    }

    public static List<CDR> getAllCDRs() {
        List<CDR> cdrs = new ArrayList<>();
        String sql = "SELECT * FROM cdrs";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            java.sql.ResultSetMetaData md = rs.getMetaData();
            int colCount = md.getColumnCount();
            boolean hasDuration = false;
            boolean hasDurationMinutes = false;
            for (int i = 1; i <= colCount; i++) {
                String name = md.getColumnLabel(i);
                if ("duration".equalsIgnoreCase(name)) hasDuration = true;
                if ("duration_minutes".equalsIgnoreCase(name)) hasDurationMinutes = true;
            }
            String durCol = hasDuration ? "duration" : (hasDurationMinutes ? "duration_minutes" : "duration");
            while (rs.next()) {
                int dur = rs.getInt(durCol);
                CDR cdr = new CDR(
                        rs.getString("msisdn"),
                        rs.getTimestamp("start_time").toLocalDateTime(),
                        rs.getTimestamp("end_time").toLocalDateTime(),
                        dur,
                        rs.getString("call_result"),
                        rs.getBigDecimal("call_cost"),
                        rs.getBigDecimal("balance_after_call")
                );
                cdrs.add(cdr);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cdrs;
    }
    // need to make  git by msisdn 

    public static List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            java.sql.ResultSetMetaData md = rs.getMetaData();
            int colCount = md.getColumnCount();
            boolean hasId = false;
            for (int i = 1; i <= colCount; i++) {
                String name = md.getColumnLabel(i);
                if ("id".equalsIgnoreCase(name)) { hasId = true; break; }
            }
            while (rs.next()) {
                User user = new User(
                        (hasId ? rs.getInt("id") : 0),
                        rs.getString("msisdn"),
                        rs.getBigDecimal("balance")
                );
                users.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public static boolean createUser(String msisdn, BigDecimal balance) {
        String sql = "INSERT INTO users (msisdn, balance) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, msisdn);
            stmt.setBigDecimal(2, balance);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateUser(int id, String msisdn, BigDecimal balance) {
        String sql = "UPDATE users SET msisdn = ?, balance = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, msisdn);
            stmt.setBigDecimal(2, balance);
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateBalanceByMsisdn(String msisdn, BigDecimal balance) {
        String sql = "UPDATE users SET balance = ? WHERE msisdn = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, balance);
            stmt.setString(2, msisdn);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteUser(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}