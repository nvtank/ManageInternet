package View;

import Model.DataTransfer;
import Model.Food;
import Model.Service;
import Model.UsageSession;
import Model.AudioCapture;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Home extends JFrame {;
    // Professional Gaming Center Color Scheme
    private static final Color BACKGROUND_COLOR = new Color(245, 247, 250);
    private static final Color CARD_COLOR = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(30, 41, 59);
    private static final Color TEXT_SECONDARY = new Color(100, 116, 139);
    private static final Color ACCENT_COLOR = new Color(16, 185, 129);
    private static final Color PRIMARY_COLOR = new Color(99, 102, 241);
    private static final Color HOVER_COLOR = new Color(241, 245, 249);
    private static final Color SECONDARY_COLOR = new Color(59, 130, 246);

    private long startTime;
    private JLabel timerLabel;
    private DefaultTableModel cartTableModel;
    private OutputStream out;
    private ObjectOutputStream outObj;
    private JPanel jpnFoodItems;
    private JPanel jpnServiceItems;
    private JPanel jpnChatMessages;

    private String username;
    private boolean isRecording = false;
    private Timer recordingTimer;
    private int timeRecord = 0;
    private byte[] audioData;
    private boolean checkCommunity = false;

    private AudioCapture audioCapture;

    public Home(String username, int machineID, Socket socket, ObjectInputStream in, ObjectOutputStream out) {
        initializeFrame();
        createComponents();

        audioCapture = new AudioCapture();

        this.outObj = out;
        this.username = username;
        try {
            this.out = socket.getOutputStream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LocalDateTime startTime = LocalDateTime.now();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (!socket.isClosed()) {
                    LocalDateTime endTime = LocalDateTime.now();
                    UsageSession usageSession = new UsageSession(username, machineID, startTime, endTime);
                    sendData(new DataTransfer("/*outData", usageSession));
                    outObj.flush();
                    outObj.close();
                    socket.close();
                }
                System.out.println("Logout request sent and connection closed.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));

        new Thread(() -> {
            try {
                Object inObj;
                while ((inObj = in.readObject()) != null) {
                    if (inObj instanceof DataTransfer data) {
                        if (data.getType().equals("/*forceLogout")) {
                            JOptionPane.showMessageDialog(null, "You have been logged out by admin.", "Notice", JOptionPane.WARNING_MESSAGE);
                            in.close();
                            out.close();
                            socket.close();
                            System.exit(0);
                        } else if (data.getType().equals("/*loadTimePlay")) {
                            timerLabel.setText("Time Remaining: " + data.getData().toString());
                        } else if (data.getType().equals("/*dataAllFood")) {
                            List<Food> listFood = (List<Food>) data.getData();
                            for (Food food : listFood) {
                                jpnFoodItems.add(createModernItemPanel(food.getName(), food.getImage(), food.getAmount()));
                            }
                        } else if (data.getType().equals("/*cancelOrders")) {
                            String message = data.getData().toString();
                            JOptionPane.showMessageDialog(null, message, "Order Cancellation Reason", JOptionPane.INFORMATION_MESSAGE);
                        } else if (data.getType().equals("/*dataAllService")) {
                            List<Service> listService = (List<Service>) data.getData();
                            for (Service service : listService) {
                                jpnServiceItems.add(createModernItemPanel(service.getName(), service.getAmount()));
                            }
                        } else if (data.getType().equals("/*sendMessageText")) {
                            JLabel label = new JLabel(data.getData().toString());
                            JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                            Dimension lineSize = new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height);
                            line.setMaximumSize(lineSize);
                            line.setAlignmentX(Component.LEFT_ALIGNMENT);
                            line.setBackground(Color.WHITE);
                            line.add(label);
                            jpnChatMessages.add(line);
                            jpnChatMessages.add(Box.createVerticalStrut(5));
                            jpnChatMessages.revalidate();
                            jpnChatMessages.repaint();
                        } else if (data.getType().equals("/*newFood")) {
                            Food food = (Food) data.getData();
                            jpnFoodItems.add(createModernItemPanel(food.getName(), food.getImage(), food.getAmount()));
                        } else if (data.getType().equals("/*updateFood")) {
                            Food food = (Food) data.getData();
                            for (Component comp : jpnFoodItems.getComponents()) {
                                if (comp instanceof JPanel itemPanel) {
                                    String name = itemPanel.getClientProperty("name").toString();
                                    if (food.getName().equals(name)) {
                                        jpnFoodItems.remove(itemPanel);
                                        jpnFoodItems.add(createModernItemPanel(food.getName(), food.getImage(), food.getAmount()));
                                        jpnFoodItems.revalidate();
                                        jpnFoodItems.repaint();
                                    }
                                }
                            }
                        } else if (data.getType().equals("/*deleteFood")) {
                            String name = data.getData().toString();
                            for (Component comp : jpnFoodItems.getComponents()) {
                                if (comp instanceof JPanel itemPanel) {
                                    String n = itemPanel.getClientProperty("name").toString();
                                    if (name.equals(n)) {
                                        jpnFoodItems.remove(itemPanel);
                                    }
                                }
                            }
                        } else if (data.getType().equals("/*newService")) {
                            Service service = (Service) data.getData();
                            jpnServiceItems.add(createModernItemPanel(service.getName(), service.getAmount()));
                        } else if (data.getType().equals("/*sendMessageImage")) {
                            byte[] filesByte = (byte[]) data.getData();
                            JLabel label = new JLabel();
                            ImageIcon icon = new ImageIcon(filesByte);
                            Image scaledImage = icon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                            ImageIcon resizedIcon = new ImageIcon(scaledImage);
                            label.setIcon(resizedIcon);
                            JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                            Dimension lineSize = new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height);
                            line.setMaximumSize(lineSize);
                            line.setAlignmentX(Component.LEFT_ALIGNMENT);
                            line.setBackground(Color.WHITE);
                            line.add(label);

                            jpnChatMessages.add(line);
                            jpnChatMessages.add(Box.createVerticalStrut(5));
                            jpnChatMessages.revalidate();
                            jpnChatMessages.repaint();
                        } else if (data.getType().equals("/*sendMessageFile")) {
                            DataTransfer files = (DataTransfer) data.getData();
                            String fileName = files.getType();
                            byte[] dataFile = (byte[]) files.getData();
                            JLabel label = new JLabel("<html><span style='color:#007BFF; text-decoration: underline;'>" + fileName + "</span></html>");
                            JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                            Dimension lineSize = new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height);
                            line.setMaximumSize(lineSize);
                            line.setAlignmentX(Component.LEFT_ALIGNMENT);
                            line.setBackground(Color.WHITE);
                            line.add(label);

                            label.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseClicked(MouseEvent e) {
                                    JFileChooser jFileChooser = new JFileChooser();
                                    jFileChooser.setDialogTitle("Choose where to save the file.");
                                    jFileChooser.setSelectedFile(new File(fileName));
                                    int userSelectedFile = jFileChooser.showSaveDialog(null);
                                    if (userSelectedFile == JFileChooser.APPROVE_OPTION) {
                                        File fileSave = jFileChooser.getSelectedFile();
                                        try {
                                            FileOutputStream fos = new FileOutputStream(fileSave);
                                            fos.write(dataFile);
                                            fos.flush();
                                        } catch (Exception ex) {
                                            throw new RuntimeException(ex);
                                        }
                                    }
                                }
                            });

                            jpnChatMessages.add(line);
                            jpnChatMessages.add(Box.createVerticalStrut(5));
                            jpnChatMessages.revalidate();
                            jpnChatMessages.repaint();
                        } else if (data.getType().equals("/*sendMessageAudio")) {
                            DataTransfer dataTransfer = (DataTransfer) data.getData();
                            JLabel label = new JLabel(dataTransfer.getType().toString());
                            JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                            Dimension lineSize = new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height);
                            line.setMaximumSize(lineSize);
                            line.setAlignmentX(Component.LEFT_ALIGNMENT);
                            line.setBackground(Color.WHITE);
                            line.add(label);
                            jpnChatMessages.add(line);
                            jpnChatMessages.add(Box.createVerticalStrut(5));
                            jpnChatMessages.revalidate();
                            jpnChatMessages.repaint();

                            byte[] audioData = (byte[]) dataTransfer.getData();
                            label.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseClicked(MouseEvent e) {
                                    audioCapture.playAudioFromBytes(audioData);
                                }
                            });
                        } else if (data.getType().equals("/*sendGroupMessageText")) {
                            if (checkCommunity) {
                                JLabel label = new JLabel(data.getData().toString());
                                JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                                Dimension lineSize = new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height);
                                line.setMaximumSize(lineSize);
                                line.setAlignmentX(Component.LEFT_ALIGNMENT);
                                line.setBackground(Color.WHITE);
                                line.add(label);
                                jpnChatMessages.add(line);
                                jpnChatMessages.add(Box.createVerticalStrut(5));
                                jpnChatMessages.revalidate();
                                jpnChatMessages.repaint();
                            }
                        } else if (data.getType().equals("/*sendGroupMessageAudio")) {
                            if (checkCommunity) {
                                DataTransfer dataTransfer = (DataTransfer) data.getData();
                                JLabel label = new JLabel(dataTransfer.getType().toString());
                                JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                                Dimension lineSize = new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height);
                                line.setMaximumSize(lineSize);
                                line.setAlignmentX(Component.LEFT_ALIGNMENT);
                                line.setBackground(Color.WHITE);
                                line.add(label);
                                jpnChatMessages.add(line);
                                jpnChatMessages.add(Box.createVerticalStrut(5));
                                jpnChatMessages.revalidate();
                                jpnChatMessages.repaint();

                                byte[] audioData = (byte[]) dataTransfer.getData();
                                label.addMouseListener(new MouseAdapter() {
                                    @Override
                                    public void mouseClicked(MouseEvent e) {
                                        audioCapture.playAudioFromBytes(audioData);
                                    }
                                });
                            }
                        } else if (data.getType().equals("/*sendGroupMessageImage")) {
                            if (checkCommunity) {
                                DataTransfer dataTransfer = (DataTransfer) data.getData();
                                JLabel label = new JLabel(dataTransfer.getType());
                                JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                                Dimension lineSize = new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height);
                                line.setMaximumSize(lineSize);
                                line.setAlignmentX(Component.LEFT_ALIGNMENT);
                                line.setBackground(Color.WHITE);
                                line.add(label);

                                byte[] dataByte = (byte[]) dataTransfer.getData();
                                JLabel label1 = new JLabel();
                                ImageIcon icon1 = new ImageIcon(dataByte);
                                Image scaledImage1 = icon1.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                                ImageIcon resizedIcon1 = new ImageIcon(scaledImage1);
                                label1.setIcon(resizedIcon1);
                                JPanel line1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                                Dimension lineSize1 = new Dimension(Integer.MAX_VALUE, label1.getPreferredSize().height);
                                line1.setMaximumSize(lineSize1);
                                line1.setAlignmentX(Component.LEFT_ALIGNMENT);
                                line1.setBackground(Color.WHITE);
                                line1.add(label1);

                                jpnChatMessages.add(line);
                                jpnChatMessages.add(Box.createVerticalStrut(5));
                                jpnChatMessages.add(line1);
                                jpnChatMessages.add(Box.createVerticalStrut(5));
                                jpnChatMessages.revalidate();
                                jpnChatMessages.repaint();
                            }
                        } else if (data.getType().equals("/*sendGroupMessageFile")) {
                            if (checkCommunity) {
                                DataTransfer files = (DataTransfer) data.getData();
                                String fileName = files.getType();
                                byte[] dataFile = (byte[]) files.getData();
                                JLabel label = new JLabel("<html><span style='color:#007BFF; text-decoration: underline;'>" + fileName + "</span></html>");
                                JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                                Dimension lineSize = new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height);
                                line.setMaximumSize(lineSize);
                                line.setAlignmentX(Component.LEFT_ALIGNMENT);
                                line.setBackground(Color.WHITE);
                                line.add(label);

                                label.addMouseListener(new MouseAdapter() {
                                    @Override
                                    public void mouseClicked(MouseEvent e) {
                                        JFileChooser jFileChooser = new JFileChooser();
                                        jFileChooser.setDialogTitle("Choose where to save the file.");
                                        jFileChooser.setSelectedFile(new File(fileName));
                                        int userSelectedFile = jFileChooser.showSaveDialog(null);
                                        if (userSelectedFile == JFileChooser.APPROVE_OPTION) {
                                            File fileSave = jFileChooser.getSelectedFile();
                                            try {
                                                FileOutputStream fos = new FileOutputStream(fileSave);
                                                fos.write(dataFile);
                                                fos.flush();
                                            } catch (Exception ex) {
                                                throw new RuntimeException(ex);
                                            }
                                        }
                                    }
                                });

                                jpnChatMessages.add(line);
                                jpnChatMessages.add(Box.createVerticalStrut(5));
                                jpnChatMessages.revalidate();
                                jpnChatMessages.repaint();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void sendData(Object data) throws Exception {
        this.outObj.writeObject(data);
        this.outObj.flush();
    }

    private void initializeFrame() {
        setTitle("🎮 Gaming Center Pro - User Portal");
        setSize(1300, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND_COLOR);
        setLayout(new BorderLayout(15, 15));

        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));
    }

    private void createComponents() {
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel jpnHeader = new JPanel(new BorderLayout(20, 0));
        jpnHeader.setBackground(CARD_COLOR);
        jpnHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                new EmptyBorder(15, 20, 15, 20)
        ));

        JPanel jpnSearchSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        jpnSearchSection.setBackground(CARD_COLOR);

        JLabel lblSearch = new JLabel("🔍");
        lblSearch.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        lblSearch.setBorder(new EmptyBorder(0, 0, 0, 10));

        JTextField txtSearch = new JTextField(25);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSearch.setForeground(TEXT_PRIMARY);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        txtSearch.setBackground(BACKGROUND_COLOR);

        txtSearch.setText("Search for food, services...");
        txtSearch.setForeground(TEXT_SECONDARY);
        txtSearch.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (txtSearch.getText().equals("Search for food, services...")) {
                    txtSearch.setText("");
                    txtSearch.setForeground(TEXT_PRIMARY);
                }
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                if (txtSearch.getText().isEmpty()) {
                    txtSearch.setText("Search for food, services...");
                    txtSearch.setForeground(TEXT_SECONDARY);
                }
            }
        });

        jpnSearchSection.add(lblSearch);
        jpnSearchSection.add(txtSearch);

        timerLabel = new JLabel();
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        timerLabel.setForeground(ACCENT_COLOR);
        timerLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        jpnHeader.add(jpnSearchSection, BorderLayout.WEST);
        jpnHeader.add(timerLabel, BorderLayout.EAST);

        return jpnHeader;
    }

    private JPanel createMainPanel() {
        JPanel jpnMain = new JPanel(new BorderLayout(15, 0));
        jpnMain.setBackground(BACKGROUND_COLOR);

        // Create tabbed pane with modern styling
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(CARD_COLOR);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setForeground(TEXT_PRIMARY);

        // Custom tab styling
        tabbedPane.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                              int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (isSelected) {
                    g2d.setColor(PRIMARY_COLOR);
                } else {
                    g2d.setColor(BACKGROUND_COLOR);
                }
                g2d.fillRoundRect(x, y, w, h, 8, 8);
            }

            @Override
            protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
                                     int tabIndex, String title, Rectangle textRect, boolean isSelected) {
                g.setColor(isSelected ? Color.WHITE : TEXT_PRIMARY);
                super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
            }
        });

        // Food tab
        tabbedPane.addTab(" 🍕 Food", createFoodTab());
        tabbedPane.addTab(" 🎮 Services", createServiceTab());
        tabbedPane.addTab(" 💬 Chat", createChatTab());

        JPanel jpnCart = createCartPanel();

        jpnMain.add(tabbedPane, BorderLayout.CENTER);
        jpnMain.add(jpnCart, BorderLayout.EAST);

        return jpnMain;
    }

    private JPanel createFoodTab() {
        JPanel jpnFoodTab = new JPanel(new BorderLayout());
        jpnFoodTab.setBackground(BACKGROUND_COLOR);
        jpnFoodTab.setBorder(new EmptyBorder(15, 15, 15, 15));

        jpnFoodItems = new JPanel(new GridLayout(0, 3, 15, 15));
        jpnFoodItems.setBackground(BACKGROUND_COLOR);

        JScrollPane scrollPane = new JScrollPane(jpnFoodItems);
        scrollPane.setBorder(null);
        scrollPane.setBackground(BACKGROUND_COLOR);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        jpnFoodTab.add(scrollPane, BorderLayout.CENTER);
        return jpnFoodTab;
    }

    private JPanel createModernItemPanel(String itemName, byte[] dataImage, int price) {
        JPanel jpnItem = new JPanel();
        jpnItem.setLayout(new BorderLayout());
        jpnItem.setBackground(CARD_COLOR);
        jpnItem.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));
        jpnItem.setPreferredSize(new Dimension(220, 280));

        jpnItem.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                jpnItem.setBackground(HOVER_COLOR);
                jpnItem.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                jpnItem.setBackground(CARD_COLOR);
                jpnItem.repaint();
            }
        });

        // Image placeholder with gradient
        JPanel jpnImage = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gradient = new GradientPaint(0, 0, SECONDARY_COLOR,
                        getWidth(), getHeight(), PRIMARY_COLOR);
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                if (dataImage != null) {
                    try {
                        // Convert byte[] to image each time we draw
                        ByteArrayInputStream bais = new ByteArrayInputStream(dataImage);
                        BufferedImage image = ImageIO.read(bais);

                        if (image != null) {
                            int drawWidth = Math.min(image.getWidth(), getWidth() - 20);
                            int drawHeight = Math.min(image.getHeight(), getHeight() - 20);
                            int x = (getWidth() - drawWidth) / 2;
                            int y = (getHeight() - drawHeight) / 2;

                            g2d.drawImage(image, x, y, drawWidth, drawHeight, this);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        jpnImage.setPreferredSize(new Dimension(180, 140));
        jpnItem.add(jpnImage, BorderLayout.CENTER);

        // Info panel
        JPanel jpnInfo = new JPanel(new BorderLayout(0, 8));
        jpnInfo.setBackground(CARD_COLOR);
        jpnInfo.setBorder(new EmptyBorder(15, 0, 0, 0));

        // Item name
        JLabel lblName = new JLabel(itemName);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblName.setForeground(TEXT_PRIMARY);
        lblName.setHorizontalAlignment(SwingConstants.CENTER);

        // Price
        JLabel lblPrice = new JLabel(String.format("%,d VND", price));
        lblPrice.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPrice.setForeground(ACCENT_COLOR);
        lblPrice.setHorizontalAlignment(SwingConstants.CENTER);

        // Add to cart button
        JButton btnAddToCart = new JButton("🛒 Add to Cart") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2d.setColor(new Color(39, 174, 96));
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(52, 211, 123));
                } else {
                    g2d.setColor(ACCENT_COLOR);
                }

                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
            }
        };

        btnAddToCart.setForeground(Color.WHITE);
        btnAddToCart.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddToCart.setContentAreaFilled(false);
        btnAddToCart.setFocusPainted(false);
        btnAddToCart.setBorder(new EmptyBorder(10, 20, 10, 20));
        btnAddToCart.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add click functionality
        btnAddToCart.addActionListener(e -> {
            addToCart(itemName, price);
        });

        jpnInfo.add(lblName, BorderLayout.NORTH);
        jpnInfo.add(lblPrice, BorderLayout.CENTER);
        jpnInfo.add(btnAddToCart, BorderLayout.SOUTH);

        jpnItem.add(jpnInfo, BorderLayout.SOUTH);

        jpnItem.putClientProperty("name", itemName);

        return jpnItem;
    }

    private void addToCart(String itemName, int price) {
        // Check if item already exists in cart
        for (int i = 0; i < cartTableModel.getRowCount(); i++) {
            if (cartTableModel.getValueAt(i, 0).equals(itemName)) {
                int currentQty = (Integer) cartTableModel.getValueAt(i, 1);
                cartTableModel.setValueAt(currentQty + 1, i, 1);
                cartTableModel.setValueAt((currentQty + 1) * price, i, 3);
                updateTotal();
                return;
            }
        }

        // Add new item with formatted price
        cartTableModel.addRow(new Object[]{itemName, 1, String.format("%,d", price), price});
        updateTotal();
    }

    private void updateTotal() {
        int total = 0;
        for (int i = 0; i < cartTableModel.getRowCount(); i++) {
            total += (Integer) cartTableModel.getValueAt(i, 3);
        }

        // Update total label in cart panel
        Component[] components = ((JPanel) ((BorderLayout) ((JPanel) getContentPane()
                .getComponent(1)).getLayout()).getLayoutComponent(BorderLayout.EAST)).getComponents();

        if (components.length > 2) {
            JPanel jpnTotalPanel = (JPanel) components[2];
            JPanel jpnTotalPayment = (JPanel) jpnTotalPanel.getComponent(0);
            JLabel lblTotalLabel = (JLabel) jpnTotalPayment.getComponent(0);
            lblTotalLabel.setText(String.format("Total: %,d VND", total)); // Fixed: Use lblTotalLabel
        }
    }

    private JPanel createCartPanel() {
        JPanel jpnCart = new JPanel(new BorderLayout());
        jpnCart.setBackground(CARD_COLOR);
        jpnCart.setPreferredSize(new Dimension(350, 0));
        jpnCart.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        // Cart header
        JLabel lblCart = new JLabel(" 🛒 Your Cart");
        lblCart.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblCart.setForeground(TEXT_PRIMARY);
        lblCart.setBorder(new EmptyBorder(0, 0, 15, 0));

        // Cart table
        String[] cartColumns = {"Product", "Qty", "Price", "Total"};
        cartTableModel = new DefaultTableModel(cartColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // Only quantity column is editable
            }
        };

        JTable cartTable = new JTable(cartTableModel);
        cartTable.setRowHeight(35);
        cartTable.setBackground(CARD_COLOR);
        cartTable.setForeground(TEXT_PRIMARY);
        cartTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cartTable.setGridColor(new Color(240, 240, 240));
        cartTable.setSelectionBackground(HOVER_COLOR);
        cartTable.setSelectionForeground(TEXT_PRIMARY);

        // Style table header
        JTableHeader header = cartTable.getTableHeader();
        header.setBackground(BACKGROUND_COLOR);
        header.setForeground(TEXT_PRIMARY);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Center align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        cartTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        cartTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        cartTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        JScrollPane cartScrollPane = new JScrollPane(cartTable);
        cartScrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1));
        cartScrollPane.setBackground(CARD_COLOR);

        // Total panel
        JPanel jpnTotal = new JPanel(new BorderLayout());
        jpnTotal.setBackground(CARD_COLOR);
        jpnTotal.setBorder(new EmptyBorder(15, 0, 0, 0));

        JLabel lblTotal = new JLabel("Total: 0 VND");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTotal.setForeground(PRIMARY_COLOR);
        lblTotal.setHorizontalAlignment(SwingConstants.LEFT);

        // Payment button
        JButton btnPayment = new JButton(" 💳 Checkout") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2d.setColor(new Color(31, 97, 141));
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(46, 134, 193));
                } else {
                    g2d.setColor(PRIMARY_COLOR);
                }

                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
            }
        };

        btnPayment.setForeground(Color.WHITE);
        btnPayment.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnPayment.setContentAreaFilled(false);
        btnPayment.setFocusPainted(false);
        btnPayment.setBorder(new EmptyBorder(12, 20, 12, 20));
        btnPayment.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPayment.setPreferredSize(new Dimension(140, 45));

        // Payment button functionality
        btnPayment.addActionListener(e -> {
            if (cartTableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this,
                        "Cart is empty! Please add products before checkout.",
                        "Notice", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int total = 0;
            Map<Integer, List<String>> foodOrders = new HashMap<>();
            for (int i = 0; i < cartTableModel.getRowCount(); i++) {
                total += (Integer) cartTableModel.getValueAt(i, 3);
                List<String> food = new ArrayList<>();
                food.add(cartTableModel.getValueAt(i, 0).toString());
                food.add(cartTableModel.getValueAt(i, 1).toString());
                food.add(cartTableModel.getValueAt(i, 2).toString());
                foodOrders.put(i, food);
            }

            int result = JOptionPane.showConfirmDialog(this,
                    String.format("Confirm checkout total: %,d VND", total),
                    "Confirm Payment", JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                try {
                    cartTableModel.setRowCount(0);
                    updateTotal();
                    JOptionPane.showMessageDialog(this,
                            "Payment successful! Thank you for using our service.",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    sendData(new DataTransfer("/*oderFoods", foodOrders));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        // Panel for total and payment button
        JPanel jpnTotalPayment = new JPanel(new BorderLayout(10, 0));
        jpnTotalPayment.setBackground(CARD_COLOR);
        jpnTotalPayment.add(lblTotal, BorderLayout.CENTER);
        jpnTotalPayment.add(btnPayment, BorderLayout.EAST);

        jpnTotal.add(jpnTotalPayment, BorderLayout.NORTH);

        jpnCart.add(lblCart, BorderLayout.NORTH); // Fixed: Use lblCart instead of lblCartHeader
        jpnCart.add(cartScrollPane, BorderLayout.CENTER);
        jpnCart.add(jpnTotal, BorderLayout.SOUTH);

        return jpnCart;
    }

    private JPanel createServiceTab() {
        JPanel jpnServiceTab = new JPanel(new BorderLayout());
        jpnServiceTab.setBackground(BACKGROUND_COLOR);
        jpnServiceTab.setBorder(new EmptyBorder(15, 15, 15, 15));

        jpnServiceItems = new JPanel(new GridLayout(0, 3, 15, 15));
        jpnServiceItems.setBackground(BACKGROUND_COLOR);

        JScrollPane scrollPane = new JScrollPane(jpnServiceItems);
        scrollPane.setBorder(null);
        scrollPane.setBackground(BACKGROUND_COLOR);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        jpnServiceTab.add(scrollPane, BorderLayout.CENTER);
        return jpnServiceTab;
    }

    private JPanel createModernItemPanel(String itemName, int price) {
        JPanel jpnItem = new JPanel();
        jpnItem.setLayout(new BorderLayout());
        jpnItem.setBackground(CARD_COLOR);
        jpnItem.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));
        jpnItem.setPreferredSize(new Dimension(220, 280));

        // Add hover effect
        jpnItem.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                jpnItem.setBackground(HOVER_COLOR);
                jpnItem.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                jpnItem.setBackground(CARD_COLOR);
                jpnItem.repaint();
            }
        });

        // Image placeholder with gradient
        JPanel jpnImage = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gradient = new GradientPaint(0, 0, SECONDARY_COLOR,
                        getWidth(), getHeight(), PRIMARY_COLOR);
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                // Add a service icon
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
                FontMetrics fm = g2d.getFontMetrics();
                String emoji = "�";
                int x = (getWidth() - fm.stringWidth(emoji)) / 2;
                int y = (getHeight() + fm.getAscent()) / 2;
                g2d.drawString(emoji, x, y);
            }
        };
        jpnImage.setPreferredSize(new Dimension(180, 140));
        jpnItem.add(jpnImage, BorderLayout.CENTER);

        // Info panel
        JPanel jpnInfo = new JPanel(new BorderLayout(0, 8));
        jpnInfo.setBackground(CARD_COLOR);
        jpnInfo.setBorder(new EmptyBorder(15, 0, 0, 0));

        // Item name
        JLabel lblName = new JLabel(itemName);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblName.setForeground(TEXT_PRIMARY);
        lblName.setHorizontalAlignment(SwingConstants.CENTER);

        // Price
        JLabel lblPrice = new JLabel(String.format("%,d VND", price));
        lblPrice.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPrice.setForeground(ACCENT_COLOR);
        lblPrice.setHorizontalAlignment(SwingConstants.CENTER);

        // Add to cart button
        JButton btnAddToCart = new JButton("🛒 Add to Cart") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2d.setColor(new Color(39, 174, 96));
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(52, 211, 123));
                } else {
                    g2d.setColor(ACCENT_COLOR);
                }

                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
            }
        };

        btnAddToCart.setForeground(Color.WHITE);
        btnAddToCart.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddToCart.setContentAreaFilled(false);
        btnAddToCart.setFocusPainted(false);
        btnAddToCart.setBorder(new EmptyBorder(10, 20, 10, 20));
        btnAddToCart.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add click functionality
        btnAddToCart.addActionListener(e -> {
            addToCart(itemName, price);
        });

        jpnInfo.add(lblName, BorderLayout.NORTH);
        jpnInfo.add(lblPrice, BorderLayout.CENTER);
        jpnInfo.add(btnAddToCart, BorderLayout.SOUTH);

        jpnItem.add(jpnInfo, BorderLayout.SOUTH);
        return jpnItem;
    }

    private JPanel createChatTab() {
        JPanel jpnChatTab = new JPanel(new BorderLayout());
        jpnChatTab.setBackground(BACKGROUND_COLOR);
        jpnChatTab.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Chat header
        JPanel jpnChatHeader = new JPanel(new BorderLayout());
        jpnChatHeader.setBackground(CARD_COLOR);
        jpnChatHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                new EmptyBorder(15, 20, 15, 20)
        ));

        JLabel lblAdmin = new JLabel(" 👤 Admin - Gaming Center Manager");
        lblAdmin.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblAdmin.setForeground(TEXT_PRIMARY);

        JLabel lblStatus = new JLabel(" ● Online");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(ACCENT_COLOR);

        JPanel jpnHeaderInfo = new JPanel(new BorderLayout());
        jpnHeaderInfo.setBackground(CARD_COLOR);
        jpnHeaderInfo.add(lblAdmin, BorderLayout.NORTH);
        jpnHeaderInfo.add(lblStatus, BorderLayout.SOUTH);

        jpnChatHeader.add(jpnHeaderInfo, BorderLayout.WEST);

        // Chat messages area
        jpnChatMessages = new JPanel();
        jpnChatMessages.setLayout(new BoxLayout(jpnChatMessages, BoxLayout.Y_AXIS));
        jpnChatMessages.setBackground(Color.WHITE);
        jpnChatMessages.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane chatScrollPane = new JScrollPane(jpnChatMessages);
        chatScrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1));
        chatScrollPane.setBackground(BACKGROUND_COLOR);
        chatScrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Chat input area
        JPanel jpnChatInput = new JPanel(new BorderLayout(10, 0));
        jpnChatInput.setBackground(CARD_COLOR);
        jpnChatInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                new EmptyBorder(15, 20, 15, 20)
        ));

        JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftButtonPanel.setBackground(CARD_COLOR);

        // Community button
        JButton btnCommunity = new JButton(" 🌐 Community") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2d.setColor(new Color(76, 175, 80).darker());
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(76, 175, 80).brighter());
                } else {
                    g2d.setColor(new Color(76, 175, 80));
                }

                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
            }
        };

        btnCommunity.setForeground(Color.WHITE);
        btnCommunity.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCommunity.setContentAreaFilled(false);
        btnCommunity.setFocusPainted(false);
        btnCommunity.setBorder(new EmptyBorder(10, 15, 10, 15));
        btnCommunity.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnCommunity.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                checkCommunity = true;
                lblAdmin.setText(" 🌐 TaVu Community Chat");
                jpnChatMessages.removeAll();
                jpnChatMessages.revalidate();
                jpnChatMessages.repaint();
            }
        });

        // Support button
        JButton btnSupport = new JButton(" 🎧 Support") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2d.setColor(new Color(255, 152, 0).darker());
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(255, 152, 0).brighter());
                } else {
                    g2d.setColor(new Color(255, 152, 0));
                }

                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
            }
        };

        btnSupport.setForeground(Color.WHITE);
        btnSupport.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSupport.setContentAreaFilled(false);
        btnSupport.setFocusPainted(false);
        btnSupport.setBorder(new EmptyBorder(10, 15, 10, 15));
        btnSupport.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnSupport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                checkCommunity = false;
                lblAdmin.setText(" 👤 Admin - Gaming Center Manager");
                jpnChatMessages.removeAll();
                jpnChatMessages.revalidate();
                jpnChatMessages.repaint();
            }
        });

        leftButtonPanel.add(btnCommunity);
        leftButtonPanel.add(btnSupport);

        JTextField txtMessage = new JTextField();
        txtMessage.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtMessage.setForeground(TEXT_PRIMARY);
        txtMessage.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                new EmptyBorder(12, 15, 12, 15)
        ));
        txtMessage.setBackground(BACKGROUND_COLOR);

        // Placeholder for message field
        txtMessage.setText("Type your message...");
        txtMessage.setForeground(TEXT_SECONDARY);
        txtMessage.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (txtMessage.getText().equals("Type your message...")) {
                    txtMessage.setText("");
                    txtMessage.setForeground(TEXT_PRIMARY);
                }
            }

            public void focusLost(java.awt.event.FocusEvent evt) {
                if (txtMessage.getText().isEmpty()) {
                    txtMessage.setText("Type your message...");
                    txtMessage.setForeground(TEXT_SECONDARY);
                }
            }
        });

        JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightButtonPanel.setBackground(CARD_COLOR);

        // Microphone button
        JButton btnMicrophone = new JButton("🎤M") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2d.setColor(new Color(244, 67, 54).darker());
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(244, 67, 54).brighter());
                } else {
                    g2d.setColor(new Color(244, 67, 54));
                }

                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
            }
        };

        btnMicrophone.setForeground(Color.WHITE);
        btnMicrophone.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btnMicrophone.setContentAreaFilled(false);
        btnMicrophone.setFocusPainted(false);
        btnMicrophone.setBorder(new EmptyBorder(12, 15, 12, 15));
        btnMicrophone.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnMicrophone.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    if (!isRecording) {
                        txtMessage.setText("Recording...");
                        audioCapture.startRecording();
                        isRecording = true;

                        timeRecord = 0;
                        recordingTimer = new Timer(1000, new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                timeRecord++;
                                txtMessage.setText("Recording (" + formatTime(timeRecord) + ")...");
                            }
                        });
                        recordingTimer.start();
                    } else {
                        if (recordingTimer != null) {
                            recordingTimer.stop();
                            recordingTimer = null;
                        }
                        audioData = audioCapture.stopRecording();
                        txtMessage.setText("Recording complete (" + formatTime(timeRecord) + ")");
                        isRecording = false;
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        JButton btnEmoji = new JButton("😊") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2d.setColor(new Color(31, 97, 141));
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(46, 134, 193));
                } else {
                    g2d.setColor(PRIMARY_COLOR);
                }

                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
            }
        };

        btnEmoji.setForeground(Color.WHITE);
        btnEmoji.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btnEmoji.setContentAreaFilled(false);
        btnEmoji.setFocusPainted(false);
        btnEmoji.setBorder(new EmptyBorder(12, 15, 12, 15));
        btnEmoji.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Emoji popup menu with 10-column grid
        JPopupMenu emojiMenu = new JPopupMenu();
        emojiMenu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        String[] emojis = {
                "😀", "😂", "😍", "😭", "😡", "👍", "🎉", "😎", "❤️", "🔥",
                "😊", "😋", "😜", "🤓", "😢", "😱", "😴", "🤗", "🙌", "👏",
                "😇", "😈", "👻", "🎃", "🎄", "🎁", "🎂", "🍎", "🍕", "🍔"
        };

        JPanel emojiPanel = new JPanel(new GridLayout(0, 10, 5, 5));
        emojiPanel.setBackground(Color.WHITE);

        for (String emoji : emojis) {
            JButton emojiButton = new JButton(emoji);
            emojiButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
            emojiButton.setBackground(Color.WHITE);
            emojiButton.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            emojiButton.setFocusPainted(false);
            emojiButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Hover effect
            emojiButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    emojiButton.setBackground(new Color(230, 230, 230));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    emojiButton.setBackground(Color.WHITE);
                }
            });

            emojiButton.addActionListener(e -> {
                String currentText = txtMessage.getText();
                if (currentText.equals("Type your message...")) {
                    txtMessage.setText(emoji);
                    txtMessage.setForeground(TEXT_PRIMARY);
                } else {
                    txtMessage.setText(currentText + emoji);
                }
                txtMessage.requestFocus();
                emojiMenu.setVisible(false);
            });

            emojiPanel.add(emojiButton);
        }

        emojiMenu.add(emojiPanel);

        btnEmoji.addActionListener(e -> {
            emojiMenu.show(btnEmoji, 0, -emojiMenu.getPreferredSize().height);
        });

        // File button
        JButton btnFile = new JButton("📎F") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2d.setColor(new Color(31, 97, 141));
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(46, 134, 193));
                } else {
                    g2d.setColor(PRIMARY_COLOR);
                }

                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
            }
        };

        btnFile.setForeground(Color.WHITE);
        btnFile.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        btnFile.setContentAreaFilled(false);
        btnFile.setFocusPainted(false);
        btnFile.setBorder(new EmptyBorder(12, 15, 12, 15));
        btnFile.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // File chooser functionality
        btnFile.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                LookAndFeel originalLookAndFeel = UIManager.getLookAndFeel();
                try {
                    UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                    JFileChooser fileChooser = new JFileChooser(System.getProperty("user.home"));
                    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fileChooser.setFileFilter(new FileNameExtensionFilter("Files", "txt", "pdf", "doc", "docx", "jpg", "jpeg", "png"));
                    if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = fileChooser.getSelectedFile();
                        if (selectedFile.length() > 10 * 1024 * 1024) {
                            JOptionPane.showMessageDialog(null, "File too large (limit 10MB)", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        FileInputStream fis = new FileInputStream(selectedFile);
                        byte[] filesByte = fis.readAllBytes();
                        fis.close();
                        if (isImage(filesByte)) {
                            JLabel label = new JLabel();
                            ImageIcon icon = new ImageIcon(filesByte);
                            Image scaledImage = icon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                            ImageIcon resizedIcon = new ImageIcon(scaledImage);
                            label.setIcon(resizedIcon);
                            JPanel line = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                            Dimension lineSize = new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height);
                            line.setMaximumSize(lineSize);
                            line.setAlignmentX(Component.LEFT_ALIGNMENT);
                            line.setBackground(Color.WHITE);
                            line.add(label);

                            jpnChatMessages.add(line);
                            jpnChatMessages.add(Box.createVerticalStrut(5));
                            jpnChatMessages.revalidate();
                            jpnChatMessages.repaint();

                            if (checkCommunity) {
                                sendData(new DataTransfer("/*sendGroupMessageImage", filesByte));
                            } else {
                                sendData(new DataTransfer("/*sendMessageImage", filesByte));
                            }
                        } else {
                            JLabel label = new JLabel("<html><span style='color:#007BFF; text-decoration: underline;'>" + selectedFile.getName() + "</span></html>");
                            JPanel line = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                            Dimension lineSize = new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height);
                            line.setMaximumSize(lineSize);
                            line.setAlignmentX(Component.LEFT_ALIGNMENT);
                            line.setBackground(Color.WHITE);
                            line.add(label);

                            jpnChatMessages.add(line);
                            jpnChatMessages.add(Box.createVerticalStrut(5));
                            jpnChatMessages.revalidate();
                            jpnChatMessages.repaint();

                            if (checkCommunity) {
                                sendData(new DataTransfer("/*sendGroupMessageFile", new DataTransfer(username + ": " +selectedFile.getName(), filesByte)));
                            } else {
                                sendData(new DataTransfer("/*sendMessageFile", new DataTransfer(selectedFile.getName(), filesByte)));
                            }
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null,
                            "Cannot open file picker: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    try {
                        UIManager.setLookAndFeel(originalLookAndFeel);
                    } catch (Exception ignored) {
                        // Ignore restoration errors
                    }
                }
            });
        });

        JButton btnSend = new JButton(" ✈️ Send") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2d.setColor(new Color(31, 97, 141));
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(46, 134, 193));
                } else {
                    g2d.setColor(PRIMARY_COLOR);
                }

                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
            }
        };

        btnSend.setForeground(Color.WHITE);
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSend.setContentAreaFilled(false);
        btnSend.setFocusPainted(false);
        btnSend.setBorder(new EmptyBorder(12, 25, 12, 25));
        btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Send button functionality
        btnSend.addActionListener(e -> {
            String message = txtMessage.getText().trim();
            if (!message.isEmpty()) {
                JLabel label = new JLabel(message);
                JPanel line = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                Dimension lineSize = new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height);
                line.setMaximumSize(lineSize);
                line.setAlignmentX(Component.LEFT_ALIGNMENT);
                line.setBackground(Color.WHITE);
                line.add(label);
                txtMessage.setText("");
                jpnChatMessages.add(line);
                jpnChatMessages.add(Box.createVerticalStrut(5));
                jpnChatMessages.revalidate();
                jpnChatMessages.repaint();

                try {
                    if (checkCommunity) {
                        if (audioData != null) {
                            label.setText("\uD83C\uDFA4 Voice message (" + formatTime(timeRecord) + ")");
                            byte[] audioCopy = audioData;
                            label.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseClicked(MouseEvent e) {
                                    audioCapture.playAudioFromBytes(audioCopy);
                                }
                            });

                            sendData(new DataTransfer("/*sendGroupMessageAudio", new DataTransfer(username + ": " +"\uD83C\uDFA4 Voice message (" + formatTime(timeRecord) + ")", audioCopy)));
                            audioData = null;
                        } else {
                            sendData(new DataTransfer("/*sendGroupMessageText", message));
                        }
                    } else {
                        if (audioData != null) {
                            label.setText("\uD83C\uDFA4 Voice message (" + formatTime(timeRecord) + ")");
                            byte[] audioCopy = audioData;
                            label.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseClicked(MouseEvent e) {
                                    audioCapture.playAudioFromBytes(audioCopy);
                                }
                            });

                            sendData(new DataTransfer("/*sendMessageAudio", new DataTransfer("\uD83C\uDFA4 Voice message (" + formatTime(timeRecord) + ")", audioCopy)));
                            audioData = null;
                        } else {
                            sendData(new DataTransfer("/*sendMessageText", message));
                        }
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        // Enter key to send message
        txtMessage.addActionListener(e -> btnSend.doClick());

        rightButtonPanel.add(btnFile);
        rightButtonPanel.add(btnMicrophone);
        rightButtonPanel.add(btnEmoji);
        rightButtonPanel.add(btnSend);

        jpnChatInput.add(leftButtonPanel, BorderLayout.WEST);
        jpnChatInput.add(txtMessage, BorderLayout.CENTER);
        jpnChatInput.add(rightButtonPanel, BorderLayout.EAST);

        jpnChatTab.add(jpnChatHeader, BorderLayout.NORTH);
        jpnChatTab.add(chatScrollPane, BorderLayout.CENTER);
        jpnChatTab.add(jpnChatInput, BorderLayout.SOUTH);

        return jpnChatTab;
    }

    private boolean isImage(byte[] fileByte) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileByte));
            return image != null;
        } catch (IOException e) {
            return false;
        }
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> {
//            Home gui = new Home(null, 0, null, null, null);
//            gui.setVisible(true);
//        });
//    }
}
