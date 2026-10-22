import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Administrator dashboard with role-gated actions. */
public class AdminConsole extends JPanel {
    private final Admin admin;
    private final BookingService service = new BookingService();

    private final DefaultTableModel busModel = Ui.model("Bus No", "Type", "From", "To", "Date", "Departs", "Seats left", "Base fare", "Charged fare");
    private final JTable busTable = Ui.table(busModel);
    private List<Bus> buses = new ArrayList<>();

    private final DefaultTableModel resModel = Ui.model("ID", "User", "Bus", "Route", "Date", "Seats", "Fare (Rs.)", "Status", "Payment");
    private final JTable resTable = Ui.table(resModel);
    private List<Reservation> reservations = new ArrayList<>();

    private final DefaultTableModel userModel = Ui.model("ID", "Role", "Name", "Email", "Phone");
    private final JTable userTable = Ui.table(userModel);
    private List<User> users = new ArrayList<>();

    public AdminConsole(Admin admin, Runnable onLogout) {
        this.admin = admin;
        setLayout(new BorderLayout());
        JLabel hello = new JLabel("Welcome, " + admin.getName() + "  |  Admin  |  Access level " + admin.getAccessLevel());
        JButton logout = new JButton("Logout");
        logout.setName("logout");
        logout.addActionListener(e -> onLogout.run());
        add(Ui.header(hello, logout), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Buses", busesTab());
        tabs.addTab("Reservations", reservationsTab());
        tabs.addTab("Users", usersTab());
        tabs.addTab("My Role", roleTab());
        tabs.addChangeListener(e -> {
            switch (tabs.getSelectedIndex()) {
                case 0 -> loadBuses();
                case 1 -> loadReservations();
                case 2 -> loadUsers();
                default -> { }
            }
        });
        add(tabs, BorderLayout.CENTER);
        loadBuses();
        loadReservations();
        loadUsers();
    }

    private void need(int level) {
        if (admin.getAccessLevel() < level)
            throw new SecurityException("Your access level (" + admin.getAccessLevel() + ") is not enough. Level " + level + " is required.");
    }

    private JPanel tab(JTable table, JButton... buttons) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        for (JButton b : buttons) bottom.add(b);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JButton button(String text, String name, Runnable action, boolean primary) {
        JButton b = primary ? Ui.primary(text) : new JButton(text);
        b.setName(name);
        b.addActionListener(e -> {
            try { action.run(); }
            catch (IllegalArgumentException | SecurityException | IllegalStateException ex) { Ui.handle(this, ex); }
        });
        return b;
    }

    // Buses
    private JPanel busesTab() {
        busTable.setName("busTable");
        return tab(busTable,
                button("Add bus", "addBus", this::addBus, true),
                button("Update fare", "updateFare", this::updateFare, false),
                button("Remove bus", "removeBus", this::removeBus, false),
                button("Refresh", "refreshBuses", this::loadBuses, false));
    }

    private void loadBuses() {
        try { buses = DBUtils.listBuses(); }
        catch (SQLException e) { Ui.handle(this, e); return; }
        busModel.setRowCount(0);
        for (Bus b : buses)
            busModel.addRow(new Object[]{b.getBusNo(), b.getBusType(), b.getSource(), b.getDestination(), b.getTravelDate(),
                    b.getDepartureTime(), b.getAvailableSeats() + " / " + b.getTotalSeats(),
                    String.format("%.2f", b.getFarePerSeat()), String.format("%.2f", b.getEffectiveFare())});
    }

    private Bus selectedBus() {
        int row = busTable.getSelectedRow();
        if (row < 0 || row >= buses.size()) throw new IllegalStateException("Select a bus first.");
        return buses.get(row);
    }

    private void addBus() {
        need(1);
        JTextField no = new JTextField(12), seats = new JTextField(12), from = new JTextField(12), to = new JTextField(12),
                fare = new JTextField(12), date = new JTextField(LocalDate.now().plusDays(1).toString(), 12), time = new JTextField("09:00", 12);
        no.setName("newBusNo"); seats.setName("newSeats"); from.setName("newFrom"); to.setName("newTo");
        fare.setName("newFare"); date.setName("newDate"); time.setName("newTime");
        JComboBox<String> type = new JComboBox<>(new String[]{"AC", "NonAC"});
        type.setName("newType");
        JPanel f = Ui.form();
        Ui.row(f, 0, "Bus number", no);
        Ui.row(f, 1, "Type", type);
        Ui.row(f, 2, "Total seats (1-" + Bus.MAX_SEATS + ")", seats);
        Ui.row(f, 3, "Source", from);
        Ui.row(f, 4, "Destination", to);
        Ui.row(f, 5, "Base fare per seat (Rs.)", fare);
        Ui.row(f, 6, "Travel date (yyyy-MM-dd)", date);
        Ui.row(f, 7, "Departure time (HH:mm)", time);
        while (true) {
            if (JOptionPane.showConfirmDialog(this, f, "Add bus", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                    != JOptionPane.OK_OPTION) return;
            try {
                int n;
                double fr;
                try { n = Integer.parseInt(seats.getText().trim()); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("Total seats must be a whole number."); }
                try { fr = Double.parseDouble(fare.getText().trim()); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("Fare must be a number."); }
                Bus bus;
                if (type.getSelectedItem().equals("AC"))
                    bus = new ACBus(no.getText(), n, from.getText(), to.getText(), fr, date.getText(), time.getText());
                else
                    bus = new NonACBus(no.getText(), n, from.getText(), to.getText(), fr, date.getText(), time.getText());
                DBUtils.insertBus(bus);
                Ui.info(this, "Bus " + bus.getBusNo() + " added.\nAmenities: " + bus.getAmenities());
                loadBuses();
                return;
            } catch (SQLException e) {
                boolean dup = e.getSQLState() != null && e.getSQLState().startsWith("23");
                Ui.error(this, dup ? "That bus number already exists." : "Database error: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                Ui.error(this, e.getMessage());
            }
        }
    }

    private void updateFare() {
        need(2);
        Bus b = selectedBus();
        String in = JOptionPane.showInputDialog(this, "New base fare for " + b.getBusNo() + " (Rs.):", b.getFarePerSeat());
        if (in == null) return;
        try {
            DBUtils.updateFare(b.getBusNo(), Double.parseDouble(in.trim()));
            loadBuses();
        } catch (NumberFormatException e) {
            Ui.error(this, "Fare must be a number.");
        } catch (SQLException | IllegalArgumentException e) {
            Ui.handle(this, e);
        }
    }

    private void removeBus() {
        need(2);
        Bus b = selectedBus();
        if (JOptionPane.showConfirmDialog(this, "Remove bus " + b.getBusNo() + "?", "Confirm", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;
        try {
            DBUtils.deleteBus(b.getBusNo());
            loadBuses();
        } catch (SQLException | InvalidBookingException e) {
            Ui.handle(this, e);
        }
    }

    // Reservations
    private JPanel reservationsTab() {
        resTable.setName("resTable");
        return tab(resTable,
                button("Cancel selected reservation", "cancelRes", this::cancelReservation, true),
                button("Refresh", "refreshRes", this::loadReservations, false));
    }

    private void loadReservations() {
        try { reservations = DBUtils.listAllReservations(); }
        catch (SQLException e) { Ui.handle(this, e); return; }
        resModel.setRowCount(0);
        for (Reservation r : reservations)
            resModel.addRow(new Object[]{r.getReservationId(), r.getUserId(), r.getBusNo(), r.getRoute(), r.getTravelDate(),
                    r.getSeats(), String.format("%.2f", r.getTotalFare()), r.getStatus(), r.getPaymentMode()});
    }

    private void cancelReservation() {
        need(2);
        int row = resTable.getSelectedRow();
        if (row < 0 || row >= reservations.size()) throw new IllegalStateException("Select a reservation first.");
        Reservation r = reservations.get(row);
        if (JOptionPane.showConfirmDialog(this, "Cancel reservation #" + r.getReservationId() + "?", "Confirm", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;
        try {
            double refund = service.cancelBooking(admin, r.getReservationId());
            Ui.info(this, String.format("Reservation #%d cancelled. Refund due: Rs.%.2f", r.getReservationId(), refund));
        } catch (SQLException | InvalidBookingException e) {
            Ui.handle(this, e);
        }
        loadReservations();
        loadBuses();
    }

    // Users
    private JPanel usersTab() {
        userTable.setName("userTable");
        return tab(userTable,
                button("Remove selected user", "removeUser", this::removeUser, true),
                button("Refresh", "refreshUsers", this::loadUsers, false));
    }

    private void loadUsers() {
        try { users = DBUtils.listUsers(); }
        catch (SQLException e) { Ui.handle(this, e); return; }
        userModel.setRowCount(0);
        for (User u : users)
            userModel.addRow(new Object[]{u.getUserId(), u.getRole(), u.getName(), u.getEmail(), u.getPhone()});
    }

    private void removeUser() {
        need(3);
        int row = userTable.getSelectedRow();
        if (row < 0 || row >= users.size()) throw new IllegalStateException("Select a user first.");
        User u = users.get(row);
        if (u.getUserId().equals(admin.getUserId())) throw new IllegalStateException("You cannot remove your own account.");
        if (JOptionPane.showConfirmDialog(this, "Remove user " + u.getName() + " (" + u.getUserId() + ")?", "Confirm",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            DBUtils.deleteUser(u.getUserId());
            loadUsers();
        } catch (SQLException | InvalidBookingException e) {
            Ui.handle(this, e);
        }
    }

    // Role
    private JPanel roleTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        User u = admin;
        JTextArea area = new JTextArea(u.getRoleDetails());
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setMargin(new Insets(10, 10, 10, 10));
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        return p;
    }
}
