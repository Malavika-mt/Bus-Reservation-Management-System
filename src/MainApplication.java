import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/** Swing entry point; use --demo for the console presentation. */
public class MainApplication extends JFrame {
    private static final String ADMIN_CODE = "BUS-ADMIN";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private JPanel dashboard;

    private final JTextField loginEmail = new JTextField(22);
    private final JPasswordField loginPass = new JPasswordField(22);
    private final JComboBox<String> role = new JComboBox<>(new String[]{"Passenger", "Admin"});
    private final JTextField regName = new JTextField(22), regEmail = new JTextField(22), regPhone = new JTextField(22),
            regExtra = new JTextField(22), regCode = new JTextField(22);
    private final JPasswordField regPass = new JPasswordField(22), regConfirm = new JPasswordField(22);
    private final JLabel extraLabel = new JLabel("Age"), codeLabel = new JLabel("Admin access code");

    public static void main(String[] args) {
        boolean demo = args.length > 0 && args[0].equals("--demo");
        try {
            DatabaseConnection.initSchema();
        } catch (SQLException e) {
            String msg = "Cannot connect to the database:\n" + e.getMessage()
                    + "\n\nCheck db.properties (url / user / password), make sure MySQL is running\nand the JDBC driver jar is in the lib folder.";
            if (demo || GraphicsEnvironment.isHeadless()) System.err.println(msg);
            else JOptionPane.showMessageDialog(null, msg, "Database error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        if (demo) {
            try { new Demo().run(); }
            catch (Exception e) { System.err.println("Demo failed: " + e); System.exit(1); }
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
            new MainApplication().setVisible(true);
        });
    }

    public MainApplication() {
        super("Bus Reservation System");
        root.add(centered(loginPanel()), "login");
        root.add(centered(registerPanel()), "register");
        add(root);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1020, 660);
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null);
    }

    /** Puts a form in the middle of the window on a light background. */
    private JPanel centered(JPanel form) {
        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(Ui.BG);
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD0D7DE)),
                BorderFactory.createEmptyBorder(24, 36, 24, 36)));
        bg.add(form);
        return bg;
    }

    private JPanel titled(String title, String subtitle) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.insets = new Insets(0, 8, 2, 8);
        p.add(Ui.title(title), c);
        c.gridy = 1; c.insets = new Insets(0, 8, 14, 8);
        JLabel sub = new JLabel(subtitle);
        sub.setForeground(Color.GRAY);
        p.add(sub, c);
        return p;
    }

    private void buttonRow(JPanel p, int row, JButton... buttons) {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        r.setOpaque(false);
        for (JButton b : buttons) r.add(b);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row; c.gridx = 1; c.anchor = GridBagConstraints.WEST; c.insets = new Insets(14, 8, 0, 8);
        p.add(r, c);
    }

    private void row(JPanel p, int row, Component label, Component field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row; c.insets = new Insets(5, 8, 5, 8); c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; p.add(label, c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; p.add(field, c);
    }

    // Login
    private JPanel loginPanel() {
        JPanel p = titled("Bus Reservation System", "Sign in to search, book and manage tickets");
        loginEmail.setName("loginEmail"); loginPass.setName("loginPass");
        row(p, 2, new JLabel("Email"), loginEmail);
        row(p, 3, new JLabel("Password"), loginPass);
        JButton login = Ui.primary("Login"), toReg = new JButton("Create an account");
        login.setName("loginBtn"); toReg.setName("toRegister");
        login.addActionListener(e -> doLogin());
        loginPass.addActionListener(e -> doLogin());
        toReg.addActionListener(e -> cards.show(root, "register"));
        buttonRow(p, 4, login, toReg);
        return p;
    }

    private void doLogin() {
        try {
            String email = loginEmail.getText().trim();
            User user = email.isEmpty() ? null : DBUtils.findUserByEmail(email);
            if (user == null || !user.authenticate(new String(loginPass.getPassword()))) {
                Ui.error(this, "Invalid email or password.");
                return;
            }
            loginPass.setText("");
            showDashboard(user);
        } catch (SQLException e) {
            Ui.handle(this, e);
        }
    }

    // Registration
    private JPanel registerPanel() {
        JPanel p = titled("Create an account", "Passengers can book tickets; admins manage buses");
        regName.setName("regName"); regEmail.setName("regEmail"); regPhone.setName("regPhone");
        regPass.setName("regPass"); regConfirm.setName("regConfirm"); regExtra.setName("regExtra");
        regCode.setName("regCode"); role.setName("regRole");
        row(p, 2, new JLabel("Register as"), role);
        row(p, 3, new JLabel("Full name"), regName);
        row(p, 4, new JLabel("Email"), regEmail);
        row(p, 5, new JLabel("Phone (10 digits)"), regPhone);
        row(p, 6, new JLabel("Password"), regPass);
        row(p, 7, new JLabel("Confirm password"), regConfirm);
        row(p, 8, extraLabel, regExtra);
        row(p, 9, codeLabel, regCode);
        codeLabel.setVisible(false);
        regCode.setVisible(false);
        role.addActionListener(e -> {
            boolean admin = role.getSelectedItem().equals("Admin");
            extraLabel.setText(admin ? "Department" : "Age");
            codeLabel.setVisible(admin);
            regCode.setVisible(admin);
        });
        JButton reg = Ui.primary("Register"), back = new JButton("Back to login");
        reg.setName("registerBtn"); back.setName("toLogin");
        reg.addActionListener(e -> doRegister());
        back.addActionListener(e -> cards.show(root, "login"));
        buttonRow(p, 10, reg, back);
        return p;
    }

    private void doRegister() {
        try {
            String pw = new String(regPass.getPassword());
            if (!pw.equals(new String(regConfirm.getPassword()))) throw new IllegalArgumentException("Passwords do not match.");
            String email = regEmail.getText().trim().toLowerCase();
            if (DBUtils.emailExists(email)) throw new IllegalArgumentException("Email already registered.");

            User u;
            if (role.getSelectedItem().equals("Admin")) {
                if (!ADMIN_CODE.equals(regCode.getText().trim())) throw new IllegalArgumentException("Wrong admin access code.");
                u = new Admin(DBUtils.nextUserId("Admin"), regName.getText(), email, regPhone.getText(), pw, regExtra.getText(), 1);
            } else {
                int age;
                try { age = Integer.parseInt(regExtra.getText().trim()); }
                catch (NumberFormatException ex) { throw new IllegalArgumentException("Age must be a number."); }
                u = new Passenger(DBUtils.nextUserId("Passenger"), regName.getText(), email, regPhone.getText(), pw, age);
            }
            DBUtils.insertUser(u);
            Ui.info(this, "Registered! Your user ID is " + u.getUserId() + ".\nYou can log in now.");
            for (JTextField f : new JTextField[]{regName, regPhone, regPass, regConfirm, regExtra, regCode}) f.setText("");
            loginEmail.setText(email);
            cards.show(root, "login");
        } catch (IllegalArgumentException | SQLException ex) {
            Ui.handle(this, ex);
        }
    }

    // Dashboards
    private void showDashboard(User user) {
        if (dashboard != null) root.remove(dashboard);
        dashboard = user instanceof Admin a ? new AdminConsole(a, this::logout) : new PassengerConsole((Passenger) user, this::logout);
        root.add(dashboard, "dash");
        cards.show(root, "dash");
        root.revalidate();
    }

    private void logout() {
        if (dashboard != null) { root.remove(dashboard); dashboard = null; }
        cards.show(root, "login");
    }

    /** Runs a scripted demonstration of the modules. */
    static class Demo {
        void run() throws Exception {
            String tag = String.valueOf(System.currentTimeMillis() % 100000);
            BookingService svc = new BookingService();
            String date = DBUtils.findBus("KA-AC-101").getTravelDate();
            System.out.println("\n--- 1. Inheritance + runtime polymorphism: User references ---");
            User[] people = {
                new Passenger("D" + tag, "Demo Passenger", "demo" + tag + "@mail.com", "9123456780", "demo123", 25),
                new Admin("DA" + tag, "Demo Admin", "demoadmin" + tag + "@mail.com", "9123456781", "admin123", "Ops", 2)
            };
            for (User u : people) System.out.println(u.getRoleDetails() + "\n");

            System.out.println("--- 2. Encapsulation: invalid data is rejected by setters ---");
            try { people[0].setPhone("123"); } catch (IllegalArgumentException e) { System.out.println("Rejected: " + e.getMessage()); }

            System.out.println("\n--- 3. Persist the passenger in the database ---");
            Passenger p = (Passenger) people[0];
            DBUtils.insertUser(p);
            System.out.println("Saved " + p + " -> reloaded as " + DBUtils.findUserById(p.getUserId()));

            System.out.println("\n--- 4. Method overloading: three searchBus() + two calculateFare() ---");
            System.out.println("searchBus(\"Chennai\"):");              svc.searchBus("Chennai").forEach(System.out::println);
            System.out.println("searchBus(\"Bangalore\",\"Chennai\"):"); svc.searchBus("Bangalore", "Chennai").forEach(System.out::println);
            System.out.println("searchBus(\"Bangalore\",\"Chennai\",\"" + date + "\"):");
            svc.searchBus("Bangalore", "Chennai", date).forEach(System.out::println);
            svc.selectBus(DBUtils.findBus("KA-AC-101"));
            System.out.printf("calculateFare(2)        = Rs.%.2f%n", svc.calculateFare(2));
            System.out.printf("calculateFare(2, 10%%)   = Rs.%.2f%n", svc.calculateFare(2, 10));

            System.out.println("\n--- 5. Abstraction + overriding: ACBus vs NonACBus via Bus reference ---");
            for (Bus b : new Bus[]{DBUtils.findBus("KA-AC-101"), DBUtils.findBus("KA-NA-102")})
                System.out.printf("%s [%s] effective fare Rs.%.2f - %s%n", b.getBusNo(), b.getBusType(), b.getEffectiveFare(), b.getAmenities());

            System.out.println("\n--- 6. Runtime polymorphism: Payment reference, three child objects ---");
            Payment[] modes = { new UPIPayment("demo@okbank"), new CardPayment("4111 1111 1111 1111", "Demo Passenger", "12/30", "123"),
                                new CashPayment(1000) };
            for (Payment pay : modes) {
                Reservation r = svc.bookTicket(p, "KA-NA-102", 1, "", pay);
                System.out.println(svc.getLastReceipt());
                System.out.println("-> Reservation #" + r.getReservationId() + " confirmed via " + pay.getMode() + "\n");
            }

            System.out.println("--- 7. Custom exceptions & error handling ---");
            try { svc.bookTicket(p, "KA-NA-102", 1, "", new CardPayment("1234567812345678", "Demo Passenger", "12/30", "123")); }
            catch (PaymentFailedException e) { System.out.println("PaymentFailedException: " + e.getMessage()); }
            try { svc.bookTicket(p, "KA-NA-102", 9, "", new CashPayment(5000)); }
            catch (InvalidBookingException e) { System.out.println("InvalidBookingException: " + e.getMessage()); }
            try { svc.bookTicket(p, "NOPE-000", 1, "", new CashPayment(5000)); }
            catch (InvalidBookingException e) { System.out.println("InvalidBookingException: " + e.getMessage()); }
            System.out.println("Seats left on KA-NA-102 after failed payment (unchanged by it): " + DBUtils.findBus("KA-NA-102").getAvailableSeats());

            System.out.println("\n--- 8. Cancellation returns seats ---");
            for (Reservation res : DBUtils.listReservationsForUser(p.getUserId()))
                System.out.printf("Refund for #%d: Rs.%.2f%n", res.getReservationId(), svc.cancelBooking(p, res.getReservationId()));
            System.out.println("Seats on KA-NA-102 restored to: " + DBUtils.findBus("KA-NA-102").getAvailableSeats());
            System.out.println("Done. Run without --demo for the interactive menu.");
        }
    }
}
