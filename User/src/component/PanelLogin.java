package component;

import Model.DataTransfer;
import View.Home;
import net.miginfocom.swing.MigLayout;
import swing.MyPasswordField;
import swing.MyTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class PanelLogin extends JLayeredPane {
    // Professional Gaming Center Color Scheme
    private static final Color PRIMARY_DARK = new Color(18, 18, 18);
    private static final Color ACCENT_GREEN = new Color(0, 255, 136);
    private static final Color ACCENT_BLUE = new Color(0, 150, 255);
    private static final Color TEXT_WHITE = new Color(255, 255, 255);
    private static final Color TEXT_GRAY = new Color(180, 180, 180);
    
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private JPanel login;

    public PanelLogin() {
        initComponents();
        initLogin();
    }

    private void initLogin() {
        login.setLayout(new MigLayout("wrap", "push[center]push", "push[]12[]8[]8[]8[]12[]push"));
        
        // Gaming Center Logo/Title
        JLabel lblLogo = new JLabel("🎮");
        lblLogo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        login.add(lblLogo);
        
        JLabel lblTitle = new JLabel("GAMING CENTER");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(ACCENT_GREEN);
        login.add(lblTitle);
        
        JLabel lblSubtitle = new JLabel("Welcome, Gamer! Please sign in to continue");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSubtitle.setForeground(TEXT_GRAY);
        login.add(lblSubtitle);
        
        // Username field
        MyTextField txtUser = new MyTextField();
        txtUser.setPrefixIcon(new ImageIcon(getClass().getResource("/icon/user.png")));
        txtUser.setHint("Username");
        txtUser.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        login.add(txtUser, "w 65%");
        
        // Password field
        MyPasswordField txtPass = new MyPasswordField();
        txtPass.setPrefixIcon(new ImageIcon(getClass().getResource("/icon/pass.png")));
        txtPass.setHint("Password");
        txtPass.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        login.add(txtPass, "w 65%");

        // Machine selection field
        MyTextField txtMachine = new MyTextField();
        txtMachine.setPrefixIcon(new ImageIcon(getClass().getResource("/icon/computer.png")));
        txtMachine.setHint("Station Number (1-10)");
        txtMachine.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        login.add(txtMachine, "w 65%");

        // Login button with modern styling
        JButton btnLogin = new JButton("START GAMING") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2d.setColor(ACCENT_GREEN.darker());
                } else if (getModel().isRollover()) {
                    g2d.setColor(ACCENT_GREEN.brighter());
                } else {
                    g2d.setColor(ACCENT_GREEN);
                }
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                g2d.setColor(PRIMARY_DARK);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), x, y);
            }
        };
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLogin.setForeground(PRIMARY_DARK);
        btnLogin.setContentAreaFilled(false);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String username = txtUser.getText();
                char[] passByte = txtPass.getPassword();
                String password = new String(passByte);
                String machine = txtMachine.getText();
                if (username.isEmpty() || password.isEmpty() || machine.isEmpty()) {
                    JOptionPane.showMessageDialog(null, 
                        "Please fill in all fields to continue.", 
                        "Validation Error", 
                        JOptionPane.WARNING_MESSAGE);
                } else {
                    try {
                        performLogin(username, password, machine);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        login.add(btnLogin, "w 50%, h 42");
        
        // Footer
        JLabel lblFooter = new JLabel("© 2024 Gaming Center Pro - Play Hard, Win Big!");
        lblFooter.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblFooter.setForeground(TEXT_GRAY);
        login.add(lblFooter);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        login = new JPanel();
        setLayout(new CardLayout());
        login.setBackground(new Color(255, 255, 255));

        GroupLayout loginLayout = new GroupLayout(login);
        login.setLayout(loginLayout);
        loginLayout.setHorizontalGroup(
                loginLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 327, Short.MAX_VALUE)
        );
        loginLayout.setVerticalGroup(
                loginLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 300, Short.MAX_VALUE)
        );

        add(login, "card3");
    }

    private void performLogin(String username, String password, String machine) throws Exception {
        Socket socket = new Socket("localhost", 5010);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        out.writeObject(new DataTransfer("/*requestLogin", username + "," + password + "," + machine));
        out.flush();

        DataTransfer response = (DataTransfer) in.readObject();

        if ("/*acceptLogin".equals(response.getType())) {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof JFrame) {
                window.dispose();
                Home home = new Home(username, Integer.parseInt(machine), socket, in, out);
                home.setVisible(true);
            }
        } else if ("/*failLogin".equals(response.getType())) {
            String errorMsg = response.getData().toString();
            // Translate common error messages
            if (errorMsg.contains("Máy đã có người")) {
                errorMsg = "This station is already in use.";
            } else if (errorMsg.contains("Tài khoản đang được sử dụng")) {
                errorMsg = "This account is currently logged in elsewhere.";
            } else if (errorMsg.contains("Sai mật khẩu")) {
                errorMsg = "Invalid username or password.";
            }
            JOptionPane.showMessageDialog(this, errorMsg, "Login Failed", JOptionPane.ERROR_MESSAGE);
            socket.close();
        }
    }
}