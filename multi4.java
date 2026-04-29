import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

// 
//  multi4.java  -  GEU Online Complaint Management System
//  Features:
//    - Portal Selection Screen (Admin / Student / Register)
//    - Role-Based Login with validation
//    - Student Dashboard: Submit and track own complaints live
//    - Admin Dashboard: Full moderation with stats strip and filter
//    - Complaint Lifecycle: Pending to Accept/Reject/Block to Progress to Resolved
//    - Blocked flow: Unblock resets to Pending
//    - Admin notes pushed to student view in real-time
//    - Glassmorphism UI with animated slideshow background
// 
public class multi4 extends JFrame {

    //Shared In-Memory Store for temporary use
    static final HashMap<String, String> USER_DB    = new HashMap<>();
    static final HashMap<String, String> ROLE_DB    = new HashMap<>();
    static final ArrayList<Complaint>    COMPLAINTS = new ArrayList<>();
    static final Color C_TEXT_LIGHT = new Color(230, 240, 255);
    static final Color C_TEXT_MED   = new Color(200, 215, 240);
    static final double[] COL_WEIGHTS = {0.04, 0.08, 0.09, 0.07, 0.09, 0.36, 0.27};
    static final String USER_FILE = "users.txt";
   
    JPanel wrap(JComponent comp) {
    JPanel p = new JPanel(new BorderLayout());
    p.setOpaque(false);
    p.add(comp, BorderLayout.CENTER);
    return p;
}

   JPanel cell(String text, int align, Font font, Color color) {
    JLabel lbl = new JLabel(text, align);
    lbl.setFont(font);
    lbl.setForeground(color);
    lbl.setBorder(new EmptyBorder(0, 4, 0, 4)); 

    JPanel p = new JPanel(new BorderLayout());
    p.setOpaque(false);
    p.add(lbl, BorderLayout.CENTER);

    return p;
}

    void saveUsers() {
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(USER_FILE))) {
        for (String user : USER_DB.keySet()) {
            bw.write(user + "|" + USER_DB.get(user));
            bw.newLine();
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}

void loadUsers() {
    try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\\|");

            if (parts.length == 2) {
                USER_DB.put(parts[0], parts[1]);
                ROLE_DB.put(parts[0], "student");   // 
            }
        }
    } catch (Exception e) {
        // ignore
    }
}


  static {
    USER_DB.put("admin", "1234");
    ROLE_DB.put("admin", "admin");
}


    // Colors and dimensions
    static final Color C_BG_DARK    = new Color(8,   15,  35);
    static final Color C_BG_MID     = new Color(14,  26,  60);
    static final Color C_ACCENT     = new Color(99,  179, 237);
    static final Color C_ACCENT2    = new Color(154, 117, 255);
    static final Color C_GLASS_FILL = new Color(255, 255, 255, 22);
    static final Color C_GLASS_EDGE = new Color(255, 255, 255, 70);
    static final Color C_TEXT       = new Color(230, 238, 255);
    static final Color C_TEXT_DIM   = new Color(160, 175, 210);

    static final Color C_PENDING  = new Color(255, 214, 90);
    static final Color C_ACCEPTED = new Color(72,  213, 151);
    static final Color C_INPROG   = new Color(99,  179, 237);
    static final Color C_RESOLVED = new Color(72,  213, 151);
    static final Color C_REJECTED = new Color(255, 100, 100);
    static final Color C_BLOCKED  = new Color(200,  80, 200);

    // Font styles
    static final Font F_DISPLAY = new Font("Georgia",    Font.BOLD,  50);
    static final Font F_TITLE   = new Font("Georgia",    Font.BOLD,  26);
    static final Font F_HEADING = new Font("SansSerif",  Font.BOLD,  19);
    static final Font F_BODY    = new Font("SansSerif",  Font.PLAIN, 15);
    static final Font F_SMALL   = new Font("SansSerif",  Font.PLAIN, 12);
    static final Font F_MONO    = new Font("Monospaced", Font.BOLD,  14);

    // Slideshow 
    private final String[] IMAGE_PATHS = {
        "resources/image_8.png",  "resources/image_9.png",
        "resources/image_10.png", "resources/image_11.png",
        "resources/image_12.png", "resources/image_13.png"
    };
    final ArrayList<BufferedImage> bgImages = new ArrayList<>();
    int bgIndex = 0;

    // ── Session ───────────────────────────────────────────────────
    String sessionUser;
    String sessionRole;

    // ── Root pane & timer ─────────────────────────────────────────
    BackgroundPanel root;
    Timer           slideTimer;

    // ── Real-time sync callback ───────────────────────────────────
    // Admin dashboard sets this; student dashboard reads it after submit.
    Runnable onComplaintsChanged = () -> {};

    // ════════════════════════════════════════════════════════════════
    public multi4() {
        loadImages();
        loadUsers();
        setTitle("GEU Online Complaint Management System");
         USER_DB.put("admin", "1234");
         ROLE_DB.put("admin", "admin");
        COMPLAINTS.clear();
        COMPLAINTS.addAll(FileDatabase.loadAll());
        FileDatabase.saveAll(COMPLAINTS);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        root = new BackgroundPanel();
        root.setLayout(new BorderLayout());
        setContentPane(root);

        slideTimer = new Timer(4000, e -> {
            if (!bgImages.isEmpty()) { bgIndex = (bgIndex + 1) % bgImages.size(); root.repaint(); }
        });
        slideTimer.start();

        showPortalSelection();
        setVisible(true);
    }

    void loadImages() {
        for (String p : IMAGE_PATHS) {
            try { File f = new File(p); if (f.exists()) bgImages.add(ImageIO.read(f)); }
            catch (IOException ignored) {}
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  SCREEN 1 — PORTAL SELECTION
    // ════════════════════════════════════════════════════════════════
    void showPortalSelection() {
        root.removeAll();
        root.setLayout(new GridBagLayout());

        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setOpaque(false);

        JLabel title = lbl("GEU Complaint Portal", F_DISPLAY, C_TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = lbl("Select your dashboard to continue", F_BODY, C_TEXT_DIM);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        wrap.add(Box.createVerticalStrut(10));
        wrap.add(title);
        wrap.add(Box.createVerticalStrut(8));
        wrap.add(sub);
        wrap.add(Box.createVerticalStrut(48));

        JPanel cards = new JPanel(new FlowLayout(FlowLayout.CENTER, 36, 0));
        cards.setOpaque(false);

        cards.add(portalCard("⚙",  "Admin Portal",
            "Manage all complaints,\nmoderate users,\nview analytics.",
            C_ACCENT2, () -> showLoginScreen("admin")));

        cards.add(portalCard("🎓", "Student Portal",
            "Submit complaints\nand track their\nlive status.",
            C_ACCENT, () -> showLoginScreen("student")));

        cards.add(portalCard("✎",  "Register",
            "New student?\nCreate your\naccount here.",
            C_ACCEPTED, this::showRegisterScreen));

        wrap.add(cards);
        root.add(wrap);
        root.revalidate();
        root.repaint();
    }

    JPanel portalCard(String icon, String title, String desc, Color accent, Runnable action) {
        boolean[] hov = {false};
        GlassPanel card = new GlassPanel(new BorderLayout(0, 12), 26) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov[0]
                    ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 55)
                    : C_GLASS_FILL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
                g2.setStroke(new BasicStroke(hov[0] ? 2.2f : 1.4f));
                g2.setColor(hov[0] ? accent : C_GLASS_EDGE);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 26, 26);
                g2.dispose(); super.paintComponent(g);
            }
        };
        card.setPreferredSize(new Dimension(250, 270));
        card.setBorder(new EmptyBorder(32, 28, 28, 28));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hov[0]=true;  card.repaint(); }
            public void mouseExited (MouseEvent e) { hov[0]=false; card.repaint(); }
            public void mouseClicked(MouseEvent e) { action.run(); }
        });

        JLabel ico = lbl(icon, new Font("SansSerif", Font.PLAIN, 46), Color.WHITE);
        ico.setHorizontalAlignment(JLabel.CENTER);
        ico.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel ttl = lbl(title, F_TITLE, C_TEXT);
        ttl.setHorizontalAlignment(JLabel.CENTER);

        JLabel dsc = new JLabel("<html><center>" + desc.replace("\n","<br>") + "</center></html>");
        dsc.setFont(F_BODY); dsc.setForeground(C_TEXT_DIM);
        dsc.setHorizontalAlignment(JLabel.CENTER);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);
        inner.add(ico);
        inner.add(Box.createVerticalStrut(12));
        inner.add(ttl);
        inner.add(Box.createVerticalStrut(8));
        inner.add(dsc);
        card.add(inner, BorderLayout.CENTER);

        JButton btn = accentBtn("Enter →", accent);
        btn.addActionListener(e -> action.run());
        card.add(btn, BorderLayout.SOUTH);
        return card;
    }

    //  SCREEN 2 - LOGIN
    void showLoginScreen(String expectedRole) {
        root.removeAll();
        root.setLayout(new GridBagLayout());

        GlassPanel box = new GlassPanel(new GridBagLayout(), 30);
        box.setPreferredSize(new Dimension(500, 420));
        box.setBorder(new EmptyBorder(44, 50, 44, 50));

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.weightx = 1;

        String rl = "admin".equals(expectedRole) ? "Admin" : "Student";
        JLabel hdr = lbl(rl + " Login", F_TITLE, C_TEXT);
        hdr.setHorizontalAlignment(JLabel.CENTER);
        gc.gridy = 0; gc.insets = new Insets(0,0,22,0); box.add(hdr, gc);

        JTextField     userF = styledField();
        JPasswordField passF = styledPass();

        gc.insets = new Insets(5,0,5,0);
        gc.gridy = 1; box.add(fieldLbl("Username"), gc);
        gc.gridy = 2; box.add(userF, gc);
        gc.gridy = 3; box.add(fieldLbl("Password"), gc);
        gc.gridy = 4; box.add(passF, gc);

        gc.gridy = 5; gc.insets = new Insets(20,0,8,0);
        JButton loginBtn = accentBtn("LOGIN", C_ACCENT);
        loginBtn.setPreferredSize(new Dimension(380, 46));
        box.add(loginBtn, gc);

        gc.gridy = 6; gc.insets = new Insets(2,0,0,0);
        JButton backBtn = ghostBtn("← Back to Portal");
        box.add(backBtn, gc);
    loginBtn.addActionListener(e -> {
    String u = userF.getText().trim();
    String p = new String(passF.getPassword());

    // ❗ If user not registered
    if (!USER_DB.containsKey(u)) {
        JOptionPane.showMessageDialog(this, "User not registered. Please register first.");
        return;
    }

    // ❗ If wrong password
    if (!USER_DB.get(u).equals(p)) {
        shake(box);
        JOptionPane.showMessageDialog(this, "Invalid credentials.", "Login Failed", JOptionPane.ERROR_MESSAGE);
        return;
    }

    String role = ROLE_DB.getOrDefault(u, "student");
    sessionUser = u;
    sessionRole = role;

    if ("admin".equals(role)) showAdminDashboard();
    else showStudentDashboard();
});
        backBtn.addActionListener(e -> showPortalSelection());

        root.add(box);
        root.revalidate(); root.repaint();
    }



    //  SCREEN 3 - REGISTER



    void showRegisterScreen() {
        root.removeAll();
        root.setLayout(new GridBagLayout());

        GlassPanel box = new GlassPanel(new GridBagLayout(), 30);
        box.setPreferredSize(new Dimension(500, 460));
        box.setBorder(new EmptyBorder(44, 50, 44, 50));

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.weightx = 1;

        JLabel hdr = lbl("Create Account", F_TITLE, C_TEXT);
        hdr.setHorizontalAlignment(JLabel.CENTER);
        gc.gridy = 0; gc.insets = new Insets(0,0,22,0); box.add(hdr, gc);

        JTextField     userF  = styledField();
        JPasswordField passF  = styledPass();
        JPasswordField pass2F = styledPass();

        gc.insets = new Insets(5,0,5,0);
        gc.gridy = 1; box.add(fieldLbl("Username"), gc);
        gc.gridy = 2; box.add(userF, gc);
        gc.gridy = 3; box.add(fieldLbl("Password"), gc);
        gc.gridy = 4; box.add(passF, gc);
        gc.gridy = 5; box.add(fieldLbl("Confirm Password"), gc);
        gc.gridy = 6; box.add(pass2F, gc);

        gc.gridy = 7; gc.insets = new Insets(20,0,8,0);
        JButton regBtn = accentBtn("REGISTER", C_ACCEPTED);
        regBtn.setPreferredSize(new Dimension(380, 46));
        box.add(regBtn, gc);

        gc.gridy = 8; gc.insets = new Insets(2,0,0,0);
        JButton backBtn = ghostBtn("← Back to Portal");
        box.add(backBtn, gc);

        regBtn.addActionListener(e -> {
            String u  = userF.getText().trim();
            String p  = new String(passF.getPassword());
            String p2 = new String(pass2F.getPassword());
            if (u.isEmpty() || p.isEmpty()) { JOptionPane.showMessageDialog(this, "All fields are required."); return; }
            if (!p.equals(p2))             { JOptionPane.showMessageDialog(this, "Passwords do not match.");   return; }
            if (USER_DB.containsKey(u))    { JOptionPane.showMessageDialog(this, "Username already exists!"); return; }
            USER_DB.put(u, p); ROLE_DB.put(u, "student"); saveUsers();
            JOptionPane.showMessageDialog(this, "✔ Registration successful! Please login.");
            showLoginScreen("student");
        });
        backBtn.addActionListener(e -> showPortalSelection());

        root.add(box);
        root.revalidate(); root.repaint();
    }
    void showChangePassword() {
        JPasswordField oldPass  = styledPass();
        JPasswordField newPass  = styledPass();
        JPasswordField confPass = styledPass();

        JPanel panel = new JPanel(new GridLayout(6, 1, 5, 5));
        panel.add(new JLabel("Current Password:"));
        panel.add(oldPass);
        panel.add(new JLabel("New Password:"));
        panel.add(newPass);
        panel.add(new JLabel("Confirm New Password:"));
        panel.add(confPass);

        int result = JOptionPane.showConfirmDialog(this, panel,
            "Change Password", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String old = new String(oldPass.getPassword());
            String nw  = new String(newPass.getPassword());
            String cf  = new String(confPass.getPassword());

            if (old.isEmpty() || nw.isEmpty() || cf.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.");
                return;
            }
            if (!USER_DB.get(sessionUser).equals(old)) {
                JOptionPane.showMessageDialog(this, "Current password is incorrect!");
                return;
            }
            if (nw.equals(old)) {
                JOptionPane.showMessageDialog(this, "New password cannot be same as current!");
                return;
            }
            if (!nw.equals(cf)) {
                JOptionPane.showMessageDialog(this, "New passwords do not match!");
                return;
            }
            if (nw.length() < 4) {
                JOptionPane.showMessageDialog(this, "Password must be at least 4 characters.");
                return;
            }
            USER_DB.put(sessionUser, nw);
            JOptionPane.showMessageDialog(this, "Password changed successfully!");
        }
    }


    // 
    //  SCREEN 4 - STUDENT DASHBOARD
    // 
    void showStudentDashboard() {
        root.removeAll();
        root.setLayout(new BorderLayout());
        root.add(topBar("Student Dashboard  —  " + sessionUser.toUpperCase(),
                        C_ACCENT, () -> { sessionUser=null; showPortalSelection(); }), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setOpaque(false); split.setBorder(null);
        split.setDividerLocation(0.46); split.setResizeWeight(0.46);

        // ── Left: submission form ────────────────────────────────
        GlassPanel leftGlass = new GlassPanel(new BorderLayout(0, 14), 0);
        leftGlass.setBorder(new EmptyBorder(28, 36, 28, 18));
        JLabel subTitle = lbl("Submit a Complaint  |  Your Total: " + countUserComplaints(sessionUser), F_HEADING, C_TEXT);
        

        JTextArea area = new JTextArea(6, 20);
        styleTA(area);
        JScrollPane areaScroll = glassScroll(area);

        String[] cats = {"Academic","Hostel","Library","Canteen","Infrastructure","Other"};
        JComboBox<String> catBox = styledCombo(cats);
        JButton submitBtn = accentBtn("SUBMIT COMPLAINT", C_ACCENT);
        submitBtn.setPreferredSize(new Dimension(280, 44));
        submitBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.add(fieldLbl("Description"));  form.add(Box.createVerticalStrut(6));
        form.add(areaScroll);               form.add(Box.createVerticalStrut(12));
        form.add(fieldLbl("Category"));     form.add(Box.createVerticalStrut(5));
        form.add(catBox);                   form.add(Box.createVerticalStrut(12));
        form.add(submitBtn);

        leftGlass.add(subTitle, BorderLayout.NORTH);
        leftGlass.add(form,     BorderLayout.CENTER);

        //    Right: my complaints board
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(new EmptyBorder(28, 18, 28, 36));

        JLabel boardTitle = lbl("My Complaints", F_HEADING, C_TEXT);

        JPanel listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setOpaque(false);

        JScrollPane listScroll = glassScroll(listContainer);

        rightPanel.add(boardTitle, BorderLayout.NORTH);
        rightPanel.add(listScroll, BorderLayout.CENTER);

        split.setLeftComponent(leftGlass);
        split.setRightComponent(rightPanel);
        root.add(split, BorderLayout.CENTER);

        Runnable refreshStudent = () -> {
            listContainer.removeAll();
            List<Complaint> mine = COMPLAINTS.stream()
                .filter(c -> c.getUser().equals(sessionUser))
                .collect(Collectors.toList());

            if (mine.isEmpty()) {
                JLabel e = lbl("No complaints yet. Submit one!", F_BODY, C_TEXT_DIM);
                e.setAlignmentX(Component.CENTER_ALIGNMENT);
                listContainer.add(Box.createVerticalStrut(40));
                listContainer.add(e);
            }
            for (int i = mine.size()-1; i >= 0; i--) {
                listContainer.add(studentCard(mine.get(i)));
                listContainer.add(Box.createRigidArea(new Dimension(0, 10)));
            }
            listContainer.revalidate(); listContainer.repaint();
        };

        onComplaintsChanged = refreshStudent;
        refreshStudent.run();

        submitBtn.addActionListener(e -> {
            String txt = area.getText().trim();
            if (txt.isEmpty()) { JOptionPane.showMessageDialog(this,"Please describe your complaint."); return; }
           String category = "General";
           Complaint c = new Complaint(sessionUser, txt, category);

            COMPLAINTS.add(c);

           FileDatabase.saveAll(COMPLAINTS);  
             area.setText("");
            JOptionPane.showMessageDialog(this, "✔ Complaint #" + c.getId() + " submitted!");
            refreshStudent.run();
        });

        root.revalidate(); root.repaint();
    }

    JPanel studentCard(Complaint c) {
        GlassPanel card = new GlassPanel(new BorderLayout(0, 6), 16);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel topRow = new JPanel(new BorderLayout()); topRow.setOpaque(false);
      topRow.add(lbl("#" + c.getId() + "  [" + c.getCategory() + "]", F_SMALL, C_TEXT_DIM), BorderLayout.WEST);
        topRow.add(statusBadge(c.getStatus()), BorderLayout.EAST);

        String prev = c.getText().length()>85 ? c.getText().substring(0,82)+"..." : c.getText();
        JLabel body = lbl(prev, F_BODY, C_TEXT);

        JPanel bottom = new JPanel(new BorderLayout()); bottom.setOpaque(false);
        if (c.getAdminNote()!=null && !c.getAdminNote().isEmpty())
            bottom.add(lbl("Admin note: "+c.getAdminNote(), F_SMALL, new Color(160,210,255)), BorderLayout.WEST);

        card.add(topRow, BorderLayout.NORTH);
        card.add(body,   BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }
    int countUserComplaints(String username)
    {
        int count = 0;
        for(Complaint c : COMPLAINTS)
        {
            if(c.getUser().equals(username))
            {
                count++;
            }
        }
        return count;
    }

    // 
    //  SCREEN 5 - ADMIN DASHBOARD
    //
    void showAdminDashboard() {
    root.removeAll();
    root.setLayout(new BorderLayout());

    // ===== NORTH WRAPPER (TopBar + Stats/Filter) =====
    JPanel northWrapper = new JPanel(new BorderLayout());
    northWrapper.setOpaque(false);

    // Top bar (with logout)
    JPanel top = topBar(
        "Admin Dashboard — " + sessionUser.toUpperCase(),
        C_ACCENT2,
        () -> { sessionUser = null; showPortalSelection(); }
    );

    // Area for stats + filter
    JPanel northArea = new JPanel(new BorderLayout());
    northArea.setOpaque(false);

    northWrapper.add(top, BorderLayout.NORTH);
    northWrapper.add(northArea, BorderLayout.CENTER);

    root.add(northWrapper, BorderLayout.NORTH);

    // ===== CENTER (table) =====
    JPanel tableContainer = new JPanel();
    tableContainer.setLayout(new BoxLayout(tableContainer, BoxLayout.Y_AXIS));
    tableContainer.setOpaque(false);

    JScrollPane tableScroll = glassScroll(tableContainer);

    JPanel center = new JPanel(new BorderLayout());
    center.setOpaque(false);
    center.setBorder(new EmptyBorder(0, 22, 22, 22));
    center.add(tableScroll, BorderLayout.CENTER);

    root.add(center, BorderLayout.CENTER);

    // ===== FILTER STATE =====
    String[] filterState = {"ALL"};

    // ===== REFRESH LOGIC =====
    final Runnable[] refreshAdmin = new Runnable[1];

    refreshAdmin[0] = () -> {
        // Rebuild stats + filter
        northArea.removeAll();
        northArea.add(buildStatsStrip(), BorderLayout.NORTH);

        JPanel fb = buildFilterBar(filterState, () -> refreshAdmin[0].run());
        northArea.add(fb, BorderLayout.SOUTH);

        // Rebuild table
        tableContainer.removeAll();
        tableContainer.add(adminTableHeader());
        tableContainer.add(Box.createRigidArea(new Dimension(0, 5)));

        List<Complaint> shown = COMPLAINTS.stream()
            .filter(c -> "ALL".equals(filterState[0]) || filterState[0].equals(c.getStatus()))
            .sorted((a, b) -> b.getId() - a.getId())
            .collect(Collectors.toList());

        if (shown.isEmpty()) {
            JLabel none = lbl("No complaints match this filter.", F_BODY, C_TEXT_DIM);
            none.setAlignmentX(Component.CENTER_ALIGNMENT);
            tableContainer.add(Box.createVerticalStrut(28));
            tableContainer.add(none);
        }

        for (Complaint c : shown) {
            tableContainer.add(adminRow(c, () -> {
                refreshAdmin[0].run();
                onComplaintsChanged.run();
            }));
            tableContainer.add(Box.createRigidArea(new Dimension(0, 7)));
        }

        tableContainer.revalidate();
        tableContainer.repaint();
        northArea.revalidate();
        northArea.repaint();
    };

    // Initial load
    refreshAdmin[0].run();

    root.revalidate();
    root.repaint();
    }

    JPanel buildFilterBar(String[] filterState, Runnable onFilter) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 22, 4, 22));
        bar.add(lbl("Filter: ", F_SMALL, C_TEXT_DIM));

        String[] filters = {"ALL","Pending","Accepted","In Progress","Resolved","Rejected","Blocked"};
        Color[]  fClrs   = {C_ACCENT, C_PENDING, C_ACCEPTED, C_INPROG, C_RESOLVED, C_REJECTED, C_BLOCKED};

        for (int i = 0; i < filters.length; i++) {
            final String f = filters[i];
            final Color  c = fClrs[i];
            JButton btn = filterChip(f, c);
            btn.addActionListener(e -> { filterState[0] = f; onFilter.run(); });
            bar.add(btn);
        }
        return bar;
    }

    JPanel buildStatsStrip() {
        long total    = COMPLAINTS.size();
        long pending  = count("Pending");
        long accepted = count("Accepted");
        long inProg   = count("In Progress");
        long resolved = count("Resolved");
        long rejected = count("Rejected");
        long blocked  = count("Blocked");

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        bar.setOpaque(false);

        bar.add(statTile("Total",       total,    new Color(120,140,200,120)));
        bar.add(statTile("Pending",     pending,  withAlpha(C_PENDING,  100)));
        bar.add(statTile("Accepted",    accepted, withAlpha(C_ACCEPTED, 100)));
        bar.add(statTile("In Progress", inProg,   withAlpha(C_INPROG,   100)));
        bar.add(statTile("Resolved",    resolved, withAlpha(C_RESOLVED, 100)));
        bar.add(statTile("Rejected",    rejected, withAlpha(C_REJECTED, 100)));
        bar.add(statTile("Blocked",     blocked,  withAlpha(C_BLOCKED,  100)));
        return bar;
    }

    long count(String status) {
        return COMPLAINTS.stream().filter(c -> status.equals(c.getStatus())).count();
    }

    Color withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    JPanel statTile(String name, long cnt, Color bg) {
        JPanel tile = new JPanel(new GridLayout(2,1)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose(); super.paintComponent(g);
            }
        };
        tile.setOpaque(false);
        tile.setPreferredSize(new Dimension(124, 58));
        tile.setBorder(new EmptyBorder(5, 10, 5, 10));
        JLabel num = lbl(String.valueOf(cnt), new Font("Georgia", Font.BOLD, 24), Color.WHITE);
        num.setHorizontalAlignment(JLabel.CENTER);
        JLabel nm  = lbl(name, F_SMALL, new Color(210,220,240));
        nm.setHorizontalAlignment(JLabel.CENTER);
        tile.add(num); tile.add(nm);
        return tile;
    }
    JPanel adminTableHeader() {
    JPanel row = new JPanel(new GridLayout(1, 6));
    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
    row.setOpaque(false);

    String[] cols = {"ID","User","Category","Status","Complaint","Actions"};

    for (String col : cols) {
        JLabel h = new JLabel(col, JLabel.CENTER);
        h.setFont(F_SMALL);
        h.setForeground(new Color(140,170,230));

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(h, BorderLayout.CENTER);

        row.add(p);
    }

    return row;
}
   JPanel adminRow(Complaint c, Runnable onAction) {
    JPanel row = new JPanel(new GridLayout(1, 6));
    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
    row.setBorder(new EmptyBorder(4, 4, 4, 4));
    row.setOpaque(false);

    // ID
    JLabel id = new JLabel("#" + c.getId(), JLabel.CENTER);
    id.setFont(F_MONO);
    id.setForeground(new Color(230, 240, 255));
    row.add(id);

    // USER
    JLabel user = new JLabel(c.getUser(), JLabel.CENTER);
    user.setFont(F_BODY);
    user.setForeground(new Color(230, 240, 255));
    row.add(user);

    // CATEGORY
    JLabel cat = new JLabel(c.getCategory(), JLabel.CENTER);
    cat.setFont(F_SMALL);
    cat.setForeground(new Color(200, 215, 240));
    row.add(cat);

    // STATUS
    JPanel statusPanel = new JPanel(new GridBagLayout());
    statusPanel.setOpaque(false);
    statusPanel.add(statusBadge(c.getStatus()));
    row.add(statusPanel);

    // COMPLAINT
    String prev = c.getText().length() > 65
        ? c.getText().substring(0, 62) + "..."
        : c.getText();

    JLabel txt = new JLabel(prev, JLabel.LEFT);
    txt.setFont(F_BODY);
    txt.setForeground(new Color(230, 240, 255));
    row.add(txt);

    // ACTIONS
    row.add(adminActions(c, onAction));

    return row;
}
    /**
     *  Full lifecycle:
     *  Pending     → Accept | Reject | Block
     *  Accepted    → In Progress | Block
     *  In Progress → Resolve | Block
     *  Blocked     → Unblock (→ Pending)
     *  Resolved    → read-only
     *  Rejected    → read-only
     */
    JPanel adminActions(Complaint c, Runnable refresh) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        p.setOpaque(false);

        switch (c.getStatus()) {
            case "Pending":
                p.add(aBtn("✔ Accept",   new Color(72,213,151,200), e -> { c.setStatus("Accepted");   notePrompt(c); refresh.run(); }));
                FileDatabase.saveAll(COMPLAINTS);
                p.add(aBtn("✘ Reject",   new Color(220,70,70,200),  e -> { if(confirm("Reject",c))  { c.setStatus("Rejected");   notePrompt(c); refresh.run(); }}));
                p.add(aBtn("⊘ Block",    new Color(180,60,180,200), e -> { if(confirm("Block",c))   { c.setStatus("Blocked");    notePrompt(c); refresh.run(); }}));
                break;
            case "Accepted":
                p.add(aBtn("▶ Progress", new Color(99,179,237,200), e -> { c.setStatus("In Progress"); refresh.run(); }));
                p.add(aBtn("⊘ Block",    new Color(180,60,180,200), e -> { if(confirm("Block",c))   { c.setStatus("Blocked");    notePrompt(c); refresh.run(); }}));
                break;
            case "In Progress":
                p.add(aBtn("✔ Resolve",  new Color(72,213,151,200), e -> { c.setStatus("Resolved");   notePrompt(c); refresh.run(); }));
                p.add(aBtn("⊘ Block",    new Color(180,60,180,200), e -> { if(confirm("Block",c))   { c.setStatus("Blocked");    notePrompt(c); refresh.run(); }}));
                break;
            case "Blocked":
                p.add(aBtn("↺ Unblock",  new Color(200,175,50,200), e -> { c.setStatus("Pending"); c.setAdminNote(""); refresh.run(); }));
                break;
            case "Resolved":
                p.add(lbl("✔ Closed", F_SMALL, new Color(72,213,151))); break;
            case "Rejected":
                p.add(lbl("✘ Closed", F_SMALL, new Color(220,70,70)));  break;
        }
        return p;
    }

    boolean confirm(String action, Complaint c) {
        return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
            action + " complaint #" + c.getId() + "?",
            "Confirm " + action, JOptionPane.YES_NO_OPTION);
    }

    void notePrompt(Complaint c) {
        String n = JOptionPane.showInputDialog(this,
            "Leave a note for the student (optional):", "Admin Note", JOptionPane.PLAIN_MESSAGE);
        if (n != null && !n.trim().isEmpty()) c.setAdminNote(n.trim());
    }

    // Top navigation bar with title and logout button
    JPanel topBar(String title, Color accent, Runnable logout) {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0,0,0,140));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(accent);
                g2.fillRect(0, getHeight()-2, getWidth(), 2);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(13, 26, 13, 26));
        bar.add(lbl(title, F_HEADING, C_TEXT), BorderLayout.WEST);

       JButton logoutBtn = accentBtn("LOGOUT", new Color(255, 70, 70));
logoutBtn.setPreferredSize(new Dimension(140, 40));
logoutBtn.setFont(new Font("SansSerif", Font.BOLD, 14));

logoutBtn.addActionListener(e -> {
    int confirm = JOptionPane.showConfirmDialog(this,
        "Are you sure you want to logout?",
        "Logout",
        JOptionPane.YES_NO_OPTION);

    if (confirm == JOptionPane.YES_OPTION) {
        logout.run();
    }
});

bar.add(logoutBtn, BorderLayout.EAST);
return bar;
}

    // Glass panel with rounded corners and transparent fill
    //  GLASS PANEL
    // 
    class GlassPanel extends JPanel {
        final int arc;
        GlassPanel(LayoutManager lm, int arc) { super(lm); this.arc = arc; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(C_GLASS_FILL);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.setStroke(new BasicStroke(1.4f));
            g2.setColor(C_GLASS_EDGE);
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, arc, arc);
            g2.dispose(); super.paintComponent(g);
        }
    }

    // Handles painting the slideshow background
    //  BACKGROUND PANEL
    // 
    class BackgroundPanel extends JPanel {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            if (!bgImages.isEmpty()) {
                g2.drawImage(bgImages.get(bgIndex), 0, 0, getWidth(), getHeight(), this);
                g2.setColor(new Color(5, 12, 35, 175));
                g2.fillRect(0, 0, getWidth(), getHeight());
            } else {
                g2.setPaint(new GradientPaint(0, 0, C_BG_DARK, getWidth(), getHeight(), C_BG_MID));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.dispose();
        }
    }

    // Helper methods for creating styled components
    //  COMPONENT FACTORIES
    //
    static JLabel lbl(String t, Font f, Color c)  { JLabel l=new JLabel(t); l.setFont(f); l.setForeground(c); return l; }
    static JLabel ctrLbl(String t, Font f, Color c){ JLabel l=lbl(t,f,c); l.setHorizontalAlignment(JLabel.CENTER); return l; }
    static JLabel fieldLbl(String t) { return lbl(t, F_SMALL, C_TEXT_DIM); }

    static JTextField styledField() {
        JTextField f = new JTextField();
        f.setFont(F_BODY); f.setForeground(C_TEXT); f.setCaretColor(C_TEXT);
        f.setBackground(new Color(255,255,255,28));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_GLASS_EDGE, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        f.setPreferredSize(new Dimension(380, 40));
        return f;
    }

    static JPasswordField styledPass() {
        JPasswordField f = new JPasswordField();
        f.setFont(F_BODY); f.setForeground(C_TEXT); f.setCaretColor(C_TEXT);
        f.setBackground(new Color(255,255,255,28));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_GLASS_EDGE, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        f.setPreferredSize(new Dimension(380, 40));
        return f;
    }

    static void styleTA(JTextArea a) {
        a.setFont(F_BODY); a.setForeground(C_TEXT); a.setCaretColor(C_TEXT);
        a.setBackground(new Color(255,255,255,18));
        a.setBorder(new EmptyBorder(10, 12, 10, 12));
        a.setLineWrap(true); a.setWrapStyleWord(true);
    }

    static JScrollPane glassScroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setOpaque(false); sp.getViewport().setOpaque(false);
        sp.setBorder(null); sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    static JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(F_BODY); cb.setForeground(C_TEXT);
        cb.setBackground(new Color(28, 38, 78));
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return cb;
    }

    static JButton accentBtn(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose(); super.paintComponent(g);
            }
        };
        b.setForeground(Color.WHITE); b.setFont(F_HEADING);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JButton ghostBtn(String text) {
        JButton b = new JButton(text);
        b.setForeground(C_TEXT_DIM); b.setFont(F_BODY);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JButton aBtn(String text, Color bg, ActionListener al) {
        JButton b = accentBtn(text, bg);
        b.setFont(F_SMALL); b.setPreferredSize(new Dimension(106, 30));
        b.addActionListener(al); return b;
    }

    static JButton filterChip(String text, Color accent) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), getModel().isRollover() ? 150 : 55));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(accent); g2.setStroke(new BasicStroke(1.1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 18, 18);
                g2.dispose(); super.paintComponent(g);
            }
        };
        b.setForeground(Color.WHITE); b.setFont(F_SMALL);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(102, 26));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JLabel statusBadge(String status) {
        JLabel l = new JLabel(status, JLabel.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Color sc = statusColor(getText());
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(sc.getRed(), sc.getGreen(), sc.getBlue(), 55));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose(); super.paintComponent(g);
            }
        };
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(statusColor(status)); l.setOpaque(false);
        l.setBorder(new EmptyBorder(3, 8, 3, 8));
        l.setPreferredSize(new Dimension(96, 22));
        return l;
    }

    static Color statusColor(String s) {
        switch (s) {
            case "Pending":     return C_PENDING;
            case "Accepted":    return C_ACCEPTED;
            case "In Progress": return C_INPROG;
            case "Resolved":    return C_RESOLVED;
            case "Rejected":    return C_REJECTED;
            case "Blocked":     return C_BLOCKED;
            default:            return Color.WHITE;
        }
    }

    static Color priColor(String p) {
        switch (p) {
            case "Critical": return new Color(255, 75,  75);
            case "High":     return new Color(255,158,  55);
            case "Medium":   return new Color(255,215,  70);
            default:         return new Color(110,200, 110);
        }
    }

    void shake(Component c) {
        Point orig = c.getLocation();
        int[] offs = {10,-10,8,-8,5,-5,2,-2,0};
        int[] i = {0};
        Timer t = new Timer(28, null);
        t.addActionListener(e -> {
            if (i[0] >= offs.length) { c.setLocation(orig); t.stop(); return; }
            c.setLocation(orig.x + offs[i[0]++], orig.y);
        });
        t.start();
    }
    // Entry point - sets up look and feel then launches the window
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        UIManager.put("ComboBox.background",       new Color(28, 38, 78));
        UIManager.put("ComboBox.foreground",       Color.WHITE);
        UIManager.put("TextField.background",      new Color(18, 26, 58));
        UIManager.put("TextField.foreground",      Color.WHITE);
        UIManager.put("PasswordField.background",  new Color(18, 26, 58));
        UIManager.put("PasswordField.foreground",  Color.WHITE);
        SwingUtilities.invokeLater(multi4::new);
    }
    }
   
