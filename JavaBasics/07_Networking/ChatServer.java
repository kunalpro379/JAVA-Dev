package networking;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 5000;
    private Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private ExecutorService executor = Executors.newFixedThreadPool(20);

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chat Server is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                executor.execute(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            executor.shutdown();
        }
    }

    public void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(sender.getUsername() + ": " + message);
            }
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        broadcast(client.getUsername() + " has left the chat.", client);
    }

    public Set<String> getActiveUsers() {
        Set<String> users = new HashSet<>();
        for (ClientHandler client : clients) {
            users.add(client.getUsername());
        }
        return users;
    }

    public static void main(String[] args) {
        new ChatServer().start();
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private ChatServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            setupStreams();
            processClientConnection();
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void setupStreams() throws IOException {
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Get username
        out.println("Enter your username:");
        username = in.readLine();
        server.broadcast(username + " has joined the chat.", this);

        // Send active users list
        out.println("Active users: " + server.getActiveUsers());
    }

    private void processClientConnection() throws IOException {
        String message;
        while ((message = in.readLine()) != null) {
            if (message.equalsIgnoreCase("/quit")) {
                break;
            } else if (message.startsWith("/users")) {
                out.println("Active users: " + server.getActiveUsers());
            } else {
                server.broadcast(message, this);
            }
        }
    }

    private void cleanup() {
        server.removeClient(this);
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Error closing client socket: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String getUsername() {
        return username;
    }
}