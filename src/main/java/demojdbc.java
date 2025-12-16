import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileOutputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Random;
// --- iText PDF IMPORTS ---
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.FontFactory;

public class demojdbc extends JFrame {

    private static final long serialVersionUID = 1L;

    // --- DATABASE CONFIGURATION ---
    static final String URL = "jdbc:mysql://localhost:3306/hoteldb";
    static final String USER = "root";
    static final String PASS = "database@2007"; 

    private Connection conn;
    private JTable roomTable;
    private DefaultTableModel tableModel;
    
    // --- DASHBOARD LABELS ---
    private JLabel lblTotal, lblBooked, lblAvailable;

    // --- COLOR PALETTE ---
    private static final Color BG_DARK       = new Color(30, 30, 40);
    private static final Color BG_SIDEBAR    = new Color(39, 41, 61);
    private static final Color BG_CARD       = new Color(39, 41, 61);
    private static final Color TEXT_WHITE    = new Color(245, 245, 245);
    private static final Color ACCENT_BLUE   = new Color(29, 140, 248);
    private static final Color ACCENT_GREEN  = new Color(66, 184, 131);
    private static final Color ACCENT_RED    = new Color(253, 93, 147);
    private static final Color ACCENT_PURPLE = new Color(156, 39, 176); // New Color for Reports
    private static final Color TABLE_HEADER  = new Color(45, 48, 70);

    public demojdbc() {
        // 1. DB CONNECTION
        try {
            conn = DriverManager.getConnection(URL, USER, PASS);
            initHistoryTable(); // AUTOMATICALLY CREATE HISTORY TABLE
        } catch (SQLException e) {
            handleDatabaseError(e);
        }

        // 2. FRAME SETUP
        setTitle("Grand Hotel - Executive Dashboard");
        setSize(1380, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 0));

        // 3. ADD COMPONENTS
        add(createSidebar(), BorderLayout.WEST);
        
        JPanel mainContent = new JPanel(new BorderLayout(20, 20));
        mainContent.setBackground(BG_DARK);
        mainContent.setBorder(new EmptyBorder(30, 30, 30, 30));
        
        mainContent.add(createStatsPanel(), BorderLayout.NORTH);
        mainContent.add(createModernTable(), BorderLayout.CENTER);
        
        add(mainContent, BorderLayout.CENTER);

        // 4. LOAD DATA
        refreshRoomView();
    }

    // --- AUTOMATIC HISTORY TABLE CREATION ---
    private void initHistoryTable() {
        try {
            String sql = "CREATE TABLE IF NOT EXISTS hotel_history (" +
                         "id INT AUTO_INCREMENT PRIMARY KEY, " +
                         "action_type VARCHAR(20), " + // 'CHECKIN' or 'CHECKOUT'
                         "amount DOUBLE, " +
                         "event_date DATETIME)";
            conn.createStatement().executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- UI: SIDEBAR ---
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, new Color(30, 30, 40), 0, getHeight(), new Color(20, 20, 30));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        // Increased rows to fit new button
        sidebar.setLayout(new GridLayout(10, 1, 0, 15)); 
        sidebar.setBorder(new EmptyBorder(30, 20, 30, 20));
        sidebar.setPreferredSize(new Dimension(260, 0));

        JLabel logo = new JLabel("<html><center><font color='#1d8cf8'>GRAND</font><br><font color='white'>HOTEL</font></center></html>", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 32));
        sidebar.add(logo);
        sidebar.add(new JSeparator(SwingConstants.HORIZONTAL));

        sidebar.add(new ModernButton("✔  Book Room", ACCENT_BLUE, e -> showBookingDialog()));
        sidebar.add(new ModernButton("❓  Check Availability", new Color(255, 160, 0), e -> showAvailableRoomsDialog()));
        sidebar.add(new ModernButton("🔍  Search Guest", new Color(100, 100, 200), e -> searchCustomer()));
        sidebar.add(new ModernButton("💳  Check-Out", ACCENT_RED, e -> showCheckOutDialog()));
        sidebar.add(new ModernButton("📊  Revenue Report", ACCENT_PURPLE, e -> showRevenueReport())); // NEW BUTTON
        sidebar.add(new ModernButton("🔄  Refresh Data", new Color(100, 100, 100), e -> refreshRoomView()));
        
        return sidebar;
    }
    
    // --- UI: STATS ---
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 30, 0));
        panel.setBackground(BG_DARK);
        panel.setPreferredSize(new Dimension(0, 120));

        lblTotal = createCard("Total Rooms", "0", new Color(45, 206, 137));
        lblBooked = createCard("Occupied", "0", new Color(245, 54, 92));
        lblAvailable = createCard("Available", "0", new Color(17, 205, 239));

        panel.add(lblTotal);
        panel.add(lblBooked);
        panel.add(lblAvailable);
        return panel;
    }

    private JLabel createCard(String title, String value, Color accent) {
        JLabel card = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(accent);
                g2.fillRoundRect(20, 20, 5, 40, 5, 5);
                super.paintComponent(g);
            }
        };
        card.setText("<html><div style='margin-left:15px;'><font color='#9a9a9a' size='4'>" + title + "</font><br><font color='white' size='6'>" + value + "</font></div></html>");
        card.setBorder(new EmptyBorder(10, 20, 10, 10));
        return card;
    }

    // --- UI: TABLE ---
    private JScrollPane createModernTable() {
        String[] columnNames = {"Room", "Type", "Price/Day", "Status", "Customer", "Phone", "Aadhaar", "Check-In"};
        
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        roomTable = new JTable(tableModel);
        roomTable.setBackground(BG_CARD);
        roomTable.setForeground(TEXT_WHITE);
        roomTable.setRowHeight(50);
        roomTable.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        roomTable.setShowVerticalLines(false);
        roomTable.setShowHorizontalLines(true);
        roomTable.setGridColor(new Color(60, 60, 70));
        roomTable.setSelectionBackground(new Color(29, 140, 248, 50));
        roomTable.setSelectionForeground(Color.WHITE);

        JTableHeader header = roomTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(TABLE_HEADER);
        header.setForeground(ACCENT_BLUE);
        header.setPreferredSize(new Dimension(0, 40));
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setBackground(BG_CARD);
        for(int i=0; i<roomTable.getColumnCount(); i++) roomTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);

        roomTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) value;
                l.setText(status);
                l.setForeground(status.equals("Booked") ? ACCENT_RED : ACCENT_GREEN);
                l.setBackground(BG_CARD);
                l.setFont(new Font("Segoe UI", Font.BOLD, 14));
                return l;
            }
        });

        JScrollPane scrollPane = new JScrollPane(roomTable);
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    class ModernButton extends JButton {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Color baseColor;
        private boolean isHovered = false;
        public ModernButton(String text, Color color, ActionListener action) {
            super(text); this.baseColor = color; setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(Color.WHITE); setFocusPainted(false); setBorderPainted(false); setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR)); addActionListener(action);
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
            });
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isHovered) { g2.setColor(baseColor.brighter()); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15); }
            else { g2.setColor(new Color(50, 50, 60)); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15); g2.setColor(baseColor); g2.fillRoundRect(0, 0, 6, getHeight(), 15, 15); }
            super.paintComponent(g);
        }
    }

    // --- NEW: REVENUE REPORT DIALOG ---
    private void showRevenueReport() {
        JDialog dialog = new JDialog(this, "Financial Overview", true);
        dialog.setSize(900, 500);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(BG_DARK);
        dialog.setLayout(new BorderLayout(20, 20));

        JPanel header = new JPanel(); header.setBackground(BG_SIDEBAR);
        JLabel title = new JLabel("Revenue & Occupancy Statistics"); 
        title.setFont(new Font("Segoe UI", Font.BOLD, 24)); 
        title.setForeground(Color.WHITE); header.add(title);
        dialog.add(header, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(2, 3, 20, 20));
        gridPanel.setBackground(BG_DARK);
        gridPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // METRICS VARIABLES
        double revToday = 0, revMonth = 0;
        int checkInDay = 0, checkInMonth = 0;
        int checkOutDay = 0, checkOutMonth = 0;

        try {
            // Calculate Today's Stats
            String sqlDay = "SELECT action_type, SUM(amount) as total, COUNT(*) as cnt FROM hotel_history WHERE DATE(event_date) = CURDATE() GROUP BY action_type";
            ResultSet rsDay = conn.createStatement().executeQuery(sqlDay);
            while(rsDay.next()) {
                String type = rsDay.getString("action_type");
                if (type.equals("CHECKIN")) checkInDay = rsDay.getInt("cnt");
                if (type.equals("CHECKOUT")) {
                    checkOutDay = rsDay.getInt("cnt");
                    revToday = rsDay.getDouble("total");
                }
            }

            // Calculate Month's Stats
            String sqlMonth = "SELECT action_type, SUM(amount) as total, COUNT(*) as cnt FROM hotel_history WHERE MONTH(event_date) = MONTH(CURRENT_DATE()) AND YEAR(event_date) = YEAR(CURRENT_DATE()) GROUP BY action_type";
            ResultSet rsMonth = conn.createStatement().executeQuery(sqlMonth);
            while(rsMonth.next()) {
                String type = rsMonth.getString("action_type");
                if (type.equals("CHECKIN")) checkInMonth = rsMonth.getInt("cnt");
                if (type.equals("CHECKOUT")) {
                    checkOutMonth = rsMonth.getInt("cnt");
                    revMonth = rsMonth.getDouble("total");
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        // Add Cards
        gridPanel.add(createReportCard("Today's Revenue", String.format("₹%,.0f", revToday), ACCENT_GREEN));
        gridPanel.add(createReportCard("Today's Check-Ins", String.valueOf(checkInDay), ACCENT_BLUE));
        gridPanel.add(createReportCard("Today's Check-Outs", String.valueOf(checkOutDay), ACCENT_RED));
        
        gridPanel.add(createReportCard("This Month Revenue", String.format("₹%,.0f", revMonth), ACCENT_GREEN));
        gridPanel.add(createReportCard("Month's Check-Ins", String.valueOf(checkInMonth), ACCENT_BLUE));
        gridPanel.add(createReportCard("Month's Check-Outs", String.valueOf(checkOutMonth), ACCENT_RED));

        dialog.add(gridPanel, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private JPanel createReportCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createMatteBorder(0, 5, 0, 0, color));
        
        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblTitle.setForeground(Color.GRAY);
        
        JLabel lblValue = new JLabel(value, SwingConstants.CENTER);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 36));
        lblValue.setForeground(Color.WHITE);
        
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        return card;
    }

    // --- EXISTING LOGIC WITH HISTORY TRACKING UPDATES ---

    private void refreshRoomView() {
        tableModel.setRowCount(0);
        int total = 0, booked = 0;
        String sql = "SELECT * FROM rooms ORDER BY roomNumber ASC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                total++; if(rs.getBoolean("isBooked")) booked++;
                Timestamp ts = rs.getTimestamp("bookingDate");
                String dateStr = (ts != null) ? new SimpleDateFormat("yyyy-MM-dd").format(ts) : "—";
                String aadhaar = rs.getString("aadhaar_last4");
                Object[] row = {
                    rs.getInt("roomNumber"), rs.getString("type"), String.format("₹%,.0f", rs.getDouble("pricePerDay")),
                    rs.getBoolean("isBooked") ? "Booked" : "Available",
                    rs.getString("customerName") == null ? "—" : rs.getString("customerName"),
                    rs.getString("customerPhone") == null ? "—" : rs.getString("customerPhone"),
                    aadhaar == null ? "—" : aadhaar, dateStr
                };
                tableModel.addRow(row);
            }
            lblTotal.setText("<html><div style='margin-left:15px;'><font color='#9a9a9a' size='4'>Total Rooms</font><br><font color='white' size='6'>" + total + "</font></div></html>");
            lblBooked.setText("<html><div style='margin-left:15px;'><font color='#9a9a9a' size='4'>Occupied</font><br><font color='white' size='6'>" + booked + "</font></div></html>");
            lblAvailable.setText("<html><div style='margin-left:15px;'><font color='#9a9a9a' size='4'>Available</font><br><font color='white' size='6'>" + (total - booked) + "</font></div></html>");
        } catch (SQLException e) { handleDatabaseError(e); }
    }

    private void showBookingDialog() {
        JFrame frame = new JFrame("Book a Room");
        frame.setSize(750, 850); 
        frame.setLocationRelativeTo(this);
        frame.setLayout(new BorderLayout(15, 15));
        frame.getContentPane().setBackground(new Color(240, 240, 245));

        JPanel header = new JPanel(); header.setBackground(BG_SIDEBAR);
        JLabel title = new JLabel("New Reservation"); title.setFont(new Font("Segoe UI", Font.BOLD, 22)); title.setForeground(Color.WHITE); header.add(title);
        frame.add(header, BorderLayout.NORTH);

        String[] roomTypes = {"Standard", "Deluxe", "Suite"};
        JComboBox<String> typeBox = new JComboBox<>(roomTypes);
        typeBox.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        JPanel previewPanel = new JPanel(new BorderLayout(10, 10));
        previewPanel.setBackground(Color.WHITE);
        previewPanel.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(10, 20, 10, 20), BorderFactory.createLineBorder(new Color(200, 200, 200), 1)));
        
        JLabel imageLabel = new JLabel("", SwingConstants.CENTER); imageLabel.setPreferredSize(new Dimension(600, 350));
        JLabel descLabel = new JLabel("", SwingConstants.CENTER); descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16)); descLabel.setBorder(new EmptyBorder(10, 0, 10, 0));

        updateRoomPreview("Standard", imageLabel, descLabel);
        typeBox.addActionListener(e -> updateRoomPreview((String) typeBox.getSelectedItem(), imageLabel, descLabel));
        
        previewPanel.add(imageLabel, BorderLayout.CENTER); previewPanel.add(descLabel, BorderLayout.SOUTH);

        JPanel inputPanel = new JPanel(new GridLayout(7, 2, 15, 15));
        inputPanel.setBackground(new Color(240, 240, 245)); inputPanel.setBorder(new EmptyBorder(10, 40, 20, 40));
        
        JTextField roomField = new JTextField(); JTextField nameField = new JTextField();
        JTextField phoneField = new JTextField(); JTextField daysField = new JTextField();
        JTextField aadhaarField = new JTextField(); JCheckBox foodCheck = new JCheckBox("Include Food Service (+₹1000/day)");
        foodCheck.setBackground(new Color(240, 240, 245));

        inputPanel.add(new JLabel("Room Number:")); inputPanel.add(roomField);
        inputPanel.add(new JLabel("Customer Name:")); inputPanel.add(nameField);
        inputPanel.add(new JLabel("Phone Number:")); inputPanel.add(phoneField);
        inputPanel.add(new JLabel("Stay Duration (days):")); inputPanel.add(daysField);
        inputPanel.add(new JLabel("Room Type:")); inputPanel.add(typeBox);
        inputPanel.add(new JLabel("Aadhaar Last 4:")); inputPanel.add(aadhaarField);
        inputPanel.add(new JLabel("Food Service:")); inputPanel.add(foodCheck);

        JPanel centerPanel = new JPanel(new BorderLayout()); centerPanel.add(previewPanel, BorderLayout.CENTER); centerPanel.add(inputPanel, BorderLayout.SOUTH);
        frame.add(centerPanel, BorderLayout.CENTER);

        JButton confirmBtn = new JButton("CONFIRM BOOKING");
        confirmBtn.setBackground(ACCENT_GREEN); confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFont(new Font("Segoe UI", Font.BOLD, 18)); confirmBtn.setPreferredSize(new Dimension(0, 50));
        confirmBtn.addActionListener(e -> {
            try {
                int roomNumber = Integer.parseInt(roomField.getText().trim());
                String aadhaarLast4 = aadhaarField.getText().trim();
                if (nameField.getText().isEmpty() || phoneField.getText().isEmpty()) { JOptionPane.showMessageDialog(frame, "Name and Phone cannot be empty."); return; }
                if (!aadhaarLast4.matches("\\d{4}")) { JOptionPane.showMessageDialog(frame, "Aadhaar must be 4 digits."); return; }

                String checkSQL = "SELECT isBooked FROM rooms WHERE roomNumber=?";
                PreparedStatement psCheck = conn.prepareStatement(checkSQL); psCheck.setInt(1, roomNumber); ResultSet rs = psCheck.executeQuery();
                if (rs.next() && !rs.getBoolean("isBooked")) {
                    String updateSQL = "UPDATE rooms SET isBooked=TRUE, customerName=?, customerPhone=?, daysStayed=?, bookingDate=NOW(), type=?, foodOrdered=?, aadhaar_last4=? WHERE roomNumber=?";
                    PreparedStatement psUpdate = conn.prepareStatement(updateSQL);
                    psUpdate.setString(1, nameField.getText()); psUpdate.setString(2, phoneField.getText()); psUpdate.setInt(3, Integer.parseInt(daysField.getText()));
                    psUpdate.setString(4, (String)typeBox.getSelectedItem()); psUpdate.setBoolean(5, foodCheck.isSelected()); psUpdate.setString(6, aadhaarLast4); psUpdate.setInt(7, roomNumber);
                    psUpdate.executeUpdate();
                    
                    // --- TRACK HISTORY (CHECK IN) ---
                    String histSQL = "INSERT INTO hotel_history (action_type, amount, event_date) VALUES ('CHECKIN', 0, NOW())";
                    conn.createStatement().executeUpdate(histSQL);

                    JOptionPane.showMessageDialog(frame, "✅ Booking Successful!"); frame.dispose(); refreshRoomView();
                } else { JOptionPane.showMessageDialog(frame, "Room unavailable."); }
            } catch (Exception ex) { JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage()); }
        });
        frame.add(confirmBtn, BorderLayout.SOUTH); frame.setVisible(true);
    }

    private void showCheckOutDialog() {
        String roomNumStr = JOptionPane.showInputDialog(this, "Enter Room Number to Check-Out:");
        if (roomNumStr == null) return;
        try {
            int roomNumber = Integer.parseInt(roomNumStr);
            String sql = "SELECT * FROM rooms WHERE roomNumber=? AND isBooked=TRUE";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, roomNumber);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String name = rs.getString("customerName");
                String phone = rs.getString("customerPhone");
                String rType = rs.getString("type");
                String aadhaar = rs.getString("aadhaar_last4");
                Timestamp bookingTs = rs.getTimestamp("bookingDate");
                
                int days = rs.getInt("daysStayed");
                double pricePerDay = rs.getDouble("pricePerDay");
                boolean foodOrdered = rs.getBoolean("foodOrdered");

                double foodCost = foodOrdered ? 1000 * days : 0;
                double subTotal = (pricePerDay * days) + foodCost;
                double taxRate = (pricePerDay > 7500) ? 0.18 : (pricePerDay >= 1000) ? 0.05 : 0;
                double taxAmount = subTotal * taxRate;
                double grandTotal = subTotal + taxAmount;
                
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                String checkInTime = (bookingTs != null) ? sdf.format(bookingTs) : "N/A";
                String checkOutTime = sdf.format(new java.util.Date());

                String[] options = {"Cash", "G-Pay"};
                int choice = JOptionPane.showOptionDialog(this, "Select Payment Mode", "Payment", 0, 3, null, options, options[0]);
                String paymentMode = (choice == 1) ? "G-Pay" : "Cash";

                String receiptText = generateDetailedReceipt(name, phone, aadhaar, rType, roomNumber, checkInTime, checkOutTime, days, pricePerDay, foodCost, subTotal, taxRate, taxAmount, grandTotal, paymentMode);
                
                JDialog billDialog = new JDialog(this, "Official Invoice", true);
                billDialog.setSize(600, 800); billDialog.setLayout(new BorderLayout()); billDialog.setLocationRelativeTo(this);

                JTextArea billArea = new JTextArea(receiptText);
                billArea.setFont(new Font("Monospaced", Font.BOLD, 13)); billArea.setEditable(false);
                billArea.setBorder(new EmptyBorder(20, 20, 20, 20)); billArea.setBackground(Color.WHITE);
                
                billDialog.add(billArea, BorderLayout.CENTER);

                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
                JButton btnPrint = new JButton("Save PDF"); JButton btnPay = new JButton("Pay & Close");
                
                btnPrint.addActionListener(e -> saveReceiptAsPDF("Invoice_" + roomNumber + ".pdf", receiptText));
                btnPay.addActionListener(e -> {
                    if (paymentMode.equals("G-Pay") && new java.io.File("images/gpay_qr.png").exists()) {
                        JOptionPane.showMessageDialog(billDialog, "Scan QR", "Payment", 1, resizeImage(new ImageIcon("images/gpay_qr.png"), 200, 200));
                    }
                    try {
                        conn.createStatement().executeUpdate("UPDATE rooms SET isBooked=FALSE, customerName=NULL, customerPhone=NULL, aadhaar_last4=NULL, daysStayed=0, bookingDate=NULL, foodOrdered=FALSE WHERE roomNumber=" + roomNumber);
                        
                        // --- TRACK HISTORY (CHECK OUT REVENUE) ---
                        String histSQL = "INSERT INTO hotel_history (action_type, amount, event_date) VALUES ('CHECKOUT', " + grandTotal + ", NOW())";
                        conn.createStatement().executeUpdate(histSQL);

                        refreshRoomView();
                        billDialog.dispose();
                    } catch (SQLException ex) { ex.printStackTrace(); }
                });

                btnPanel.add(btnPrint); btnPanel.add(btnPay);
                billDialog.add(btnPanel, BorderLayout.SOUTH);
                billDialog.setVisible(true);
            } else { JOptionPane.showMessageDialog(this, "Room not found or not booked."); }
        } catch (Exception e) { handleDatabaseError(e); }
    }

    private String generateDetailedReceipt(String name, String phone, String aadhaar, String type, int room, 
                                         String checkIn, String checkOut, int days, double price, 
                                         double foodCost, double subTotal, double taxRate, double taxAmt, 
                                         double grandTotal, String payMode) {
        
        String invoiceNo = "INV-" + (1000 + new Random().nextInt(9000));
        double cgst = taxAmt / 2.0;
        double sgst = taxAmt / 2.0;
        double gstPercent = taxRate * 100;

        return 
        "===============================================================\n" +
        "                 GRAND HOTEL - OFFICIAL INVOICE                \n" +
        "===============================================================\n\n" +
        "  123 Luxury Avenue, Tech City, India \n" +
        "  GSTIN: 29ABCDE1234F1Z5 | +91 9876543210 \n\n" +
        "  Invoice No : " + invoiceNo + "             Date: " + checkOut.substring(0,10) + "\n" +
        "---------------------------------------------------------------\n" +
        "  GUEST DETAILS:\n" +
        String.format("  Name       : %-20s  Room No: %d\n", name, room) +
        String.format("  Phone      : %-20s  Type   : %s\n", phone, type) +
        String.format("  Aadhaar    : XXXX-XXXX-%-4s\n", (aadhaar!=null?aadhaar:"XXXX")) +
        String.format("  Check-In   : %-20s\n  Check-Out  : %s\n", checkIn, checkOut) +
        "---------------------------------------------------------------\n" +
        "  DESCRIPTION           RATE      DAYS      AMOUNT (INR)\n" +
        "---------------------------------------------------------------\n" +
        String.format("  Room Charges          %-9.2f %-9d %10.2f\n", price, days, (price*days)) +
        (foodCost > 0 ? String.format("  Food & Bev            %-9s %-9d %10.2f\n", "1000.00", days, foodCost) : "") +
        "---------------------------------------------------------------\n" +
        String.format("  SUB TOTAL                                 %10.2f\n", subTotal) +
        String.format("  CGST (%.1f%%)                               %10.2f\n", gstPercent/2, cgst) +
        String.format("  SGST (%.1f%%)                               %10.2f\n", gstPercent/2, sgst) +
        "===============================================================\n" +
        String.format("  GRAND TOTAL                               %10.2f\n", grandTotal) +
        "===============================================================\n" +
        "  Payment Mode : " + payMode + "\n\n" +
        "  Terms: This is a computer generated invoice.\n" +
        "         Thank you for your stay!\n" +
        "===============================================================\n";
    }

    private void updateRoomPreview(String type, JLabel imageLabel, JLabel descLabel) {
        String imgPath = switch (type) {
            case "Deluxe" -> "images/Deluxe.jpeg";
            case "Suite" -> "images/Suite.jpeg";
            default -> "images/Standard.jpeg";
        };
        if(new java.io.File(imgPath).exists()) imageLabel.setIcon(resizeImage(new ImageIcon(imgPath), 600, 350));
        
        String desc = switch (type) {
            case "Deluxe" -> "<html><center><b style='font-size:14px; color:#2E8B57'>Deluxe Room (₹4500)</b><br>King Bed • AC • City View • Free WiFi</center></html>";
            case "Suite" -> "<html><center><b style='font-size:14px; color:#DAA520'>Luxury Suite (₹7500)</b><br>Jacuzzi • Lounge • Breakfast • Ocean View</center></html>";
            default -> "<html><center><b style='font-size:14px; color:#4682B4'>Standard Room (₹2500)</b><br>Queen Bed • Basic Amenities • WiFi</center></html>";
        };
        descLabel.setText(desc);
    }

    private ImageIcon resizeImage(ImageIcon icon, int width, int height) {
        return new ImageIcon(icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    private void searchCustomer() {
        String key = JOptionPane.showInputDialog(this, "Enter Name/Phone:");
        if(key == null) return;
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM rooms WHERE (customerName LIKE ? OR customerPhone LIKE ?) AND isBooked=TRUE");
            ps.setString(1, "%"+key+"%"); ps.setString(2, "%"+key+"%");
            ResultSet rs = ps.executeQuery();
            if(rs.next()) JOptionPane.showMessageDialog(this, "Found in Room " + rs.getInt("roomNumber") + "\nAadhaar: " + rs.getString("aadhaar_last4"));
            else JOptionPane.showMessageDialog(this, "Not Found");
        } catch(SQLException e) { handleDatabaseError(e); }
    }

    private void showAvailableRoomsDialog() {
        try {
            ResultSet rs = conn.createStatement().executeQuery("SELECT roomNumber, type FROM rooms WHERE isBooked=FALSE");
            StringBuilder sb = new StringBuilder("Available Rooms:\n");
            while(rs.next()) sb.append(rs.getString("type")).append(": ").append(rs.getInt("roomNumber")).append("\n");
            JOptionPane.showMessageDialog(this, new JScrollPane(new JTextArea(sb.toString(), 10, 30)));
        } catch(SQLException e) { handleDatabaseError(e); }
    }

    private void saveReceiptAsPDF(String fileName, String content) {
        try {
            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(fileName));
            doc.open();
            doc.add(new Paragraph(content, FontFactory.getFont(FontFactory.COURIER, 10)));
            doc.close();
            JOptionPane.showMessageDialog(this, "Saved: " + fileName);
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "PDF Error"); }
    }

    private void handleDatabaseError(Exception e) { e.printStackTrace(); JOptionPane.showMessageDialog(this, "DB Error: " + e.getMessage()); }
    
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings","on");
        SwingUtilities.invokeLater(() -> new demojdbc().setVisible(true));
    }
}