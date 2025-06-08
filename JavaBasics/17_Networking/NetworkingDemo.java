package networking;

import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

// Modern HTTP Client demonstration
class HttpClientDemo {
    private final HttpClient client;

    public HttpClientDemo() {
        client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public String synchronousGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        return response.body();
    }

    public CompletableFuture<String> asynchronousGet(String url) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .GET()
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body);
    }

    public String postData(String url, String data) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(data))
            .build();

        HttpResponse<String> response = client.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        return response.body();
    }
}

// WebSocket Server implementation
class WebSocketServer {
    private final ServerSocketChannel serverChannel;
    private final Selector selector;
    private final Map<SocketChannel, ByteBuffer> clients;

    public WebSocketServer(int port) throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);
        
        selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        
        clients = new ConcurrentHashMap<>();
    }

    public void start() {
        try {
            while (true) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if (key.isAcceptable()) {
                        register(selector, serverChannel);
                    }

                    if (key.isReadable()) {
                        readData(key);
                    }

                    iter.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void register(Selector selector, ServerSocketChannel serverChannel) 
            throws IOException {
        SocketChannel client = serverChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        clients.put(client, ByteBuffer.allocate(1024));
        System.out.println("New client connected: " + client);
    }

    private void readData(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = clients.get(client);
        buffer.clear();
        
        try {
            int read = client.read(buffer);
            
            if (read == -1) {
                clients.remove(client);
                client.close();
                key.cancel();
                System.out.println("Client disconnected");
                return;
            }

            buffer.flip();
            byte[] data = new byte[buffer.limit()];
            buffer.get(data);
            
            String message = new String(data, StandardCharsets.UTF_8).trim();
            System.out.println("Received: " + message);

            // Echo back to all clients
            broadcast(message);
            
        } catch (IOException e) {
            clients.remove(client);
            client.close();
            key.cancel();
            System.out.println("Client forcibly disconnected");
        }
    }

    private void broadcast(String message) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        for (SocketChannel client : clients.keySet()) {
            buffer.rewind();
            client.write(buffer);
        }
    }
}

// Asynchronous Network Client
class AsyncNetworkClient {
    private final AsynchronousSocketChannel client;
    private final CountDownLatch connectLatch;

    public AsyncNetworkClient() throws IOException {
        this.client = AsynchronousSocketChannel.open();
        this.connectLatch = new CountDownLatch(1);
    }

    public void connect(String host, int port) throws Exception {
        client.connect(new InetSocketAddress(host, port), null, 
            new CompletionHandler<Void, Void>() {
                @Override
                public void completed(Void result, Void attachment) {
                    System.out.println("Connected to server");
                    connectLatch.countDown();
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    System.err.println("Failed to connect: " + exc.getMessage());
                    connectLatch.countDown();
                }
            });

        connectLatch.await();
    }

    public Future<Integer> sendMessage(String message) {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        return client.write(buffer);
    }

    public Future<Integer> receiveMessage(ByteBuffer buffer) {
        return client.read(buffer);
    }

    public void close() throws IOException {
        client.close();
    }
}

// Non-blocking I/O Server
class NonBlockingServer {
    private final ServerSocketChannel serverChannel;
    private final Selector selector;

    public NonBlockingServer(int port) throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        
        selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void start() throws IOException {
        System.out.println("Server started on port " + 
            serverChannel.socket().getLocalPort());

        while (true) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    accept(key);
                } else if (key.isReadable()) {
                    read(key);
                }
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        System.out.println("New connection accepted");
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int read = channel.read(buffer);

        if (read == -1) {
            channel.close();
            key.cancel();
            System.out.println("Connection closed by client");
            return;
        }

        buffer.flip();
        byte[] data = new byte[buffer.limit()];
        buffer.get(data);
        String message = new String(data).trim();
        System.out.println("Received: " + message);

        // Echo back
        buffer.rewind();
        channel.write(buffer);
    }
}

public class NetworkingDemo {
    public static void main(String[] args) {
        try {
            // Demonstrate HTTP Client
            HttpClientDemo httpDemo = new HttpClientDemo();
            
            // Synchronous GET request
            System.out.println("Performing synchronous GET request...");
            String response = httpDemo.synchronousGet("https://api.github.com/users/octocat");
            System.out.println("Response: " + response);

            // Asynchronous GET request
            System.out.println("\nPerforming asynchronous GET request...");
            CompletableFuture<String> futureResponse = 
                httpDemo.asynchronousGet("https://api.github.com/users/octocat");
            futureResponse.thenAccept(r -> System.out.println("Async Response: " + r));

            // Start WebSocket server in a separate thread
            System.out.println("\nStarting WebSocket server...");
            WebSocketServer wsServer = new WebSocketServer(8080);
            new Thread(wsServer::start).start();

            // Demonstrate Async Network Client
            System.out.println("\nTesting Async Network Client...");
            AsyncNetworkClient asyncClient = new AsyncNetworkClient();
            asyncClient.connect("localhost", 8080);
            
            Future<Integer> sendFuture = asyncClient.sendMessage("Hello, Server!");
            sendFuture.get(); // Wait for send to complete

            ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);
            Future<Integer> receiveFuture = asyncClient.receiveMessage(receiveBuffer);
            receiveFuture.get(); // Wait for receive to complete

            // Start Non-blocking server in a separate thread
            System.out.println("\nStarting Non-blocking server...");
            NonBlockingServer nbServer = new NonBlockingServer(8081);
            new Thread(() -> {
                try {
                    nbServer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Give some time for servers to start
            Thread.sleep(1000);

            // Clean up
            asyncClient.close();
            System.out.println("\nNetworking demo completed");

        } catch (Exception e) {
            System.err.println("Error in networking demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}