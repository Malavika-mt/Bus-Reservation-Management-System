import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/** Creates JDBC connections and initializes the schema. */
public final class DatabaseConnection {
    private static Properties props;

    private DatabaseConnection() { }

    private static synchronized Properties config() {
        if (props == null) {
            Properties p = new Properties();
            // Check this folder, then its parent.
            String file = System.getProperty("db.config");
            if (file == null) file = new File("db.properties").exists() ? "db.properties" : "../db.properties";
            try (FileInputStream in = new FileInputStream(file)) {
                p.load(in);
            } catch (IOException e) {
                System.err.println("Note: could not read db.properties - using defaults/environment.");
            }
            // Environment variables override the file.
            if (System.getenv("DB_URL") != null) p.setProperty("db.url", System.getenv("DB_URL"));
            if (System.getenv("DB_USER") != null) p.setProperty("db.user", System.getenv("DB_USER"));
            if (System.getenv("DB_PASSWORD") != null) p.setProperty("db.password", System.getenv("DB_PASSWORD"));
            props = p;
        }
        return props;
    }

    /** Opens a connection; callers must close it. */
    public static Connection getConnection() throws SQLException {
        Properties p = config();
        ensureDriver(p.getProperty("db.url", "jdbc:mysql://localhost:3306/bus_reservation"));
        String url = p.getProperty("db.url", "jdbc:mysql://localhost:3306/bus_reservation");
        return DriverManager.getConnection(url, p.getProperty("db.user", "root"), p.getProperty("db.password", ""));
    }

    private static boolean driversLoaded = false;

    /** Loads a JDBC driver from lib when needed. */
    private static synchronized void ensureDriver(String url) {
        try { DriverManager.getDriver(url); return; } catch (SQLException ignored) { /* none registered yet */ }
        if (driversLoaded) return;
        driversLoaded = true;
        List<URL> urls = new ArrayList<>();
        for (String dir : new String[]{"lib", "../lib"}) {
            File[] jars = new File(dir).listFiles((d, n) -> n.toLowerCase().endsWith(".jar"));
            if (jars == null) continue;
            for (File j : jars) {
                try { urls.add(j.toURI().toURL()); } catch (IOException ignored) { }
            }
        }
        if (urls.isEmpty()) return;
        URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]), DatabaseConnection.class.getClassLoader());
        for (Driver d : ServiceLoader.load(Driver.class, loader)) {
            try { DriverManager.registerDriver(new DriverShim(d)); } catch (SQLException ignored) { }
        }
    }

    /** Adapts drivers loaded by a separate class loader. */
    private static final class DriverShim implements Driver {
        private final Driver d;
        DriverShim(Driver d) { this.d = d; }
        public Connection connect(String u, Properties i) throws SQLException { return d.connect(u, i); }
        public boolean acceptsURL(String u) throws SQLException { return d.acceptsURL(u); }
        public DriverPropertyInfo[] getPropertyInfo(String u, Properties i) throws SQLException { return d.getPropertyInfo(u, i); }
        public int getMajorVersion() { return d.getMajorVersion(); }
        public int getMinorVersion() { return d.getMinorVersion(); }
        public boolean jdbcCompliant() { return d.jdbcCompliant(); }
        public Logger getParentLogger() throws SQLFeatureNotSupportedException { return d.getParentLogger(); }
    }

    /** Creates tables and seeds initial data. */
    public static void initSchema() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS users ("
                + "user_id VARCHAR(10) PRIMARY KEY, role VARCHAR(10) NOT NULL, name VARCHAR(50) NOT NULL,"
                + "email VARCHAR(100) NOT NULL UNIQUE, phone VARCHAR(10) NOT NULL, password_hash CHAR(64) NOT NULL,"
                + "age INT NULL, loyalty_points INT NOT NULL DEFAULT 0, department VARCHAR(50) NULL, access_level INT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS buses ("
                + "bus_no VARCHAR(15) PRIMARY KEY, bus_type VARCHAR(10) NOT NULL, source VARCHAR(30) NOT NULL,"
                + "destination VARCHAR(30) NOT NULL, travel_date DATE NOT NULL, departure_time VARCHAR(5) NOT NULL,"
                + "total_seats INT NOT NULL, available_seats INT NOT NULL, fare_per_seat DECIMAL(10,2) NOT NULL,"
                + "CHECK (available_seats >= 0 AND available_seats <= total_seats))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS reservations ("
                + "reservation_id INT AUTO_INCREMENT PRIMARY KEY, user_id VARCHAR(10) NOT NULL, bus_no VARCHAR(15) NOT NULL,"
                + "seats INT NOT NULL, total_fare DECIMAL(10,2) NOT NULL, status VARCHAR(10) NOT NULL,"
                + "booked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, payment_mode VARCHAR(10) NOT NULL,"
                + "transaction_id VARCHAR(20) NOT NULL,"
                + "FOREIGN KEY (user_id) REFERENCES users(user_id), FOREIGN KEY (bus_no) REFERENCES buses(bus_no))");
        }
        seedIfEmpty();
        rollForwardPastBuses();
    }

    /** Moves expired buses without active bookings to a future date. */
    private static void rollForwardPastBuses() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("UPDATE buses SET travel_date = DATE_ADD(CURDATE(), INTERVAL 1 + (CRC32(bus_no) MOD 3) DAY) "
                + "WHERE travel_date < CURDATE() AND NOT EXISTS "
                + "(SELECT 1 FROM reservations r WHERE r.bus_no = buses.bus_no AND r.status = 'CONFIRMED')");
        }
    }

    private static void seedIfEmpty() throws SQLException {
        if (DBUtils.countUsers() == 0) {
            DBUtils.insertUser(new Admin("A001", "System Admin", "admin@bus.com", "9876543210", "admin123", "Operations", 3));
        }
        if (DBUtils.countBuses() == 0) {
            LocalDate d = LocalDate.now();
            DBUtils.insertBus(new ACBus("KA-AC-101", 40, "Bangalore", "Chennai", 600, d.plusDays(1).toString(), "21:30"));
            DBUtils.insertBus(new NonACBus("KA-NA-102", 50, "Bangalore", "Chennai", 350, d.plusDays(1).toString(), "07:15"));
            DBUtils.insertBus(new ACBus("KL-AC-201", 36, "Kochi", "Bangalore", 900, d.plusDays(2).toString(), "20:00"));
            DBUtils.insertBus(new NonACBus("KL-NA-202", 45, "Kochi", "Trivandrum", 220, d.plusDays(2).toString(), "09:00"));
            DBUtils.insertBus(new ACBus("TN-AC-301", 40, "Chennai", "Hyderabad", 750, d.plusDays(3).toString(), "19:45"));
            DBUtils.insertBus(new NonACBus("MH-NA-401", 50, "Mumbai", "Pune", 250, d.plusDays(1).toString(), "06:30"));
        }
    }
}
