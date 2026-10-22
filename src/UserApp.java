import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Standalone Swing login and registration application. */
public class UserApp extends JFrame {
    private static final String ADMIN_CODE = "BUS-ADMIN"; // needed to register an Admin

    private final List<User> users = new ArrayList<>();
    private int passengerCount = 0, adminCount = 1;
    private User current;

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private JPanel dashboard;

    // login fields
    private final JTextField loginEmail = new JTextField(20);
    private final JPasswordField loginPass = new JPasswordField(20);
    // register fields
    private final JComboBox<String> role = new JComboBox<>(new String[]{"Passenger", "Admin"});
    private final JTextField regName = new JTextField(20), regEmail = new JTextField(20),
            regPhone = new JTextField(20), regExtra = new JTextField(20), regCode = new JTextField(20);
    private final JPasswordField regPass = new JPasswordField(20), regConfirm = new JPasswordField(20);
    private final JLabel extraLabel = new JLabel("Age"), codeLabel = new JLabel("Admin access code");

    public UserApp() {
        super("Bus Reservation System - User Module");
        users.add(new Admin("A001", "System Admin", "admin@bus.com", "9876543210", "admin123", "Operations", 3));
        root.add(loginPanel(), "login");
        root.add(registerPanel(), "register");
        add(root);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(560, 520);
        setLocationRelativeTo(null);
    }

    // Login
    private JPanel loginPanel() {
        JPanel p = form("Login");
        addRow(p, 1, "Email", loginEmail);
        addRow(p, 2, "Password", loginPass);
        JButton login = new JButton("Login"), toReg = new JButton("Create an account");
        login.addActionListener(e -> doLogin());
        toReg.addActionListener(e -> cards.show(root, "register"));
        addRow(p, 3, "", login);
        addRow(p, 4, "", toReg);
        return p;
    }

    private void doLogin() {
        String email = loginEmail.getText().trim().toLowerCase();
        String pw = new String(loginPass.getPassword());
        for (User u : users) {
            if (u.getEmail().equals(email) && u.authenticate(pw)) {
                current = u;
                loginPass.setText("");
                showDashboard();
                return;
            }
        }
        error("Invalid email or password.");
    }

    // Registration
    private JPanel registerPanel() {
        JPanel p = form("Register");
        addRow(p, 1, "Register as", role);
        addRow(p, 2, "Full name", regName);
        addRow(p, 3, "Email", regEmail);
        addRow(p, 4, "Phone", regPhone);
        addRow(p, 5, "Password", regPass);
        addRow(p, 6, "Confirm password", regConfirm);
        addRow(p, 7, extraLabel, regExtra);
        addRow(p, 8, codeLabel, regCode);
        codeLabel.setVisible(false);
        regCode.setVisible(false);
        role.addActionListener(e -> {
            boolean admin = role.getSelectedItem().equals("Admin");
            extraLabel.setText(admin ? "Department" : "Age");
            codeLabel.setVisible(admin);
            regCode.setVisible(admin);
        });
        JButton reg = new JButton("Register"), back = new JButton("Back to login");
        reg.addActionListener(e -> doRegister());
        back.addActionListener(e -> cards.show(root, "login"));
        addRow(p, 9, "", reg);
        addRow(p, 10, "", back);
        return p;
    }

    private void doRegister() {
        try {
            String pw = new String(regPass.getPassword());
            if (!pw.equals(new String(regConfirm.getPassword())))
                throw new IllegalArgumentException("Passwords do not match.");
            String email = regEmail.getText().trim().toLowerCase();
            for (User u : users)
                if (u.getEmail().equals(email)) throw new IllegalArgumentException("Email already registered.");

            User u;
            if (role.getSelectedItem().equals("Admin")) {
                if (!ADMIN_CODE.equals(regCode.getText().trim()))
                    throw new IllegalArgumentException("Wrong admin access code.");
                u = new Admin(String.format("A%03d", adminCount + 1), regName.getText(), email,
                        regPhone.getText(), pw, regExtra.getText(), 1);
                adminCount++;
            } else {
                int age;
                try { age = Integer.parseInt(regExtra.getText().trim()); }
                catch (NumberFormatException ex) { throw new IllegalArgumentException("Age must be a number."); }
                u = new Passenger(String.format("P%03d", passengerCount + 1), regName.getText(), email,
                        regPhone.getText(), pw, age);
                passengerCount++;
            }
            users.add(u);
            JOptionPane.showMessageDialog(this, "Registered! Your ID is " + u.getUserId());
            for (JTextField f : new JTextField[]{regName, regEmail, regPhone, regPass, regConfirm, regExtra, regCode}) f.setText("");
            cards.show(root, "login");
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    // Dashboard
    private void showDashboard() {
        if (dashboard != null) root.remove(dashboard);
        dashboard = new JPanel(new BorderLayout(10, 10));
        dashboard.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel hello = new JLabel("Welcome, " + current.getName() + " (" + current.getRole() + ")");
        hello.setFont(hello.getFont().deriveFont(Font.BOLD, 16f));
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> { current = null; cards.show(root, "login"); });
        JPanel top = new JPanel(new BorderLayout());
        top.add(hello, BorderLayout.WEST);
        top.add(logout, BorderLayout.EAST);
        dashboard.add(top, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        JTextArea details = new JTextArea(current.getRoleDetails()); // polymorphic call
        details.setEditable(false);
        details.setMargin(new Insets(8, 8, 8, 8));
        tabs.addTab("My Role", new JScrollPane(details));
        tabs.addTab("My Profile", profileTab());
        if (current instanceof Admin) tabs.addTab("All Users", usersTab());
        dashboard.add(tabs, BorderLayout.CENTER);

        root.add(dashboard, "dash");
        cards.show(root, "dash");
    }

    private JPanel profileTab() {
        JPanel p = new JPanel(new GridBagLayout());
        JTextField name = new JTextField(current.getName(), 20), phone = new JTextField(current.getPhone(), 20);
        JPasswordField newPw = new JPasswordField(20);
        addRow(p, 0, "Name", name);
        addRow(p, 1, "Phone", phone);
        addRow(p, 2, "New password (optional)", newPw);
        JButton save = new JButton("Save changes");
        save.addActionListener(e -> {
            try {
                current.setName(name.getText());
                current.setPhone(phone.getText());
                String pw = new String(newPw.getPassword());
                if (!pw.isEmpty()) current.setPassword(pw);
                newPw.setText("");
                JOptionPane.showMessageDialog(this, "Profile updated.");
                showDashboard();
            } catch (IllegalArgumentException ex) { error(ex.getMessage()); }
        });
        addRow(p, 3, "", save);
        return p;
    }

    private JPanel usersTab() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Role", "Name", "Email", "Phone"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (User u : users) model.addRow(new Object[]{u.getUserId(), u.getRole(), u.getName(), u.getEmail(), u.getPhone()});
        JTable table = new JTable(model);
        JButton remove = new JButton("Remove selected user");
        remove.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { error("Select a user first."); return; }
            User target = users.get(row);
            if (target == current) { error("You cannot remove your own account."); return; }
            users.remove(target);
            showDashboard();
        });
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        p.add(remove, BorderLayout.SOUTH);
        return p;
    }

    // Helpers
    private JPanel form(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        JLabel l = new JLabel(title);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 20f));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.insets = new Insets(10, 10, 14, 10);
        p.add(l, c);
        return p;
    }

    private void addRow(JPanel p, int row, String label, Component field) {
        addRow(p, row, new JLabel(label), field);
    }

    private void addRow(JPanel p, int row, JLabel label, Component field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row; c.insets = new Insets(4, 10, 4, 10); c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; p.add(label, c);
        c.gridx = 1; p.add(field, c);
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UserApp().setVisible(true));
    }
}
