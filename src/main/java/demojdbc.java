
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class demojdbc extends JFrame {

	private static final long serialVersionUID = 1L;
	
    static final String URL = "jdbc:mysql://localhost:3306/hoteldb";
    static final String USER = "root";
    static final String PASS = "database@2007"; 

    private Connection conn;

   
    private JTable roomTable;
    private DefaultTableModel tableModel;

  
    private static final Color PRIMARY_COLOR = new Color(70, 130, 180); 
    private static final Color BACKGROUND_COLOR = new Color(245, 245, 245); 
    private static final Color FONT_COLOR = new Color(255, 255, 255);
    private static final Font HEADING_FONT = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font TABLE_FONT = new Font("Segoe UI", Font.PLAIN, 14);


    public demojdbc() {
        // --- 1. DATABASE CONNECTION ---
        try {
            conn = DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException e) {
            handleDatabaseError(e);
        }

        // --- 2. INITIALIZE THE FRAME ---
        setTitle("Grand Hotel Management System");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND_COLOR);
        setLayout(new BorderLayout(10, 10));

        // --- 3. CREATE UI PANELS ---
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        // --- 4. LOAD INITIAL DATA ---
        refreshRoomView();
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(new EmptyBorder(15, 10, 15, 10));

        JLabel titleLabel = new JLabel("Grand Hotel Management");
        titleLabel.setFont(HEADING_FONT);
        titleLabel.setForeground(FONT_COLOR);
        return headerPanel;
    }

    private JScrollPane createTablePanel() {
        String[] columnNames = {"Room No", "Type", "Price/Day", "Status", "Customer Name", "Phone", "Booked On"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        roomTable = new JTable(tableModel);
        styleTable();

        JScrollPane scrollPane = new JScrollPane(roomTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        return scrollPane;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.setBorder(new EmptyBorder(10, 10, 20, 10));

        JButton bookButton = createStyledButton("✔ Book Room", new Color(46, 139, 87));
        JButton availableButton = createStyledButton("❓ View Available", new Color(255, 165, 0));
        JButton searchButton = createStyledButton("🔍 Search Customer", new Color(30, 144, 255));
        JButton checkOutButton = createStyledButton("💳 Check-Out & Bill", new Color(220, 20, 60));
        JButton refreshButton = createStyledButton("🔄 Refresh", new Color(119, 136, 153));

        bookButton.addActionListener(e -> showBookingDialog());
        availableButton.addActionListener(e -> showAvailableRoomsDialog());
        searchButton.addActionListener(e -> searchCustomer());
        checkOutButton.addActionListener(e -> showCheckOutDialog());
        refreshButton.addActionListener(e -> refreshRoomView());
        
        buttonPanel.add(bookButton);
        buttonPanel.add(availableButton);
        buttonPanel.add(searchButton);
        buttonPanel.add(checkOutButton);
        buttonPanel.add(refreshButton);

        return buttonPanel;
    }
    
    private void showBookingDialog() {
        JFrame frame = new JFrame("Book a Room");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 700);
        frame.setLayout(new BorderLayout(10, 10));

        // Title
        JLabel title = new JLabel("Book Your Stay at Grand Hotel", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setBorder(new EmptyBorder(10, 0, 10, 0));
        frame.add(title, BorderLayout.NORTH);

        // === Room Type ComboBox ===
        String[] roomTypes = {"Standard", "Deluxe", "Suite"};
        JComboBox<String> typeBox = new JComboBox<>(roomTypes);

        // === Image + Description Panel ===
        JPanel previewPanel = new JPanel(new BorderLayout(5, 5));
        JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        JLabel descLabel = new JLabel("", SwingConstants.CENTER);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // --- Load Default Image and Description ---
        updateRoomPreview("Standard", imageLabel, descLabel);

        // Update image and description on room type change
        typeBox.addActionListener(e -> {
            String selectedType = (String) typeBox.getSelectedItem();
            updateRoomPreview(selectedType, imageLabel, descLabel);
        });

        previewPanel.add(imageLabel, BorderLayout.CENTER);
        previewPanel.add(descLabel, BorderLayout.SOUTH);

        // === Input Fields ===
        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        inputPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        JTextField roomField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField daysField = new JTextField();
        JCheckBox foodCheck = new JCheckBox("Include Food Service (+₹1000/day)");

        inputPanel.add(new JLabel("Room Number:"));
        inputPanel.add(roomField);

        inputPanel.add(new JLabel("Customer Name:"));
        inputPanel.add(nameField);

        inputPanel.add(new JLabel("Phone Number:"));
        inputPanel.add(phoneField);

        inputPanel.add(new JLabel("Stay Duration (days):"));
        inputPanel.add(daysField);

        inputPanel.add(new JLabel("Room Type:"));
        inputPanel.add(typeBox);

        inputPanel.add(new JLabel("Food Service:"));
        inputPanel.add(foodCheck);

        // === Combine Panels ===
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(previewPanel, BorderLayout.CENTER);
        centerPanel.add(inputPanel, BorderLayout.SOUTH);
        frame.add(centerPanel, BorderLayout.CENTER);

        // === Confirm Button ===
        JButton confirmBtn = new JButton("Book Now");
        confirmBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        confirmBtn.setBackground(new Color(46, 139, 87));
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFocusPainted(false);
        confirmBtn.addActionListener(e -> {
            try {
                int roomNumber = Integer.parseInt(roomField.getText());
                String name = nameField.getText().trim();
                String phone = phoneField.getText().trim();
                int days = Integer.parseInt(daysField.getText());
                String roomType = (String) typeBox.getSelectedItem();
                boolean wantsFood = foodCheck.isSelected();

                if (name.isEmpty() || phone.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Name and Phone cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Check if room available
                String checkSQL = "SELECT isBooked FROM rooms WHERE roomNumber=?";
                PreparedStatement psCheck = conn.prepareStatement(checkSQL);
                psCheck.setInt(1, roomNumber);
                ResultSet rs = psCheck.executeQuery();

                if (rs.next() && !rs.getBoolean("isBooked")) {
                    String updateSQL = "UPDATE rooms SET isBooked=TRUE, customerName=?, customerPhone=?, daysStayed=?, bookingDate=NOW(), type=?, foodOrdered=? WHERE roomNumber=?";
                    PreparedStatement psUpdate = conn.prepareStatement(updateSQL);
                    psUpdate.setString(1, name);
                    psUpdate.setString(2, phone);
                    psUpdate.setInt(3, days);
                    psUpdate.setString(4, roomType);
                    psUpdate.setBoolean(5, wantsFood);
                    psUpdate.setInt(6, roomNumber);
                    psUpdate.executeUpdate();

                    JOptionPane.showMessageDialog(frame,
                            "✅ Room " + roomNumber + " booked successfully!\n" +
                            "Room Type: " + roomType + "\nFood Service: " + (wantsFood ? "Yes" : "No"),
                            "Booking Successful", JOptionPane.INFORMATION_MESSAGE);

                    frame.dispose();
                    refreshRoomView();
                } else {
                    JOptionPane.showMessageDialog(frame, "Room is not available or does not exist.", "Error", JOptionPane.WARNING_MESSAGE);
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Please enter valid numbers for Room Number and Days.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                handleDatabaseError(ex);
            }
        });

        frame.add(confirmBtn, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    // Helper: Update image and description
    private void updateRoomPreview(String type, JLabel imageLabel, JLabel descLabel) {
        String imgPath = switch (type) {
            case "Deluxe" -> "images/Deluxe.jpeg";
            case "Suite" -> "images/Suite.jpeg";
            default -> "images/Standard.jpeg";
        };
        ImageIcon icon = new ImageIcon(imgPath);
        imageLabel.setIcon(resizeImage(icon, 500, 300));

        String description = switch (type) {
            case "Deluxe" -> "<html><center><b>Deluxe Room</b> - ₹4500/day<br>Includes AC, WiFi, and King Bed.</center></html>";
            case "Suite" -> "<html><center><b>Luxury Suite</b> - ₹7500/day<br>Includes Jacuzzi, Lounge Area, and Complimentary Breakfast.</center></html>";
            default -> "<html><center><b>Standard Room</b> - ₹2500/day<br>Includes Basic Amenities and Free WiFi.</center></html>";
        };
        descLabel.setText(description);
    }

    // Helper: Resize image smoothly
    private ImageIcon resizeImage(ImageIcon icon, int width, int height) {
        Image img = icon.getImage();
        Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private void showAvailableRoomsDialog() {
        String[] roomTypes = {"Standard", "Deluxe", "Suite"};
        String type = (String) JOptionPane.showInputDialog(this, "Select Room Type to View:",
                "View Available Rooms", JOptionPane.QUESTION_MESSAGE, null, roomTypes, roomTypes[0]);

        if (type == null) return; // User cancelled

        String sql = "SELECT * FROM rooms WHERE type=? AND isBooked=FALSE ORDER BY roomNumber ASC";
        StringBuilder availableRooms = new StringBuilder("<html><h3>Available " + type + " Rooms:</h3><br>");
        boolean found = false;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                availableRooms.append("<b>Room No:</b> ").append(rs.getInt("roomNumber"))
                        .append(" | <b>Price:</b> ₹").append(String.format("%,.2f", rs.getDouble("pricePerDay"))).append("<br>");
                found = true;
            }

            if (!found) {
                showWarning("No available rooms of type '" + type + "' found.");
            } else {
                availableRooms.append("</html>");
                showMessage(availableRooms.toString());
            }
        } catch (SQLException e) {
            handleDatabaseError(e);
        }
    }
    private void showCheckOutDialog() {
        String roomNumStr = JOptionPane.showInputDialog(this, "Enter Room Number to Check-Out:", "Check-Out", JOptionPane.QUESTION_MESSAGE);
        if (roomNumStr == null || roomNumStr.trim().isEmpty()) return;

        try {
            int roomNumber = Integer.parseInt(roomNumStr);
            String sql = "SELECT * FROM rooms WHERE roomNumber=? AND isBooked=TRUE";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, roomNumber);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String name = rs.getString("customerName");
                String phone = rs.getString("customerPhone");
                int days = rs.getInt("daysStayed");
                double pricePerDay = rs.getDouble("pricePerDay");
                boolean foodOrdered = rs.getBoolean("foodOrdered");

                double foodCost = foodOrdered ? 1000 * days : 0;
                double baseAmount = pricePerDay * days + foodCost;
                double gstRate = (pricePerDay > 7500) ? 0.18 : (pricePerDay >= 1000) ? 0.05 : 0;
                double gstAmount = (pricePerDay * gstRate) * days;
                double totalAmount = baseAmount + gstAmount;
                String checkOutTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                // === Ask Payment Mode ===
                String[] paymentOptions = {"Cash", "G-Pay"};
                String paymentMode = (String) JOptionPane.showInputDialog(this,
                        "Select Payment Mode:", "Payment", JOptionPane.QUESTION_MESSAGE, null,
                        paymentOptions, paymentOptions[0]);

                if (paymentMode == null) paymentMode = "Cash"; // default

                // === Generate Receipt ===
                String receipt = generateReceipt(name, phone, roomNumber, checkOutTime, days, pricePerDay, baseAmount, gstRate, gstAmount, totalAmount, paymentMode, foodOrdered);

                JTextArea receiptArea = new JTextArea(receipt, 18, 50);
                receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
                receiptArea.setEditable(false);
                JOptionPane.showMessageDialog(this, new JScrollPane(receiptArea), "Final Bill", JOptionPane.INFORMATION_MESSAGE);

                // === Show QR only if G-Pay ===
                if (paymentMode.equals("G-Pay")) {
                    ImageIcon qrIcon = resizeImage(new ImageIcon("images/gpay_qr.png"), 250, 250);
                    JOptionPane.showMessageDialog(this,"Scan QR to Pay","Payment", JOptionPane.INFORMATION_MESSAGE,qrIcon);
                }

                // === Feedback ===
                String feedback = JOptionPane.showInputDialog(this, "Thank you, " + name + "! Please provide feedback on your stay:", "Feedback", JOptionPane.QUESTION_MESSAGE);
                if (feedback == null) feedback = "No feedback provided.";

                // === Update database ===
                String updateSQL = "UPDATE rooms SET isBooked=FALSE, customerName=NULL, customerPhone=NULL, daysStayed=0, bookingDate=NULL, foodOrdered=FALSE, feedback=? WHERE roomNumber=?";
                PreparedStatement psUpdate = conn.prepareStatement(updateSQL);
                psUpdate.setString(1, feedback);
                psUpdate.setInt(2, roomNumber);
                psUpdate.executeUpdate();

                showMessage("Check-out completed for Room " + roomNumber + ". Payment Mode: " + paymentMode);
                refreshRoomView();

            } else {
                showWarning("Room is not booked or does not exist.");
            }

        } catch (NumberFormatException e) {
            showError("Invalid room number.");
        } catch (SQLException e) {
            handleDatabaseError(e);
        }
    }


    private void searchCustomer() {
        // This logic is unchanged but will use the new styled dialogs.
        String key = JOptionPane.showInputDialog(this, "Enter Customer Name or Phone to Search:");
        if (key == null || key.trim().isEmpty()) return;
        String sql = "SELECT * FROM rooms WHERE (customerName LIKE ? OR customerPhone LIKE ?) AND isBooked=TRUE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + key + "%");
            ps.setString(2, "%" + key + "%");
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                 String result = String.format("<html><h3>Customer Found:</h3><br>"
                        + "<b>Room No:</b> %d<br>"
                        + "<b>Type:</b> %s<br>"
                        + "<b>Customer:</b> %s<br>"
                        + "<b>Phone:</b> %s</html>",
                        rs.getInt("roomNumber"), rs.getString("type"),
                        rs.getString("customerName"), rs.getString("customerPhone"));
                showMessage(result);
            } else {
                showWarning("No customer found matching '" + key + "'.");
            }
        } catch (SQLException e) {
            handleDatabaseError(e);
        }
    }
    
    private void refreshRoomView() {
        tableModel.setRowCount(0);
        String sql = "SELECT * FROM rooms ORDER BY roomNumber ASC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("bookingDate");
                String dateStr = (ts != null) ? new SimpleDateFormat("yyyy-MM-dd").format(ts) : "N/A";
                Object[] row = {
                    rs.getInt("roomNumber"), rs.getString("type"),
                    String.format("₹%,.2f", rs.getDouble("pricePerDay")),
                    rs.getBoolean("isBooked") ? "Booked" : "Available",
                    rs.getString("customerName") == null ? "—" : rs.getString("customerName"),
                    rs.getString("customerPhone") == null ? "—" : rs.getString("customerPhone"),
                    dateStr
                };
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            handleDatabaseError(e);
        }
    }
    private void styleTable() {
        roomTable.setFont(TABLE_FONT);
        roomTable.setRowHeight(30);
        roomTable.setGridColor(Color.LIGHT_GRAY);
        roomTable.setSelectionBackground(PRIMARY_COLOR.brighter());
        roomTable.setSelectionForeground(Color.WHITE);

        JTableHeader header = roomTable.getTableHeader();
        header.setFont(BUTTON_FONT);
        header.setBackground(PRIMARY_COLOR);
        header.setForeground(FONT_COLOR);
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
        
        // Center align table cell content
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for(int i=0; i<tableModel.getColumnCount(); i++){
            roomTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 20, 10, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
    private String generateReceipt(String name, String phone, int room, String time, int days,
            double price, double base, double gstRate, double gst, double total,
            String paymentMode, boolean foodOrdered) {
return "************************************************\n" +
		"              HOTEL CHECK-OUT BILL              \n" +
		"************************************************\n\n" +
		String.format(" Customer Name : %-29s\n", name) +
		String.format(" Customer Phone: %-29s\n", phone) +
		String.format(" Room Number   : %-29d\n", room) +
		String.format(" Check-Out Time: %-29s\n", time) +
		"------------------------------------------------\n" +
					" BILLING DETAILS:\n" +
		"------------------------------------------------\n" +
		String.format(" Stay Duration : %d Day(s) x ₹%,.2f/day\n", days, price) +
		(foodOrdered ? String.format(" Food Service  : %d Day(s) x ₹1000/day\n", days) : "") +
		String.format("   Base Amount : %,29.2f\n", base) +
		String.format("   GST (%.0f%%)    : %,29.2f\n", gstRate * 100, gst) +
		"================================================\n" +
		String.format("   TOTAL AMOUNT  : %,29.2f\n", total) +
		String.format(" Payment Mode   : %-29s\n", paymentMode) +
		"================================================\n\n" +
		"      Thank you for staying with us!\n" +
		"************************************************\n";
}

    
    private void showMessage(String message) { JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE); }
    private void showWarning(String message) { JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE); }
    private void showError(String message) { JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE); }
    private void handleDatabaseError(SQLException e) {
        e.printStackTrace();
        showError("Database Error: " + e.getMessage() + "\nPlease check console and database connection.");
    }
    
    public static void main(String[] args) {
        try {
           
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> new demojdbc().setVisible(true));
    }
}









