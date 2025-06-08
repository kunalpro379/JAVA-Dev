package networking;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClient {
    private String serverAddress;
    private int serverPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private ChatWindow chatWindow;

    public ChatClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void start() {
        // Get username
        username = JOptionPane.showInputDialog(
            null,
            "Enter your username:",
            "Chat Client Login",
            JOptionPane.QUESTION_MESSAGE
        );

        if (username == null || username.trim().isEmpty()) {
            System.exit(0);
        }

        try {
            // Connect to server
            socket = new Socket(serverAddress, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Create and show chat window
            chatWindow = new ChatWindow();
            chatWindow.setVisible(true);

            // Start message receiving thread
            new Thread(this::receiveMessages).start();

            // Send username to server
            out.println(username);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                null,
                "Error connecting to server: " + e.getMessage(),
                "Connection Error",
                JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }
    }

    private void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                final String finalMessage = message;
                SwingUtilities.invokeLater(() -> 
                    chatWindow.appendMessage(finalMessage)
                );
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> 
                JOptionPane.showMessageDialog(
                    chatWindow,
                    "Lost connection to server: " + e.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE
                )
            );
        } finally {
            disconnect();
        }
    }

    private void sendMessage(String message) {
        out.println(message);
        chatWindow.appendMessage("You: " + message);
    }

    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                out.println("/quit");
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    // Chat Window GUI
    private class ChatWindow extends JFrame {
        private JTextArea chatArea;
        private JTextField messageField;
        private JButton sendButton;
        private JButton usersButton;

        public ChatWindow() {
            setTitle("Chat Client - " + username);
            setSize(500, 400);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Chat area
            chatArea = new JTextArea();
            chatArea.setEditable(false);
            chatArea.setWrapStyleWord(true);
            chatArea.setLineWrap(true);
            JScrollPane scrollPane = new JScrollPane(chatArea);

            // Message input area
            messageField = new JTextField();
            sendButton = new JButton("Send");
            usersButton = new JButton("Users");

            // Layout
            JPanel inputPanel = new JPanel(new BorderLayout());
            inputPanel.add(messageField, BorderLayout.CENTER);
            inputPanel.add(sendButton, BorderLayout.EAST);
            inputPanel.add(usersButton, BorderLayout.WEST);

            add(scrollPane, BorderLayout.CENTER);
            add(inputPanel, BorderLayout.SOUTH);

            // Event handlers
            sendButton.addActionListener(e -> sendCurrentMessage());
            usersButton.addActionListener(e -> out.println("/users"));
            messageField.addActionListener(e -> sendCurrentMessage());

            // Window closing handler
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    disconnect();
                }
            });
        }

        private void sendCurrentMessage() {
            String message = messageField.getText().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageField.setText("");
            }
            messageField.requestFocus();
        }

        public void appendMessage(String message) {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
    }

    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Error setting look and feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            ChatClient client = new ChatClient("localhost", 5000);
            client.start();
        });
    }
}