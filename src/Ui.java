import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;

/** Shared Swing helpers. */
public final class Ui {
    public static final Color PRIMARY = new Color(0x1F4E79);
    public static final Color ACCENT = new Color(0x2E86C1);
    public static final Color BG = new Color(0xF4F6F8);
    public static final Color ERROR = new Color(0xB03A2E);
    public static final Color OK = new Color(0x1E8449);

    private Ui() { }

    public static void error(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Bus Reservation System", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Turns any exception (custom, validation, SQL) into a friendly dialog. */
    public static void handle(Component parent, Exception e) {
        String m = e.getMessage() == null ? e.toString() : e.getMessage();
        error(parent, e instanceof SQLException ? "Database error: " + m : m);
    }

    public static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    public static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(24);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(false);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setFont(t.getTableHeader().getFont().deriveFont(Font.BOLD));
        return t;
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 22f));
        l.setForeground(PRIMARY);
        return l;
    }

    public static JPanel form() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        return p;
    }

    public static void row(JPanel p, int row, String label, Component field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row; c.insets = new Insets(5, 8, 5, 8); c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; p.add(new JLabel(label), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; p.add(field, c);
    }

    public static JButton primary(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        return b;
    }

    /** Dark top bar with a welcome message on the left and a logout button on the right. */
    public static JPanel header(JLabel hello, JButton logout) {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(PRIMARY);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        hello.setForeground(Color.WHITE);
        hello.setFont(hello.getFont().deriveFont(Font.BOLD, 16f));
        bar.add(hello, BorderLayout.WEST);
        bar.add(logout, BorderLayout.EAST);
        return bar;
    }
}
