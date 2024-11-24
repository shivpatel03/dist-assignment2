import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.util.List;
import java.util.Map;

public class Client extends UnicastRemoteObject implements ClientInterface {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private ChatInterface server;
    private String username;
    private String currentRoom = "general";

    public Client(String username) throws Exception {
        this.username = username;

        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        server = (ChatInterface) registry.lookup("ChatService");
        server.registerClient(username, this);
        server.joinRoom(username, "general");

        createGUI();
    }

    private void createGUI() {
        frame = new JFrame("Chat Client - " + username);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        inputField = new JTextField();
        inputField.addActionListener(e -> sendMessage(inputField.getText()));
        frame.add(inputField, BorderLayout.SOUTH);

        // Add menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu commandsMenu = new JMenu("Commands");

        addMenuItem(commandsMenu, "Private Message", e -> showPrivateMessageDialog());
        addMenuItem(commandsMenu, "Show Stats", e -> showStats());
        addMenuItem(commandsMenu, "Create Room", e -> createRoom());
        addMenuItem(commandsMenu, "Join Room", e -> joinRoom());
        addMenuItem(commandsMenu, "Show Rooms", e -> showRooms());
        addMenuItem(commandsMenu, "Upload File", e -> uploadFile());
        addMenuItem(commandsMenu, "Download File", e -> downloadFile());

        menuBar.add(commandsMenu);
        frame.setJMenuBar(menuBar);

        frame.setVisible(true);
    }

    private void addMenuItem(JMenu menu, String label, ActionListener listener) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(listener);
        menu.add(item);
    }

    private void sendMessage(String message) {
        try {
            if (message.trim().isEmpty())
                return;
            server.sendMessage(currentRoom, username, message);
            inputField.setText("");
        } catch (Exception e) {
            showError("Error sending message: " + e.getMessage());
        }
    }

    private void showPrivateMessageDialog() {
        String recipient = JOptionPane.showInputDialog("Enter recipient username:");
        if (recipient != null && !recipient.trim().isEmpty()) {
            String message = JOptionPane.showInputDialog("Enter message:");
            if (message != null && !message.trim().isEmpty()) {
                try {
                    server.sendPrivateMessage(username, recipient, message);
                } catch (Exception e) {
                    showError("Error sending private message: " + e.getMessage());
                }
            }
        }
    }

    private void showStats() {
        try {
            Map<String, Integer> stats = server.getChatStats();
            long uptime = server.getUptime();
            StringBuilder sb = new StringBuilder("Chat Statistics:\n\n");
            sb.append("Server uptime: ").append(uptime).append(" seconds\n\n");
            sb.append("Messages per user:\n");
            stats.forEach((user, count) -> sb.append(user).append(": ").append(count).append("\n"));
            JOptionPane.showMessageDialog(frame, sb.toString());
        } catch (Exception e) {
            showError("Error getting stats: " + e.getMessage());
        }
    }

    private void createRoom() {
        String roomName = JOptionPane.showInputDialog("Enter room name:");
        if (roomName != null && !roomName.trim().isEmpty()) {
            try {
                server.createRoom(roomName);
                appendToChatArea("System", "Room created: " + roomName);
            } catch (Exception e) {
                showError("Error creating room: " + e.getMessage());
            }
        }
    }

    private void joinRoom() {
        try {
            List<String> rooms = server.getAvailableRooms();
            String roomName = (String) JOptionPane.showInputDialog(
                    frame,
                    "Select room to join:",
                    "Join Room",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    rooms.toArray(),
                    rooms.get(0));

            if (roomName != null) {
                server.leaveRoom(username, currentRoom);
                server.joinRoom(username, roomName);
                currentRoom = roomName;
                appendToChatArea("System", "Joined room: " + roomName);
            }
        } catch (Exception e) {
            showError("Error joining room: " + e.getMessage());
        }
    }

    private void showRooms() {
        try {
            List<String> rooms = server.getAvailableRooms();
            JOptionPane.showMessageDialog(frame, "Available rooms:\n" + String.join("\n", rooms));
        } catch (Exception e) {
            showError("Error getting rooms: " + e.getMessage());
        }
    }

    private void uploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                byte[] fileData = new byte[(int) file.length()];
                FileInputStream fis = new FileInputStream(file);
                fis.read(fileData);
                fis.close();

                server.uploadFile(username, file.getName(), fileData);
                appendToChatArea("System", "File uploaded: " + file.getName());
            } catch (Exception e) {
                showError("Error uploading file: " + e.getMessage());
            }
        }
    }

    private void downloadFile() {
        try {
            List<String> files = server.getAvailableFiles();
            if (files.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No files available");
                return;
            }

            String fileName = (String) JOptionPane.showInputDialog(
                    frame,
                    "Select file to download:",
                    "Download File",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    files.toArray(),
                    files.get(0));

            if (fileName != null) {
                byte[] fileData = server.downloadFile(fileName);
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new File(fileName));
                if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    FileOutputStream fos = new FileOutputStream(fileChooser.getSelectedFile());
                    fos.write(fileData);
                    fos.close();
                    appendToChatArea("System", "File downloaded: " + fileName);
                }
            }
        } catch (Exception e) {
            showError("Error downloading file: " + e.getMessage());
        }
    }

    @Override
    public void receiveMessage(String room, String sender, String message) {
        if (room.equals(currentRoom)) {
            appendToChatArea(sender, message);
        }
    }

    @Override
    public void receivePrivateMessage(String sender, String message) {
        appendToChatArea(sender + " (private)", message);
    }

    private void appendToChatArea(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(sender + ": " + message + "\n");
        });
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Enter your username:");
        if (username != null && !username.trim().isEmpty()) {
            try {
                new Client(username);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Error starting client: " + e.getMessage());
            }
        }
    }
}
