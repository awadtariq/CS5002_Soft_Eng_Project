import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * BookingSystem — Java Swing UI
 * Mirrors the Use Case Diagram:
 *   User: Register, Login, Renewal, Booking, Update Booking, Reserves,
 *         Attend, Confirm Availability, Take Payments, Cancellations,
 *         Process Refund, Rescheduling
 *   Admin/System: Payment Confirmed, Refund Rejected, Refund Confirmed,
 *                 Maintenance
 *
 * Run: javac BookingSystem.java && java BookingSystem
 */
public class BookingSystem {

    // ─── Palette ────────────────────────────────────────────────────────────────
    static final Color BG          = new Color(0xF4F6FB);
    static final Color SIDEBAR_BG  = new Color(0x1B2A4A);
    static final Color ACCENT      = new Color(0x3A7BFF);
    static final Color ACCENT2     = new Color(0x00C896);
    static final Color DANGER      = new Color(0xFF4D4F);
    static final Color WARN        = new Color(0xFFA940);
    static final Color CARD        = Color.WHITE;
    static final Color TEXT        = new Color(0x1B2A4A);
    static final Color MUTED       = new Color(0x8A94A6);
    static final Color BORDER      = new Color(0xDDE3EE);

    // ─── Shared state ────────────────────────────────────────────────────────────
    static String loggedInUser  = null;
    static boolean isAdmin      = false;
    static List<String[]> bookings = new ArrayList<>();   // {id, user, event, date, status, amount}
    static List<String[]> refunds  = new ArrayList<>();   // {bookingId, user, amount, status}
    static JFrame mainFrame;
    static JPanel contentArea;
    static JLabel statusBar;

    // ─── Users file ──────────────────────────────────────────────────────────────
    // Credential storage is fully handled by UserManager.java
    // users.txt format:  username:password:role

    // ─── Seed data ────────────────────────────────────────────────────────────────
    static {
        bookings.add(new String[]{"BK001","alice","Jazz Night",   "2026-04-10","Confirmed","£45.00"});
        bookings.add(new String[]{"BK002","bob",  "Art Expo",     "2026-04-15","Expired",  "£30.00"});
        bookings.add(new String[]{"BK003","alice","Tech Summit",  "2026-05-01","Confirmed","£120.00"});
        bookings.add(new String[]{"BK004","carol","Comedy Show",  "2026-03-20","Cancelled","£25.00"});
        refunds.add(new String[]{"BK004","carol","£25.00","Refund Confirmed"});
    }

    // ════════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        UserManager.loadUsers();
        SwingUtilities.invokeLater(BookingSystem::showLoginScreen);
    }

    // ─── LOGIN SCREEN ────────────────────────────────────────────────────────────
    static void showLoginScreen() {
        JFrame f = new JFrame("BookingSystem — Login");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(480, 560);
        f.setLocationRelativeTo(null);
        f.setResizable(false);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(BG);
        f.setContentPane(root);

        JPanel card = roundedCard(380, 460);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(36, 36, 36, 36));

        JLabel logo = new JLabel("🎟  BookingSystem");
        logo.setFont(new Font("SansSerif", Font.BOLD, 22));
        logo.setForeground(ACCENT);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Sign in to your account");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField userField = styledField("Username  (try: alice / admin)");
        JPasswordField passField = new JPasswordField();
        stylePasswordField(passField, "Password  (any)");

        JButton loginBtn = accentButton("Sign In", ACCENT);
        JButton registerBtn = ghostButton("Create Account");

        JLabel msg = new JLabel(" ");
        msg.setForeground(DANGER);
        msg.setFont(new Font("SansSerif", Font.PLAIN, 12));
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginBtn.addActionListener(e -> {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword()).trim();
            if (u.isEmpty() || p.isEmpty()) { msg.setText("Please fill in all fields."); return; }
            if (!UserManager.validateLogin(u, p)) {
                msg.setText("⚠ Incorrect username or password.");
                return;
            }
            loggedInUser = u;
            isAdmin = UserManager.isAdmin(u);
            f.dispose();
            openMainApp();
        });

        registerBtn.addActionListener(e -> { f.dispose(); showRegisterScreen(); });

        card.add(logo);
        card.add(Box.createVerticalStrut(6));
        card.add(sub);
        card.add(Box.createVerticalStrut(28));
        card.add(fieldLabel("Username"));
        card.add(Box.createVerticalStrut(4));
        card.add(userField);
        card.add(Box.createVerticalStrut(14));
        card.add(fieldLabel("Password"));
        card.add(Box.createVerticalStrut(4));
        card.add(passField);
        card.add(Box.createVerticalStrut(8));
        card.add(msg);
        card.add(Box.createVerticalStrut(8));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(10));
        card.add(registerBtn);

        root.add(card);
        f.setVisible(true);
    }

    // ─── REGISTER SCREEN ─────────────────────────────────────────────────────────
    static void showRegisterScreen() {
        JFrame f = new JFrame("BookingSystem — Register");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(480, 640);
        f.setLocationRelativeTo(null);
        f.setResizable(false);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(BG);
        f.setContentPane(root);

        JPanel card = roundedCard(400, 540);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));

        JLabel title = new JLabel("Create Account");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField nameF  = styledField("Full name");
        JTextField emailF = styledField("Email address");
        JTextField userF  = styledField("Choose username");
        JPasswordField passF  = new JPasswordField(); stylePasswordField(passF, "Password");
        JPasswordField pass2F = new JPasswordField(); stylePasswordField(pass2F, "Confirm password");

        JButton regBtn  = accentButton("Register", ACCENT2);
        JButton backBtn = ghostButton("← Back to Login");

        JLabel msg = new JLabel(" ");
        msg.setForeground(DANGER);
        msg.setFont(new Font("SansSerif", Font.PLAIN, 12));
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);

        regBtn.addActionListener(e -> {
            String n = nameF.getText().trim(), em = emailF.getText().trim(),
                    u = userF.getText().trim(), p  = new String(passF.getPassword()),
                    p2 = new String(pass2F.getPassword());
            if (n.isEmpty()||em.isEmpty()||u.isEmpty()||p.isEmpty()) {
                msg.setText("All fields are required."); return;
            }
            if (!p.equals(p2)) { msg.setText("Passwords do not match."); return; }
            if (!em.contains("@")) { msg.setText("Enter a valid email."); return; }
            if (UserManager.userExists(u)) { msg.setText("Username already taken."); return; }
            boolean ok = UserManager.registerUser(u, p, "user");
            if (ok) {
                msg.setForeground(ACCENT2);
                msg.setText("✓ Registration successful! Saved to users.txt");
                Timer t = new Timer(1400, ev -> { f.dispose(); showLoginScreen(); });
                t.setRepeats(false); t.start();
            } else {
                msg.setText("Registration failed. Try a different username.");
            }
        });

        backBtn.addActionListener(e -> { f.dispose(); showLoginScreen(); });

        card.add(title);
        card.add(Box.createVerticalStrut(22));
        for (String[] pair : new String[][]{
                {"Full Name", null}, {"Email", null},
                {"Username", null}, {"Password", null}, {"Confirm Password", null}}) {
            card.add(fieldLabel(pair[0]));
            card.add(Box.createVerticalStrut(4));
        }
        // rebuild properly
        card.removeAll();
        card.add(title);
        card.add(Box.createVerticalStrut(22));
        JComponent[][] rows = {
                {fieldLabel("Full Name"),     nameF},
                {fieldLabel("Email"),         emailF},
                {fieldLabel("Username"),      userF},
                {fieldLabel("Password"),      passF},
                {fieldLabel("Confirm Password"), pass2F}
        };
        for (JComponent[] row : rows) {
            card.add(row[0]);
            card.add(Box.createVerticalStrut(4));
            card.add(row[1]);
            card.add(Box.createVerticalStrut(12));
        }
        card.add(msg);
        card.add(Box.createVerticalStrut(8));
        card.add(regBtn);
        card.add(Box.createVerticalStrut(8));
        card.add(backBtn);

        root.add(card);
        f.setVisible(true);
    }

    // ─── MAIN APP ────────────────────────────────────────────────────────────────
    static void openMainApp() {
        mainFrame = new JFrame("BookingSystem — " + loggedInUser + (isAdmin ? " [Admin]" : ""));
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1100, 720);
        mainFrame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        mainFrame.setContentPane(root);

        // ── Sidebar ──────────────────────────────────────────────────────────
        JPanel sidebar = new JPanel();
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JLabel brand = new JLabel("🎟 BookingSystem");
        brand.setFont(new Font("SansSerif", Font.BOLD, 14));
        brand.setForeground(Color.WHITE);
        brand.setBorder(BorderFactory.createEmptyBorder(0, 20, 16, 0));
        sidebar.add(brand);

        JLabel userTag = new JLabel("  👤 " + loggedInUser + (isAdmin ? " ★" : ""));
        userTag.setFont(new Font("SansSerif", Font.PLAIN, 12));
        userTag.setForeground(new Color(0xAABBDD));
        userTag.setBorder(BorderFactory.createEmptyBorder(0, 14, 20, 0));
        sidebar.add(userTag);

        sidebar.add(sidebarDivider("USER"));

        String[][] userItems = {
                {"🏠","Dashboard"}, {"📅","New Booking"}, {"🔄","Renewal"},
                {"📋","My Bookings"}, {"✏","Update Booking"}, {"🪑","Reserves"},
                {"✅","Attend"}, {"🔍","Availability"}, {"💳","Take Payments"},
                {"❌","Cancellations"}, {"💸","Process Refund"}, {"🕐","Rescheduling"}
        };

        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(BG);

        for (String[] item : userItems) {
            sidebar.add(sidebarBtn(item[0] + "  " + item[1], item[1]));
        }

        if (isAdmin) {
            sidebar.add(Box.createVerticalStrut(12));
            sidebar.add(sidebarDivider("ADMIN"));
            String[][] adminItems = {
                    {"💰","Payment Status"}, {"↩","Refund Manager"}, {"🔧","Maintenance"}
            };
            for (String[] item : adminItems) {
                sidebar.add(sidebarBtn(item[0] + "  " + item[1], item[1]));
            }
        }

        sidebar.add(Box.createVerticalGlue());
        JButton logout = new JButton("⎋  Sign Out");
        logout.setForeground(new Color(0xFFAAAA));
        logout.setBackground(SIDEBAR_BG);
        logout.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));
        logout.setFocusPainted(false);
        logout.setBorderPainted(false);
        logout.setFont(new Font("SansSerif", Font.PLAIN, 13));
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.addActionListener(e -> { mainFrame.dispose(); loggedInUser=null; showLoginScreen(); });
        sidebar.add(logout);

        // ── Status bar ───────────────────────────────────────────────────────
        statusBar = new JLabel("  Ready");
        statusBar.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusBar.setForeground(MUTED);
        statusBar.setBorder(BorderFactory.createMatteBorder(1,0,0,0, BORDER));
        statusBar.setPreferredSize(new Dimension(0, 24));

        showPanel("Dashboard");

        root.add(sidebar, BorderLayout.WEST);
        root.add(contentArea, BorderLayout.CENTER);
        root.add(statusBar, BorderLayout.SOUTH);
        mainFrame.setVisible(true);
    }

    static void showPanel(String name) {
        contentArea.removeAll();
        switch (name) {
            case "Dashboard":       contentArea.add(dashboardPanel());      break;
            case "New Booking":     contentArea.add(bookingPanel());        break;
            case "Renewal":         contentArea.add(renewalPanel());        break;
            case "My Bookings":     contentArea.add(myBookingsPanel());     break;
            case "Update Booking":  contentArea.add(updateBookingPanel());  break;
            case "Reserves":        contentArea.add(reservesPanel());       break;
            case "Attend":          contentArea.add(attendPanel());         break;
            case "Availability":    contentArea.add(availabilityPanel());   break;
            case "Take Payments":   contentArea.add(paymentsPanel());       break;
            case "Cancellations":   contentArea.add(cancellationsPanel());  break;
            case "Process Refund":  contentArea.add(refundPanel());         break;
            case "Rescheduling":    contentArea.add(reschedulingPanel());   break;
            case "Payment Status":  contentArea.add(paymentStatusPanel());  break;
            case "Refund Manager":  contentArea.add(refundManagerPanel());  break;
            case "Maintenance":     contentArea.add(maintenancePanel());    break;
            default:                contentArea.add(dashboardPanel());
        }
        contentArea.revalidate();
        contentArea.repaint();
        setStatus("Viewing: " + name);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  PANELS
    // ══════════════════════════════════════════════════════════════════════════════

    static JPanel dashboardPanel() {
        JPanel p = scrollableBase();
        p.add(pageTitle("Dashboard", "Welcome back, " + loggedInUser + "!"));
        p.add(Box.createVerticalStrut(16));

        // Stat cards row
        JPanel stats = new JPanel(new GridLayout(1, 4, 14, 0));
        stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        long confirmed = bookings.stream().filter(b -> b[4].equals("Confirmed")).count();
        long expired   = bookings.stream().filter(b -> b[4].equals("Expired")).count();
        long cancelled = bookings.stream().filter(b -> b[4].equals("Cancelled")).count();
        stats.add(statCard("Total Bookings", String.valueOf(bookings.size()), ACCENT));
        stats.add(statCard("Confirmed",      String.valueOf(confirmed), ACCENT2));
        stats.add(statCard("Expired",        String.valueOf(expired),   WARN));
        stats.add(statCard("Cancelled",      String.valueOf(cancelled), DANGER));
        p.add(stats);
        p.add(Box.createVerticalStrut(20));

        // Recent bookings mini-table
        p.add(sectionHeader("Recent Bookings"));
        p.add(Box.createVerticalStrut(8));
        String[] cols = {"ID","Event","Date","Status","Amount"};
        Object[][] data = bookings.stream()
                .filter(b -> isAdmin || b[1].equals(loggedInUser))
                .map(b -> new Object[]{b[0],b[2],b[3],b[4],b[5]})
                .toArray(Object[][]::new);
        p.add(styledTable(cols, data));
        p.add(Box.createVerticalStrut(20));

        // Quick action buttons
        p.add(sectionHeader("Quick Actions"));
        p.add(Box.createVerticalStrut(8));
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btns.setOpaque(false);
        for (String[] a : new String[][]{
                {"New Booking","New Booking"}, {"Renewal","Renewal"},
                {"Cancellations","Cancellations"}, {"Process Refund","Process Refund"}}) {
            JButton b = accentButton(a[0], ACCENT);
            b.setPreferredSize(new Dimension(140, 36));
            String panel = a[1];
            b.addActionListener(e -> showPanel(panel));
            btns.add(b);
        }
        p.add(btns);
        return p;
    }

    static JPanel bookingPanel() {
        JPanel p = scrollableBase();
        p.add(pageTitle("New Booking", "Book an event or venue"));
        p.add(Box.createVerticalStrut(16));

        JPanel form = formCard();
        JTextField eventF  = styledField("e.g. Jazz Night, Tech Summit");
        JTextField venueF  = styledField("e.g. Central Hall");
        JSpinner   dateS   = new JSpinner(new SpinnerDateModel());
        dateS.setEditor(new JSpinner.DateEditor(dateS, "yyyy-MM-dd"));
        styleSpinner(dateS);
        JComboBox<String> typeBox = styledCombo(new String[]{"Standard","VIP","Group"});
        JTextField seatsF  = styledField("Number of seats");
        JLabel     msgL    = statusLabel();

        form.add(fieldLabel("Event Name"));     form.add(Box.createVerticalStrut(4));
        form.add(eventF);                       form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("Venue"));          form.add(Box.createVerticalStrut(4));
        form.add(venueF);                       form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("Date"));           form.add(Box.createVerticalStrut(4));
        form.add(dateS);                        form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("Booking Type"));   form.add(Box.createVerticalStrut(4));
        form.add(typeBox);                      form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("Seats"));          form.add(Box.createVerticalStrut(4));
        form.add(seatsF);                       form.add(Box.createVerticalStrut(16));

        JButton confirmBtn = accentButton("Confirm Booking", ACCENT);
        confirmBtn.addActionListener(e -> {
            String ev = eventF.getText().trim(), ve = venueF.getText().trim();
            if (ev.isEmpty() || ve.isEmpty()) { setStatus("Fill all fields."); showError(msgL,"Please fill in all fields."); return; }
            String id = "BK" + String.format("%03d", bookings.size() + 1);
            String date = new java.text.SimpleDateFormat("yyyy-MM-dd").format(
                    ((SpinnerDateModel) dateS.getModel()).getDate());
            bookings.add(new String[]{id, loggedInUser, ev, date, "Confirmed", "£50.00"});
            showSuccess(msgL, "✓ Booking " + id + " confirmed!");
            eventF.setText(""); venueF.setText(""); seatsF.setText("");
        });
        form.add(confirmBtn);
        form.add(Box.createVerticalStrut(8));
        form.add(msgL);

        // Extends → Expired note
        JLabel note = new JLabel("ℹ  Expired memberships extend to Booking. Renew first if needed.");
        note.setFont(new Font("SansSerif", Font.ITALIC, 12));
        note.setForeground(WARN);

        p.add(form);
        p.add(Box.createVerticalStrut(12));
        p.add(note);
        return p;
    }

    static JPanel renewalPanel() {
        JPanel p = scrollableBase();
        p.add(pageTitle("Renewal", "Renew your membership or pass"));
        p.add(Box.createVerticalStrut(16));

        JPanel form = formCard();
        JComboBox<String> planBox = styledCombo(new String[]{"Monthly — £9.99","Quarterly — £24.99","Annual — £79.99"});
        JTextField cardF = styledField("Card number (last 4 digits)");
        JLabel msgL = statusLabel();

        form.add(fieldLabel("Renewal Plan"));   form.add(Box.createVerticalStrut(4));
        form.add(planBox);                      form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("Payment Card"));   form.add(Box.createVerticalStrut(4));
        form.add(cardF);                        form.add(Box.createVerticalStrut(16));

        JButton btn = accentButton("Renew & Pay", ACCENT2);
        btn.addActionListener(e -> {
            // Includes → Confirmed
            showSuccess(msgL, "✓ Renewal confirmed! Your plan is now active.");
        });
        form.add(btn);
        form.add(Box.createVerticalStrut(8));
        form.add(msgL);

        JLabel note = new JLabel("ℹ  Renewal includes: Confirmed status automatically assigned.");
        note.setFont(new Font("SansSerif", Font.ITALIC, 12));
        note.setForeground(ACCENT);
        p.add(form);
        p.add(Box.createVerticalStrut(10));
        p.add(note);
        return p;
    }

    static JPanel myBookingsPanel() {
        JPanel p = scrollableBase();
        p.add(pageTitle("My Bookings", "All your reservations at a glance"));
        p.add(Box.createVerticalStrut(16));

        String[] cols = {"ID","Event","Date","Status","Amount"};
        Object[][] data = bookings.stream()
                .filter(b -> isAdmin || b[1].equals(loggedInUser))
                .map(b -> new Object[]{b[0],b[2],b[3],b[4],b[5]})
                .toArray(Object[][]::new);
        p.add(styledTable(cols, data));
        return p;
    }

    static JPanel updateBookingPanel() {
        JPanel p = scrollableBase();
        p.add(pageTitle("Update Booking", "Modify an existing booking"));
        p.add(Box.createVerticalStrut(16));

        JPanel form = formCard();
        String[] ids = bookings.stream()
                .filter(b -> isAdmin || b[1].equals(loggedInUser))
                .map(b -> b[0]).toArray(String[]::new);
        JComboBox<String> idBox = styledCombo(ids.length == 0 ? new String[]{"No bookings"} : ids);
        JTextField newEvent = styledField("New event name (leave blank to keep)");
        JTextField newDate  = styledField("New date YYYY-MM-DD (leave blank to keep)");
        JLabel msgL = statusLabel();

        form.add(fieldLabel("Booking ID"));     form.add(Box.createVerticalStrut(4));
        form.add(idBox);                        form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("New Event Name")); form.add(Box.createVerticalStrut(4));
        form.add(newEvent);                     form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("New Date"));       form.add(Box.createVerticalStrut(4));
        form.add(newDate);                      form.add(Box.createVerticalStrut(16));

        JButton btn = accentButton("Update Booking", ACCENT);
        btn.addActionListener(e -> {
            String sel = (String) idBox.getSelectedItem();
            bookings.stream().filter(b -> b[0].equals(sel)).findFirst().ifPresent(b -> {
                if (!newEvent.getText().isEmpty()) b[2] = newEvent.getText().trim();
                if (!newDate.getText().isEmpty())  b[3] = newDate.getText().trim();
            });
            showSuccess(msgL, "✓ Booking " + sel + " updated.");
        });
        form.add(btn);
        form.add(Box.createVerticalStrut(8));
        form.add(msgL);
        p.add(form);
        return p;
    }

    static JPanel reservesPanel() {
        JPanel p = scrollableBase();
        p.add(pageTitle("Reserves", "Hold a spot without full payment"));
        p.add(Box.createVerticalStrut(16));

        JPanel form = formCard();
        JTextField evF   = styledField("Event name");
        JTextField dateF = styledField("Date  YYYY-MM-DD");
        JTextField seatF = styledField("Preferred seat / section");
        JLabel msgL = statusLabel();

        form.add(fieldLabel("Event"));          form.add(Box.createVerticalStrut(4));
        form.add(evF);                          form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("Date"));           form.add(Box.createVerticalStrut(4));
        form.add(dateF);                        form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("Seat Preference"));form.add(Box.createVerticalStrut(4));
        form.add(seatF);                        form.add(Box.createVerticalStrut(16));

        JButton btn = accentButton("Reserve Spot", ACCENT2);
        btn.addActionListener(e -> {
            if (evF.getText().trim().isEmpty()) { showError(msgL,"Enter event name."); return; }
            String id = "RS" + String.format("%03d", bookings.size() + 1);
            bookings.add(new String[]{id, loggedInUser, evF.getText().trim(),
                    dateF.getText().trim(), "Reserved","£0.00"});
            showSuccess(msgL, "✓ Spot reserved as " + id + ". Complete payment within 48h.");
            evF.setText(""); dateF.setText(""); seatF.setText("");
        });
        form.add(btn);
        form.add(Box.createVerticalStrut(8));
        form.add(msgL);
        p.add(form);
        return p;
    }

    static JPanel attendPanel() {
        JPanel p = scrollableBase();
        p.add(pageTitle("Attend", "Check in to your event"));
        p.add(Box.createVerticalStrut(16));

        JPanel form = formCard();
        String[] confIds = bookings.stream()
                .filter(b -> b[4].equals("Confirmed") && (isAdmin || b[1].equals(loggedInUser)))
                .map(b -> b[0] + " — " + b[2]).toArray(String[]::new);
        JComboBox<String> box = styledCombo(confIds.length == 0
                ? new String[]{"No confirmed bookings"} : confIds);
        JTextField tokenF = styledField("Entry token / QR code");
        JLabel msgL = statusLabel();

        form.add(fieldLabel("Select Booking"));  form.add(Box.createVerticalStrut(4));
        form.add(box);                           form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("Entry Token"));     form.add(Box.createVerticalStrut(4));
        form.add(tokenF);                        form.add(Box.createVerticalStrut(16));

        JButton btn = accentButton("Check In", ACCENT2);
        btn.addActionListener(e -> {
            if (confIds.length == 0) { showError(msgL,"No confirmed bookings."); return; }
            showSuccess(msgL, "✓ Check-in successful! Enjoy your event 🎉");
        });
        form.add(btn);
        form.add(Box.createVerticalStrut(8));
        form.add(msgL);
        p.add(form);
        return p;
    }

    static JPanel availabilityPanel() {
        JPanel p = scrollableBase();
        p.add(pageTitle("Confirm Availability", "Check if seats are available"));
        p.add(Box.createVerticalStrut(16));

        JPanel form = formCard();
        JTextField evF   = styledField("Event name");
        JTextField dateF = styledField("Date  YYYY-MM-DD");
        JLabel resultL   = new JLabel(" ");
        resultL.setFont(new Font("SansSerif", Font.BOLD, 14));
        resultL.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(fieldLabel("Event"));  form.add(Box.createVerticalStrut(4));
        form.add(evF);                  form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("Date"));   form.add(Box.createVerticalStrut(4));
        form.add(dateF);                form.add(Box.createVerticalStrut(16));

        JButton btn = accentButton("Check Availability", ACCENT);
        btn.addActionListener(e -> {
            Random r = new Random();
            int seats = r.nextInt(50) + 1;
            resultL.setForeground(seats > 5 ? ACCENT2 : WARN);
            resultL.setText("  " + seats + " seats available for \"" + evF.getText().trim() + "\"");
        });
        form.add(btn);
        form.add(Box.createVerticalStrut(12));
        form.add(resultL);
        p.add(form);
        return p;
    }

    static JPanel paymentsPanel() {
        JPanel p = scrollableBase();
        p.add(pageTitle("Take Payments", "Complete payment for your booking"));
        p.add(Box.createVerticalStrut(16));

        JPanel form = formCard();
        String[] unpaid = bookings.stream()
                .filter(b -> b[4].equals("Reserved") && (isAdmin || b[1].equals(loggedInUser)))
                .map(b -> b[0] + " — " + b[2] + " (" + b[5] + ")")
                .toArray(String[]::new);
        JComboBox<String> box = styledCombo(unpaid.length == 0
                ? new String[]{"No pending payments"} : unpaid);
        JComboBox<String> methodBox = styledCombo(new String[]{"Credit Card","Debit Card","PayPal","Apple Pay"});
        JTextField cardF = styledField("Card number");
        JTextField cvvF  = styledField("CVV");
        JLabel msgL = statusLabel();

        form.add(fieldLabel("Booking"));        form.add(Box.createVerticalStrut(4));
        form.add(box);                          form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("Payment Method")); form.add(Box.createVerticalStrut(4));
        form.add(methodBox);                    form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("Card Number"));    form.add(Box.createVerticalStrut(4));
        form.add(cardF);                        form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("CVV"));            form.add(Box.createVerticalStrut(4));
        form.add(cvvF);                         form.add(Box.createVerticalStrut(16));

        JButton btn = accentButton("Pay Now", ACCENT2);
        btn.addActionListener(e -> {
            // Includes → Payment Confirmed
            String sel = (String) box.getSelectedItem();
            if (sel != null && !sel.equals("No pending payments")) {
                String id = sel.split(" ")[0];
                bookings.stream().filter(b->b[0].equals(id)).findFirst().ifPresent(b->b[4]="Confirmed");
                showSuccess(msgL, "✓ Payment confirmed! Booking updated to Confirmed.");
            } else {
                showSuccess(msgL, "✓ Payment confirmed! (Payment Confirmed status applied)");
            }
        });
        form.add(btn);
        form.add(Box.createVerticalStrut(8));
        form.add(msgL);

        JLabel note = new JLabel("ℹ  Take Payments includes: Payment Confirmed / Refund Rejected outcomes.");
        note.setFont(new Font("SansSerif", Font.ITALIC, 12));
        note.setForeground(MUTED);
        p.add(form);
        p.add(Box.createVerticalStrut(8));
        p.add(note);
        return p;
    }

    static JPanel cancellationsPanel() {
        JPanel p = scrollableBase();
        p.add(pageTitle("Cancellations", "Cancel a booking and request a refund"));
        p.add(Box.createVerticalStrut(16));

        JPanel form = formCard();
        String[] myIds = bookings.stream()
                .filter(b -> !b[4].equals("Cancelled") && (isAdmin || b[1].equals(loggedInUser)))
                .map(b -> b[0] + " — " + b[2]).toArray(String[]::new);
        JComboBox<String> box = styledCombo(myIds.length == 0 ? new String[]{"No active bookings"} : myIds);
        JComboBox<String> reasonBox = styledCombo(new String[]{
                "Personal reasons","Schedule conflict","Found alternative","Event cancelled by host","Other"});
        JCheckBox refundChk = new JCheckBox("Request refund");
        refundChk.setOpaque(false);
        refundChk.setFont(new Font("SansSerif", Font.PLAIN, 13));
        refundChk.setForeground(TEXT);
        JLabel msgL = statusLabel();

        form.add(fieldLabel("Booking to Cancel")); form.add(Box.createVerticalStrut(4));
        form.add(box);                             form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("Reason"));            form.add(Box.createVerticalStrut(4));
        form.add(reasonBox);                       form.add(Box.createVerticalStrut(12));
        form.add(refundChk);                       form.add(Box.createVerticalStrut(16));

        JButton btn = accentButton("Cancel Booking", DANGER);
        btn.addActionListener(e -> {
            String sel = (String) box.getSelectedItem();
            if (sel == null || sel.equals("No active bookings")) { showError(msgL,"Select a booking."); return; }
            String id = sel.split(" ")[0];
            bookings.stream().filter(b->b[0].equals(id)).findFirst().ifPresent(b->b[4]="Cancelled");
            if (refundChk.isSelected()) {
                // Includes → Refund Rejected / Refund Confirmed
                refunds.add(new String[]{id, loggedInUser, "£25.00", "Pending"});
                showSuccess(msgL, "✓ Booking cancelled. Refund request submitted (pending review).");
            } else {
                showSuccess(msgL, "✓ Booking " + id + " cancelled.");
            }
        });
        form.add(btn);
        form.add(Box.createVerticalStrut(8));
        form.add(msgL);

        JLabel note = new JLabel("ℹ  Cancellations includes: Refund Rejected & Refund Confirmed outcomes.");
        note.setFont(new Font("SansSerif", Font.ITALIC, 12));
        note.setForeground(MUTED);
        p.add(form);
        p.add(Box.createVerticalStrut(8));
        p.add(note);
        return p;
    }

    static JPanel refundPanel() {
        JPanel p = scrollableBase();
        p.add(pageTitle("Process Refund", "Track and request refunds"));
        p.add(Box.createVerticalStrut(16));

        String[] cols = {"Booking ID","User","Amount","Status"};
        Object[][] data = refunds.stream()
                .filter(r -> isAdmin || r[1].equals(loggedInUser))
                .map(r -> new Object[]{r[0],r[1],r[2],r[3]})
                .toArray(Object[][]::new);
        p.add(styledTable(cols, data));
        p.add(Box.createVerticalStrut(16));

        JPanel form = formCard();
        JTextField bkIdF  = styledField("Booking ID  e.g. BK001");
        JTextField amtF   = styledField("Refund amount  e.g. £25.00");
        JLabel msgL = statusLabel();

        form.add(fieldLabel("Booking ID"));     form.add(Box.createVerticalStrut(4));
        form.add(bkIdF);                        form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("Refund Amount"));  form.add(Box.createVerticalStrut(4));
        form.add(amtF);                         form.add(Box.createVerticalStrut(16));

        JButton btn = accentButton("Submit Refund Request", ACCENT);
        btn.addActionListener(e -> {
            refunds.add(new String[]{bkIdF.getText().trim(), loggedInUser,
                    amtF.getText().trim(), "Pending"});
            showSuccess(msgL, "✓ Refund request submitted. Admin will review.");
            bkIdF.setText(""); amtF.setText("");
        });
        form.add(btn);
        form.add(Box.createVerticalStrut(8));
        form.add(msgL);
        p.add(form);
        return p;
    }

    static JPanel reschedulingPanel() {
        JPanel p = scrollableBase();
        p.add(pageTitle("Rescheduling", "Move your booking to a new date"));
        p.add(Box.createVerticalStrut(16));

        JPanel form = formCard();
        String[] confIds = bookings.stream()
                .filter(b -> b[4].equals("Confirmed") && (isAdmin || b[1].equals(loggedInUser)))
                .map(b -> b[0] + " — " + b[2] + " (" + b[3] + ")")
                .toArray(String[]::new);
        JComboBox<String> box = styledCombo(confIds.length == 0
                ? new String[]{"No confirmed bookings"} : confIds);
        JTextField newDateF = styledField("New date  YYYY-MM-DD");
        JTextField reasonF  = styledField("Reason for rescheduling");
        JLabel msgL = statusLabel();

        form.add(fieldLabel("Booking"));         form.add(Box.createVerticalStrut(4));
        form.add(box);                           form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("New Date"));        form.add(Box.createVerticalStrut(4));
        form.add(newDateF);                      form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("Reason"));          form.add(Box.createVerticalStrut(4));
        form.add(reasonF);                       form.add(Box.createVerticalStrut(16));

        JButton btn = accentButton("Reschedule", ACCENT);
        btn.addActionListener(e -> {
            String sel = (String) box.getSelectedItem();
            if (sel == null || sel.equals("No confirmed bookings")) { showError(msgL,"Select booking."); return; }
            String id = sel.split(" ")[0];
            bookings.stream().filter(b->b[0].equals(id)).findFirst()
                    .ifPresent(b->b[3]=newDateF.getText().trim());
            showSuccess(msgL, "✓ Booking " + id + " rescheduled to " + newDateF.getText().trim());
        });
        form.add(btn);
        form.add(Box.createVerticalStrut(8));
        form.add(msgL);
        p.add(form);
        return p;
    }

    // ── ADMIN PANELS ─────────────────────────────────────────────────────────────
    static JPanel paymentStatusPanel() {
        JPanel p = scrollableBase();
        p.add(pageTitle("Payment Status", "Confirm or reject payments — Admin"));
        p.add(Box.createVerticalStrut(16));

        String[] cols = {"ID","User","Event","Date","Status","Amount"};
        Object[][] data = bookings.stream()
                .map(b -> new Object[]{b[0],b[1],b[2],b[3],b[4],b[5]})
                .toArray(Object[][]::new);
        p.add(styledTable(cols, data));
        p.add(Box.createVerticalStrut(16));

        JPanel form = formCard();
        JTextField idF = styledField("Booking ID to update");
        JComboBox<String> statusBox = styledCombo(
                new String[]{"Confirmed","Payment confirmed","Expired","Cancelled"});
        JLabel msgL = statusLabel();

        form.add(fieldLabel("Booking ID")); form.add(Box.createVerticalStrut(4));
        form.add(idF);                      form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("New Status")); form.add(Box.createVerticalStrut(4));
        form.add(statusBox);               form.add(Box.createVerticalStrut(16));

        JButton btn = accentButton("Update Status", WARN);
        btn.addActionListener(e -> {
            String id = idF.getText().trim(), st = (String)statusBox.getSelectedItem();
            bookings.stream().filter(b->b[0].equals(id)).findFirst()
                    .ifPresent(b->b[4]=st);
            showSuccess(msgL, "✓ Status updated to: " + st);
        });
        form.add(btn);
        form.add(Box.createVerticalStrut(8));
        form.add(msgL);
        p.add(form);
        return p;
    }

    static JPanel refundManagerPanel() {
        JPanel p = scrollableBase();
        p.add(pageTitle("Refund Manager", "Approve or reject refund requests — Admin"));
        p.add(Box.createVerticalStrut(16));

        String[] cols = {"Booking ID","User","Amount","Status"};
        Object[][] data = refunds.stream()
                .map(r -> new Object[]{r[0],r[1],r[2],r[3]})
                .toArray(Object[][]::new);
        p.add(styledTable(cols, data));
        p.add(Box.createVerticalStrut(16));

        JPanel form = formCard();
        JTextField idF = styledField("Booking ID to process");
        JComboBox<String> decBox = styledCombo(new String[]{"Refund Confirmed","Refund Rejected"});
        JLabel msgL = statusLabel();

        form.add(fieldLabel("Booking ID"));  form.add(Box.createVerticalStrut(4));
        form.add(idF);                       form.add(Box.createVerticalStrut(12));
        form.add(fieldLabel("Decision"));    form.add(Box.createVerticalStrut(4));
        form.add(decBox);                    form.add(Box.createVerticalStrut(16));

        JButton btn = accentButton("Process Refund", ACCENT2);
        btn.addActionListener(e -> {
            String id = idF.getText().trim(), dec = (String)decBox.getSelectedItem();
            refunds.stream().filter(r->r[0].equals(id)).findFirst().ifPresent(r->r[3]=dec);
            Color c = dec.equals("Refund Confirmed") ? ACCENT2 : DANGER;
            msgL.setForeground(c);
            msgL.setText("✓ " + dec + " for booking " + id);
        });
        form.add(btn);
        form.add(Box.createVerticalStrut(8));
        form.add(msgL);
        p.add(form);
        return p;
    }

    static JPanel maintenancePanel() {
        JPanel p = scrollableBase();
        p.add(pageTitle("Maintenance", "System administration — Admin only"));
        p.add(Box.createVerticalStrut(16));

        JPanel grid = new JPanel(new GridLayout(2, 3, 14, 14));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        String[][] tools = {
                {"🗄","Database Backup","Run"},
                {"📊","System Report","Generate"},
                {"🔐","User Management","Open"},
                {"🧹","Clear Old Bookings","Run"},
                {"📧","Email Notifications","Configure"},
                {"⚙","System Settings","Open"}
        };

        JLabel msgL = statusLabel();

        for (String[] t : tools) {
            JPanel card = roundedCard(160, 80);
            card.setLayout(new BorderLayout(8,4));
            card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
            JLabel ic   = new JLabel(t[0]);  ic.setFont(new Font("SansSerif",Font.PLAIN,22));
            JLabel name = new JLabel(t[1]);  name.setFont(new Font("SansSerif",Font.BOLD,12));
            name.setForeground(TEXT);
            JButton btn = accentButton(t[2], ACCENT);
            btn.setFont(new Font("SansSerif",Font.BOLD,11));
            btn.addActionListener(e -> {
                msgL.setForeground(ACCENT2);
                msgL.setText("✓ " + t[1] + " executed successfully.");
            });
            card.add(ic, BorderLayout.WEST);
            card.add(name, BorderLayout.CENTER);
            card.add(btn, BorderLayout.EAST);
            grid.add(card);
        }

        p.add(grid);
        p.add(Box.createVerticalStrut(12));
        p.add(msgL);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ══════════════════════════════════════════════════════════════════════════════

    static JPanel scrollableBase() {
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(BG);
        inner.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        return inner;
    }

    static JPanel formCard() {
        JPanel c = new JPanel();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBackground(CARD);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        c.setMaximumSize(new Dimension(520, Integer.MAX_VALUE));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    static JPanel roundedCard(int w, int h) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        if (w > 0) card.setPreferredSize(new Dimension(w, h));
        return card;
    }

    static JPanel statCard(String label, String value, Color accent) {
        JPanel c = new JPanel(new BorderLayout(0, 4));
        c.setBackground(CARD);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER, 1),
                        BorderFactory.createEmptyBorder(14, 16, 14, 16))));
        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.BOLD, 28));
        val.setForeground(accent);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setForeground(MUTED);
        c.add(val, BorderLayout.CENTER);
        c.add(lbl, BorderLayout.SOUTH);
        return c;
    }

    static JComponent pageTitle(String title, String sub) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 22));
        t.setForeground(TEXT);
        JLabel s = new JLabel(sub);
        s.setFont(new Font("SansSerif", Font.PLAIN, 13));
        s.setForeground(MUTED);
        p.add(t); p.add(Box.createVerticalStrut(2)); p.add(s);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    static JLabel sectionHeader(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        l.setForeground(TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    static JLabel fieldLabel(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    static JTextField styledField(String ph) {
        JTextField f = new JTextField();
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setForeground(TEXT);
        f.setBackground(new Color(0xF8FAFF));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Placeholder
        f.setForeground(MUTED);
        f.setText(ph);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { if (f.getText().equals(ph)) { f.setText(""); f.setForeground(TEXT); } }
            public void focusLost(FocusEvent e)   { if (f.getText().isEmpty()) { f.setText(ph); f.setForeground(MUTED); } }
        });
        return f;
    }

    static void stylePasswordField(JPasswordField f, String ph) {
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setForeground(MUTED);
        f.setBackground(new Color(0xF8FAFF));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setEchoChar((char) 0);
        f.setText(ph);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (new String(f.getPassword()).equals(ph)) { f.setText(""); f.setForeground(TEXT); f.setEchoChar((char) 0x25CF); }
            }
            public void focusLost(FocusEvent e) {
                if (new String(f.getPassword()).isEmpty()) { f.setEchoChar((char)0); f.setForeground(MUTED); f.setText(ph); }
            }
        });
    }

    static void styleSpinner(JSpinner s) {
        s.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER,1,true),
                BorderFactory.createEmptyBorder(2,6,2,6)));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    static <T> JComboBox<T> styledCombo(T[] items) {
        JComboBox<T> c = new JComboBox<>(items);
        c.setFont(new Font("SansSerif", Font.PLAIN, 13));
        c.setBackground(new Color(0xF8FAFF));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    static JButton accentButton(String txt, Color bg) {
        JButton b = new JButton(txt) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? bg.darker() :
                        getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        b.setPreferredSize(new Dimension(200, 38));
        return b;
    }

    static JButton ghostButton(String txt) {
        JButton b = new JButton(txt);
        b.setForeground(ACCENT);
        b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        return b;
    }

    static JButton sidebarBtn(String label, String panelName) {
        JButton b = new JButton(label);
        b.setBackground(SIDEBAR_BG);
        b.setForeground(new Color(0xCCDDFF));
        b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(BorderFactory.createEmptyBorder(9, 22, 9, 22));
        b.setMaximumSize(new Dimension(200, 36));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(0x2A3F6A)); b.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e)  { b.setBackground(SIDEBAR_BG); b.setForeground(new Color(0xCCDDFF)); }
        });
        b.addActionListener(e -> showPanel(panelName));
        return b;
    }

    static JLabel sidebarDivider(String txt) {
        JLabel l = new JLabel("  " + txt);
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setForeground(new Color(0x607090));
        l.setMaximumSize(new Dimension(200, 22));
        l.setBorder(BorderFactory.createEmptyBorder(6,0,2,0));
        return l;
    }

    static JScrollPane styledTable(String[] cols, Object[][] data) {
        DefaultTableModel model = new DefaultTableModel(data, cols) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setRowHeight(32);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(0xEEF2FF));
        table.getTableHeader().setForeground(TEXT);
        table.setGridColor(BORDER);
        table.setBackground(CARD);
        table.setSelectionBackground(new Color(0xDEEAFF));
        table.setFillsViewportHeight(true);

        // Color-code Status column
        int statusCol = -1;
        for (int i = 0; i < cols.length; i++) if (cols[i].equals("Status")) { statusCol = i; break; }
        if (statusCol >= 0) {
            final int sc = statusCol;
            table.getColumnModel().getColumn(sc).setCellRenderer(new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable t, Object v,
                                                                         boolean sel, boolean foc, int r, int c) {
                    super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                    String s = v == null ? "" : v.toString();
                    setForeground(s.contains("Confirm") ? ACCENT2 :
                            s.contains("Expired") ? WARN :
                                    s.contains("Cancel") || s.contains("Reject") ? DANGER :
                                            s.contains("Reserve") ? ACCENT : TEXT);
                    setFont(getFont().deriveFont(Font.BOLD));
                    return this;
                }
            });
        }

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        return sp;
    }

    static JLabel statusLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    static void showSuccess(JLabel l, String msg) { l.setForeground(ACCENT2); l.setText(msg); setStatus(msg); }
    static void showError(JLabel l, String msg)   { l.setForeground(DANGER);  l.setText(msg); setStatus(msg); }
    static void setStatus(String msg) { if (statusBar != null) statusBar.setText("  " + msg); }
}