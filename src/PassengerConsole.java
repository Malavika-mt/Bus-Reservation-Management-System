import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Passenger dashboard for search, booking, payments, and profile management. */
public class PassengerConsole extends JPanel {
    private Passenger passenger;
    private final BookingService service = new BookingService();
    private final JLabel hello = new JLabel();

    // Search and booking
    private final JTextField src = new JTextField(10), dst = new JTextField(10), date = new JTextField(9);
    private final DefaultTableModel busModel = Ui.model("Bus No", "Type", "From", "To", "Date", "Departs", "Seats left", "Fare / seat (Rs.)");
    private final JTable busTable = Ui.table(busModel);
    private List<Bus> shown = new ArrayList<>();
    private final JSpinner seats = new JSpinner(new SpinnerNumberModel(1, 1, BookingService.MAX_SEATS_PER_BOOKING, 1));
    private final JTextField promo = new JTextField(9);
    private final JLabel fareLabel = new JLabel("Select a bus to see the fare.");

    // Bookings
    private final DefaultTableModel resModel = Ui.model("ID", "Bus", "Route", "Date", "Seats", "Fare (Rs.)", "Status", "Payment", "Transaction");
    private final JTable resTable = Ui.table(resModel);
    private List<Reservation> myReservations = new ArrayList<>();

    // Role and profile
    private final JTextArea roleArea = new JTextArea();
    private final JTextField pName = new JTextField(20), pPhone = new JTextField(20), pAge = new JTextField(20);
    private final JPasswordField pPass = new JPasswordField(20);

    public PassengerConsole(Passenger passenger, Runnable onLogout) {
        this.passenger = passenger;
        setLayout(new BorderLayout());
        JButton logout = new JButton("Logout");
        logout.setName("logout");
        logout.addActionListener(e -> onLogout.run());
        add(Ui.header(hello, logout), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Search & Book", searchTab());
        tabs.addTab("My Bookings", bookingsTab());
        tabs.addTab("My Role", roleTab());
        tabs.addTab("My Profile", profileTab());
        tabs.addChangeListener(e -> { if (tabs.getSelectedIndex() == 1) loadBookings(); });
        add(tabs, BorderLayout.CENTER);

        refreshHeaderAndProfile();
        runSearch();
        loadBookings();
    }

    // Search and booking
    private JPanel searchTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        src.setName("src"); dst.setName("dst"); date.setName("date");
        date.setToolTipText("yyyy-MM-dd, e.g. " + java.time.LocalDate.now().plusDays(1));
        JButton search = Ui.primary("Search"), all = new JButton("Show all");
        search.setName("search");
        search.addActionListener(e -> runSearch());
        all.addActionListener(e -> { src.setText(""); dst.setText(""); date.setText(""); runSearch(); });
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.add(new JLabel("From")); top.add(src);
        top.add(new JLabel("To")); top.add(dst);
        top.add(new JLabel("Date (yyyy-MM-dd)")); top.add(date);
        top.add(search); top.add(all);
        p.add(top, BorderLayout.NORTH);

        busTable.setName("busTable");
        busTable.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) updateFare(); });
        p.add(new JScrollPane(busTable), BorderLayout.CENTER);

        seats.setName("seats"); promo.setName("promo"); fareLabel.setName("fareLabel");
        seats.addChangeListener(e -> updateFare());
        promo.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateFare(); }
            public void removeUpdate(DocumentEvent e) { updateFare(); }
            public void changedUpdate(DocumentEvent e) { updateFare(); }
        });
        fareLabel.setFont(fareLabel.getFont().deriveFont(Font.BOLD, 14f));
        JButton book = Ui.primary("Book & Pay");
        book.setName("book");
        book.addActionListener(e -> book());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        bottom.add(new JLabel("Seats")); bottom.add(seats);
        bottom.add(new JLabel("Promo code")); bottom.add(promo);
        bottom.add(fareLabel);
        bottom.add(book);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    /** Selects a search overload from the filled fields. */
    private void runSearch() {
        String s = src.getText().trim(), d = dst.getText().trim(), dt = date.getText().trim();
        try {
            if (!dt.isEmpty()) shown = service.searchBus(s, d, dt);            // 3 arguments
            else if (!s.isEmpty()) shown = service.searchBus(s, d);            // 2 arguments
            else if (!d.isEmpty()) shown = service.searchBus(d);               // 1 argument
            else shown = DBUtils.searchBuses(null, null, null);
        } catch (SQLException | InvalidBookingException e) {
            Ui.handle(this, e);
            return;
        }
        busModel.setRowCount(0);
        for (Bus b : shown)
            busModel.addRow(new Object[]{b.getBusNo(), b.getBusType(), b.getSource(), b.getDestination(), b.getTravelDate(),
                    b.getDepartureTime(), b.getAvailableSeats() + " / " + b.getTotalSeats(), String.format("%.2f", b.getEffectiveFare())});
        updateFare();
    }

    private Bus selectedBus() {
        int row = busTable.getSelectedRow();
        return row < 0 || row >= shown.size() ? null : shown.get(row);
    }

    private void updateFare() {
        Bus b = selectedBus();
        if (b == null) { setFare("Select a bus to see the fare.", Color.DARK_GRAY); return; }
        try {
            double fare = service.quote(b.getBusNo(), (Integer) seats.getValue(), promo.getText());
            setFare(String.format("Total fare: Rs.%.2f", fare), Ui.OK);
        } catch (SQLException | InvalidBookingException | SeatUnavailableException e) {
            setFare(e.getMessage(), Ui.ERROR);
        }
    }

    private void setFare(String text, Color c) { fareLabel.setText(text); fareLabel.setForeground(c); }

    private void book() {
        Bus b = selectedBus();
        if (b == null) { Ui.info(this, "Please select a bus first."); return; }
        int n = (Integer) seats.getValue();
        String code = promo.getText();
        try {
            double fare = service.quote(b.getBusNo(), n, code);
            PaymentForm form = new PaymentForm();
            while (true) {
                Payment payment = form.ask(this, fare);
                if (payment == null) return;
                try {
                    Reservation r = service.bookTicket(passenger, b.getBusNo(), n, code, payment);
                    showReceipt(r);
                    break;
                } catch (PaymentFailedException e) {
                    Ui.error(this, "Payment failed: " + e.getMessage());
                }
            }
        } catch (SQLException | InvalidBookingException | SeatUnavailableException | IllegalArgumentException e) {
            Ui.handle(this, e);
        }
        promo.setText("");
        seats.setValue(1);
        refreshAfterChange();
    }

    private void showReceipt(Reservation r) {
        JTextArea area = new JTextArea(service.getLastReceipt() + "\n\nBOOKING CONFIRMED\nReservation #" + r.getReservationId()
                + "\n" + r.getRoute() + " on " + r.getTravelDate() + "\n" + r.getSeats() + " seat(s) on bus " + r.getBusNo());
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setEditable(false);
        area.setMargin(new Insets(8, 8, 8, 8));
        JOptionPane.showMessageDialog(this, area, "Booking confirmed", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Payment dialog: the passenger picks Card / UPI / Cash and the matching Payment child is built. */
    private static class PaymentForm extends JPanel {
        private final JComboBox<String> mode = new JComboBox<>(new String[]{"Card", "UPI", "Cash"});
        private final CardLayout cards = new CardLayout();
        private final JPanel stack = new JPanel(cards);
        private final JTextField cardNo = new JTextField(18), holder = new JTextField(18), expiry = new JTextField(6),
                cvv = new JTextField(5), upi = new JTextField(18), cash = new JTextField(10);

        PaymentForm() {
            setLayout(new BorderLayout(8, 8));
            mode.setName("payMode"); cardNo.setName("cardNo"); holder.setName("cardHolder");
            expiry.setName("cardExpiry"); cvv.setName("cardCvv"); upi.setName("upiId"); cash.setName("cashAmt");
            JPanel card = Ui.form();
            Ui.row(card, 0, "Card number", cardNo);
            Ui.row(card, 1, "Name on card", holder);
            Ui.row(card, 2, "Expiry (MM/yy)", expiry);
            Ui.row(card, 3, "CVV", cvv);
            JPanel upiP = Ui.form();
            Ui.row(upiP, 0, "UPI ID (name@bank)", upi);
            JPanel cashP = Ui.form();
            Ui.row(cashP, 0, "Cash tendered (Rs.)", cash);
            stack.add(card, "Card"); stack.add(upiP, "UPI"); stack.add(cashP, "Cash");
            mode.addActionListener(e -> cards.show(stack, (String) mode.getSelectedItem()));
            add(mode, BorderLayout.NORTH);
            add(stack, BorderLayout.CENTER);
        }

        Payment ask(Component parent, double fare) {
            int ok = JOptionPane.showConfirmDialog(parent, this, String.format("Payment - Rs.%.2f", fare),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return null;
            return build();
        }

        Payment build() {
            Payment p;
            switch ((String) mode.getSelectedItem()) {
                case "Card" -> p = new CardPayment(cardNo.getText(), holder.getText(), expiry.getText(), cvv.getText());
                case "UPI" -> p = new UPIPayment(upi.getText());
                default -> {
                    try { p = new CashPayment(Double.parseDouble(cash.getText().trim())); }
                    catch (NumberFormatException e) { p = new CashPayment(Double.NaN); }
                }
            }
            return p;
        }
    }

    // Bookings
    private JPanel bookingsTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        resTable.setName("resTable");
        p.add(new JScrollPane(resTable), BorderLayout.CENTER);
        JButton cancel = Ui.primary("Cancel selected booking"), refresh = new JButton("Refresh");
        cancel.setName("cancelBooking");
        cancel.addActionListener(e -> cancelSelected());
        refresh.addActionListener(e -> loadBookings());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        bottom.add(cancel); bottom.add(refresh);
        bottom.add(new JLabel("(" + (int) BookingService.CANCELLATION_FEE_PERCENT + "% cancellation fee applies)"));
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private void loadBookings() {
        try {
            myReservations = DBUtils.listReservationsForUser(passenger.getUserId());
        } catch (SQLException e) {
            Ui.handle(this, e);
            return;
        }
        resModel.setRowCount(0);
        for (Reservation r : myReservations)
            resModel.addRow(new Object[]{r.getReservationId(), r.getBusNo(), r.getRoute(), r.getTravelDate(), r.getSeats(),
                    String.format("%.2f", r.getTotalFare()), r.getStatus(), r.getPaymentMode(), r.getTransactionId()});
    }

    private void cancelSelected() {
        int row = resTable.getSelectedRow();
        if (row < 0 || row >= myReservations.size()) { Ui.info(this, "Select a booking first."); return; }
        Reservation r = myReservations.get(row);
        if (JOptionPane.showConfirmDialog(this, "Cancel booking #" + r.getReservationId() + "? A "
                + (int) BookingService.CANCELLATION_FEE_PERCENT + "% fee applies.", "Confirm", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;
        try {
            double refund = service.cancelBooking(passenger, r.getReservationId());
            Ui.info(this, String.format("Booking #%d cancelled.%nRefund: Rs.%.2f", r.getReservationId(), refund));
        } catch (SQLException | InvalidBookingException e) {
            Ui.handle(this, e);
        }
        refreshAfterChange();
    }

    // Role and profile
    private JPanel roleTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        roleArea.setEditable(false);
        roleArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        roleArea.setMargin(new Insets(10, 10, 10, 10));
        p.add(new JScrollPane(roleArea), BorderLayout.CENTER);
        return p;
    }

    private JPanel profileTab() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel f = Ui.form();
        pName.setName("pName"); pPhone.setName("pPhone"); pAge.setName("pAge"); pPass.setName("pPass");
        Ui.row(f, 0, "Name", pName);
        Ui.row(f, 1, "Phone", pPhone);
        Ui.row(f, 2, "Age", pAge);
        Ui.row(f, 3, "New password (optional)", pPass);
        JButton save = Ui.primary("Save changes");
        save.setName("saveProfile");
        save.addActionListener(e -> saveProfile());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 4; c.gridx = 1; c.anchor = GridBagConstraints.WEST; c.insets = new Insets(12, 8, 0, 8);
        f.add(save, c);
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wrap.add(f);
        outer.add(wrap, BorderLayout.NORTH);
        return outer;
    }

    private void saveProfile() {
        try {
            passenger.setName(pName.getText());
            passenger.setPhone(pPhone.getText());
            try { passenger.setAge(Integer.parseInt(pAge.getText().trim())); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Age must be a number."); }
            String pw = new String(pPass.getPassword());
            if (!pw.isEmpty()) passenger.setPassword(pw);
            DBUtils.updateUser(passenger);
            pPass.setText("");
            Ui.info(this, "Profile updated.");
        } catch (IllegalArgumentException | SQLException e) {
            Ui.handle(this, e);
            reloadPassenger();   // discard half-applied changes
        }
        refreshHeaderAndProfile();
    }

    // Refresh
    private void reloadPassenger() {
        try {
            User u = DBUtils.findUserById(passenger.getUserId());
            if (u instanceof Passenger p) passenger = p;
        } catch (SQLException ignored) { /* keep the in-memory copy */ }
    }

    private void refreshAfterChange() {
        reloadPassenger();
        refreshHeaderAndProfile();
        runSearch();
        loadBookings();
    }

    private void refreshHeaderAndProfile() {
        User u = passenger;
        hello.setText("Welcome, " + passenger.getName() + "  |  Passenger  |  Loyalty points: " + passenger.getLoyaltyPoints());
        roleArea.setText(u.getRoleDetails());
        roleArea.setCaretPosition(0);
        pName.setText(passenger.getName());
        pPhone.setText(passenger.getPhone());
        pAge.setText(String.valueOf(passenger.getAge()));
    }
}
