import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Database operations for users, buses, and reservations. */
public final class DBUtils {
    private DBUtils() { }

    // Users
    public static int countUsers() throws SQLException {
        return scalar("SELECT COUNT(*) FROM users");
    }

    public static int countBuses() throws SQLException {
        return scalar("SELECT COUNT(*) FROM buses");
    }

    private static int scalar(String sql) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Returns the next ID for a role. */
    public static String nextUserId(String role) throws SQLException {
        String prefix = role.equals("Admin") ? "A" : "P";
        String sql = "SELECT MAX(CAST(SUBSTRING(user_id, 2) AS UNSIGNED)) FROM users WHERE role = ? AND user_id REGEXP '^[AP][0-9]+$'";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return String.format("%s%03d", prefix, rs.getInt(1) + 1);
            }
        }
    }

    public static boolean emailExists(String email) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE email = ?")) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public static void insertUser(User u) throws SQLException {
        String sql = "INSERT INTO users (user_id, role, name, email, phone, password_hash, age, loyalty_points, department, access_level)"
                   + " VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getUserId());
            ps.setString(2, u.getRole());
            ps.setString(3, u.getName());
            ps.setString(4, u.getEmail());
            ps.setString(5, u.getPhone());
            ps.setString(6, u.getPasswordHash());
            if (u instanceof Passenger p) {
                ps.setInt(7, p.getAge());
                ps.setInt(8, p.getLoyaltyPoints());
                ps.setNull(9, Types.VARCHAR);
                ps.setNull(10, Types.INTEGER);
            } else if (u instanceof Admin a) {
                ps.setNull(7, Types.INTEGER);
                ps.setInt(8, 0);
                ps.setString(9, a.getDepartment());
                ps.setInt(10, a.getAccessLevel());
            }
            ps.executeUpdate();
        }
    }

    /** Saves editable user details. */
    public static void updateUser(User u) throws SQLException {
        String sql = "UPDATE users SET name=?, phone=?, password_hash=?, age=?, department=? WHERE user_id=?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getName());
            ps.setString(2, u.getPhone());
            ps.setString(3, u.getPasswordHash());
            if (u instanceof Passenger p) ps.setInt(4, p.getAge()); else ps.setNull(4, Types.INTEGER);
            if (u instanceof Admin a) ps.setString(5, a.getDepartment()); else ps.setNull(5, Types.VARCHAR);
            ps.setString(6, u.getUserId());
            ps.executeUpdate();
        }
    }

    public static User findUserByEmail(String email) throws SQLException {
        return findUser("SELECT * FROM users WHERE email = ?", email.trim().toLowerCase());
    }

    public static User findUserById(String id) throws SQLException {
        return findUser("SELECT * FROM users WHERE user_id = ?", id);
    }

    private static User findUser(String sql, String key) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapUser(rs) : null; }
        }
    }

    public static List<User> listUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM users ORDER BY role, user_id")) {
            while (rs.next()) list.add(mapUser(rs));
        }
        return list;
    }

    /** Deletes a user; refuses if the user still has bookings (foreign key). */
    public static void deleteUser(String userId) throws SQLException, InvalidBookingException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
            ps.setString(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (isConstraintViolation(e))
                throw new InvalidBookingException("User has booking history and cannot be removed.", e);
            throw e;
        }
    }

    private static User mapUser(ResultSet rs) throws SQLException {
        User u;
        if (rs.getString("role").equals("Admin")) {
            u = new Admin(rs.getString("user_id"), rs.getString("name"), rs.getString("email"), rs.getString("phone"),
                    "temp123", rs.getString("department"), rs.getInt("access_level"));
        } else {
            Passenger p = new Passenger(rs.getString("user_id"), rs.getString("name"), rs.getString("email"),
                    rs.getString("phone"), "temp123", rs.getInt("age"));
            int pts = rs.getInt("loyalty_points");
            if (pts > 0) p.addLoyaltyPoints(pts);
            u = p;
        }
        u.restorePasswordHash(rs.getString("password_hash")); // replace the placeholder with the stored hash
        return u;
    }

    public static void addLoyaltyPoints(Connection c, String userId, int delta) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET loyalty_points = GREATEST(0, loyalty_points + ?) WHERE user_id = ? AND role = 'Passenger'")) {
            ps.setInt(1, delta);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    // Buses
    public static void insertBus(Bus b) throws SQLException {
        String sql = "INSERT INTO buses (bus_no, bus_type, source, destination, travel_date, departure_time,"
                   + " total_seats, available_seats, fare_per_seat) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, b.getBusNo());
            ps.setString(2, b.getBusType());
            ps.setString(3, b.getSource());
            ps.setString(4, b.getDestination());
            ps.setString(5, b.getTravelDate());
            ps.setString(6, b.getDepartureTime());
            ps.setInt(7, b.getTotalSeats());
            ps.setInt(8, b.getAvailableSeats());
            ps.setDouble(9, b.getFarePerSeat());
            ps.executeUpdate();
        }
    }

    public static Bus findBus(String busNo) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            return findBus(c, busNo, false);
        }
    }

    /** Optionally locks the bus row for a transaction. */
    public static Bus findBus(Connection c, String busNo, boolean lock) throws SQLException {
        String sql = "SELECT * FROM buses WHERE bus_no = ?" + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, busNo.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapBus(rs) : null; }
        }
    }

    public static List<Bus> listBuses() throws SQLException {
        return queryBuses("SELECT * FROM buses ORDER BY travel_date, departure_time");
    }

    /** Finds upcoming buses with seats available; blank filters are ignored. */
    public static List<Bus> searchBuses(String source, String destination, String travelDate) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM buses WHERE available_seats > 0 AND travel_date >= CURDATE()");
        List<String> args = new ArrayList<>();
        if (source != null && !source.isBlank()) { sql.append(" AND LOWER(source) = LOWER(?)"); args.add(source.trim()); }
        if (destination != null && !destination.isBlank()) { sql.append(" AND LOWER(destination) = LOWER(?)"); args.add(destination.trim()); }
        if (travelDate != null && !travelDate.isBlank()) { sql.append(" AND travel_date = ?"); args.add(travelDate.trim()); }
        sql.append(" ORDER BY travel_date, departure_time");
        return queryBuses(sql.toString(), args.toArray(new String[0]));
    }

    private static List<Bus> queryBuses(String sql, String... args) throws SQLException {
        List<Bus> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setString(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapBus(rs)); }
        }
        return list;
    }

    public static boolean updateFare(String busNo, double fare) throws SQLException {
        Bus.validateFarePerSeat(fare);
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE buses SET fare_per_seat = ? WHERE bus_no = ?")) {
            ps.setDouble(1, fare);
            ps.setString(2, busNo.trim().toUpperCase());
            return ps.executeUpdate() > 0;
        }
    }

    /** Removes a bus; refuses if reservations reference it. */
    public static boolean deleteBus(String busNo) throws SQLException, InvalidBookingException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM buses WHERE bus_no = ?")) {
            ps.setString(1, busNo.trim().toUpperCase());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (isConstraintViolation(e))
                throw new InvalidBookingException("Bus has reservations and cannot be removed.", e);
            throw e;
        }
    }

    private static Bus mapBus(ResultSet rs) throws SQLException {
        String type = rs.getString("bus_type");
        String no = rs.getString("bus_no"), src = rs.getString("source"), dst = rs.getString("destination");
        String date = rs.getString("travel_date"), time = rs.getString("departure_time");
        int total = rs.getInt("total_seats"), avail = rs.getInt("available_seats");
        double fare = rs.getDouble("fare_per_seat");
        return type.equals("AC") ? new ACBus(no, total, avail, src, dst, fare, date, time)
                                 : new NonACBus(no, total, avail, src, dst, fare, date, time);
    }

    /** Atomically reserves seats without allowing overselling. */
    public static void decrementSeats(Connection c, String busNo, int seats) throws SQLException, SeatUnavailableException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE buses SET available_seats = available_seats - ? WHERE bus_no = ? AND available_seats >= ?")) {
            ps.setInt(1, seats);
            ps.setString(2, busNo);
            ps.setInt(3, seats);
            if (ps.executeUpdate() == 0) {
                Bus b = findBus(c, busNo, false);
                int left = b == null ? 0 : b.getAvailableSeats();
                throw new SeatUnavailableException(left == 0 ? "Bus " + busNo + " is fully booked."
                        : "Only " + left + " seat(s) left on bus " + busNo + ".", seats, left);
            }
        }
    }

    public static void restoreSeats(Connection c, String busNo, int seats) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE buses SET available_seats = LEAST(total_seats, available_seats + ?) WHERE bus_no = ?")) {
            ps.setInt(1, seats);
            ps.setString(2, busNo);
            ps.executeUpdate();
        }
    }

    // Reservations
    public static int insertReservation(Connection c, Reservation r) throws SQLException {
        String sql = "INSERT INTO reservations (user_id, bus_no, seats, total_fare, status, payment_mode, transaction_id)"
                   + " VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getUserId());
            ps.setString(2, r.getBusNo());
            ps.setInt(3, r.getSeats());
            ps.setDouble(4, r.getTotalFare());
            ps.setString(5, r.getStatus());
            ps.setString(6, r.getPaymentMode());
            ps.setString(7, r.getTransactionId());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getInt(1);
            }
        }
    }

    private static final String RES_SELECT =
        "SELECT r.*, b.source, b.destination, b.travel_date FROM reservations r JOIN buses b ON r.bus_no = b.bus_no ";

    public static Reservation findReservation(Connection c, int id, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(RES_SELECT + "WHERE r.reservation_id = ?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapReservation(rs) : null; }
        }
    }

    public static List<Reservation> listReservationsForUser(String userId) throws SQLException {
        return queryReservations(RES_SELECT + "WHERE r.user_id = ? ORDER BY r.reservation_id DESC", userId);
    }

    public static List<Reservation> listAllReservations() throws SQLException {
        return queryReservations(RES_SELECT + "ORDER BY r.reservation_id DESC");
    }

    private static List<Reservation> queryReservations(String sql, String... args) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setString(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapReservation(rs)); }
        }
        return list;
    }

    public static void markCancelled(Connection c, int reservationId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE reservations SET status = 'CANCELLED' WHERE reservation_id = ?")) {
            ps.setInt(1, reservationId);
            ps.executeUpdate();
        }
    }

    private static Reservation mapReservation(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("booked_at");
        Reservation r = new Reservation(rs.getInt("reservation_id"), rs.getString("user_id"), rs.getString("bus_no"),
                rs.getInt("seats"), rs.getDouble("total_fare"), rs.getString("status"),
                ts == null ? LocalDateTime.now() : ts.toLocalDateTime(),
                rs.getString("payment_mode"), rs.getString("transaction_id"));
        r.setDisplayInfo(rs.getString("source") + " -> " + rs.getString("destination"), rs.getString("travel_date"));
        return r;
    }

    private static boolean isConstraintViolation(SQLException e) {
        return e instanceof SQLIntegrityConstraintViolationException
            || (e.getSQLState() != null && e.getSQLState().startsWith("23"));
    }
}
