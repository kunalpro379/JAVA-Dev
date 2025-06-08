package networking;

import java.nio.channels.*;
import java.nio.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.*;
import java.security.*;
import javax.net.ssl.*;
import java.time.*;

public class AdvancedNetworkServer {
    private final AsynchronousServerSocketChannel serverChannel;
    private final ExecutorService workerPool;
    private final ConnectionPool connectionPool;
    private final SSLContext sslContext;
    private final ServerMetrics metrics;
    private final ProtocolHandler protocolHandler;
    private volatile boolean isRunning;

    public AdvancedNetworkServer(ServerConfig config) throws Exception {
        this.workerPool = Executors.newFixedThreadPool(
            config.workerThreads,
            new ServerThreadFactory()
        );
        this.connectionPool = new ConnectionPool(config.maxConnections);
        this.sslContext = createSSLContext(config.keyStorePath, 
                                         config.keyStorePassword);
        this.metrics = new ServerMetrics();
        this.protocolHandler = new ProtocolHandler();
        this.isRunning = true;

        // Initialize server channel
        this.serverChannel = AsynchronousServerSocketChannel.open()
            .bind(new InetSocketAddress(config.port));
    }

    public void start() {
        acceptConnections();
        startMetricsReporting();
    }

    private void acceptConnections() {
        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel,
                                                       Void>() {
            @Override
            public void completed(AsynchronousSocketChannel clientChannel, 
                                Void attachment) {
                // Accept next connection
                if (isRunning) {
                    serverChannel.accept(null, this);
                }

                handleClient(clientChannel);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                metrics.recordAcceptError();
                if (isRunning) {
                    serverChannel.accept(null, this);
                }
            }
        });
    }

    private void handleClient(AsynchronousSocketChannel clientChannel) {
        try {
            // Wrap client channel with SSL
            SSLEngine engine = sslContext.createSSLEngine();
            engine.setUseClientMode(false);
            engine.setNeedClientAuth(false);

            // Get connection from pool
            PooledConnection connection = connectionPool.acquire();
            if (connection == null) {
                metrics.recordConnectionRejected();
                clientChannel.close();
                return;
            }

            // Initialize connection
            connection.init(clientChannel, engine);
            metrics.recordConnectionAccepted();

            // Start reading
            readFromClient(connection);

        } catch (Exception e) {
            metrics.recordHandleError();
            try {
                clientChannel.close();
            } catch (IOException ignored) {}
        }
    }

    private void readFromClient(PooledConnection connection) {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        connection.getChannel().read(buffer, connection, 
            new CompletionHandler<Integer, PooledConnection>() {
                @Override
                public void completed(Integer bytesRead, 
                                    PooledConnection connection) {
                    if (bytesRead == -1) {
                        closeConnection(connection);
                        return;
                    }

                    try {
                        // Process received data
                        buffer.flip();
                        byte[] data = new byte[buffer.remaining()];
                        buffer.get(data);
                        
                        // Handle protocol
                        CompletableFuture.runAsync(() -> {
                            try {
                                byte[] response = protocolHandler.handleRequest(data);
                                writeToClient(connection, response);
                            } catch (Exception e) {
                                metrics.recordProtocolError();
                                closeConnection(connection);
                            }
                        }, workerPool);

                        // Continue reading
                        buffer.clear();
                        connection.getChannel().read(buffer, connection, this);

                    } catch (Exception e) {
                        metrics.recordReadError();
                        closeConnection(connection);
                    }
                }

                @Override
                public void failed(Throwable exc, PooledConnection connection) {
                    metrics.recordReadError();
                    closeConnection(connection);
                }
            });
    }

    private void writeToClient(PooledConnection connection, byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        connection.getChannel().write(buffer, connection,
            new CompletionHandler<Integer, PooledConnection>() {
                @Override
                public void completed(Integer bytesWritten, 
                                    PooledConnection connection) {
                    if (buffer.hasRemaining()) {
                        connection.getChannel().write(buffer, connection, this);
                    }
                    metrics.recordBytesWritten(bytesWritten);
                }

                @Override
                public void failed(Throwable exc, PooledConnection connection) {
                    metrics.recordWriteError();
                    closeConnection(connection);
                }
            });
    }

    private void closeConnection(PooledConnection connection) {
        try {
            connection.getChannel().close();
        } catch (IOException ignored) {}
        connectionPool.release(connection);
        metrics.recordConnectionClosed();
    }

    private SSLContext createSSLContext(String keyStorePath, 
                                      String keyStorePassword) 
            throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keyStorePath), 
                     keyStorePassword.toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyStorePassword.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);
        return sslContext;
    }

    private void startMetricsReporting() {
        ScheduledExecutorService scheduler = 
            Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            ServerMetrics.Snapshot snapshot = metrics.snapshot();
            System.out.println("\nServer Metrics:");
            System.out.println("Active connections: " + 
                snapshot.getActiveConnections());
            System.out.println("Total connections: " + 
                snapshot.getTotalConnections());
            System.out.println("Rejected connections: " + 
                snapshot.getRejectedConnections());
            System.out.println("Bytes written: " + snapshot.getBytesWritten());
            System.out.println("Accept errors: " + snapshot.getAcceptErrors());
            System.out.println("Read errors: " + snapshot.getReadErrors());
            System.out.println("Write errors: " + snapshot.getWriteErrors());
            System.out.println("Protocol errors: " + 
                snapshot.getProtocolErrors());
        }, 1, 1, TimeUnit.MINUTES);
    }

    public void shutdown() {
        isRunning = false;
        try {
            serverChannel.close();
        } catch (IOException ignored) {}
        workerPool.shutdown();
        try {
            if (!workerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Connection pool
    private static class ConnectionPool {
        private final Queue<PooledConnection> availableConnections;
        private final Set<PooledConnection> activeConnections;
        private final int maxConnections;

        public ConnectionPool(int maxConnections) {
            this.maxConnections = maxConnections;
            this.availableConnections = new ConcurrentLinkedQueue<>();
            this.activeConnections = ConcurrentHashMap.newKeySet();

            // Pre-create connections
            for (int i = 0; i < maxConnections; i++) {
                availableConnections.offer(new PooledConnection());
            }
        }

        public PooledConnection acquire() {
            if (activeConnections.size() >= maxConnections) {
                return null;
            }

            PooledConnection connection = availableConnections.poll();
            if (connection != null) {
                activeConnections.add(connection);
            }
            return connection;
        }

        public void release(PooledConnection connection) {
            if (activeConnections.remove(connection)) {
                connection.reset();
                availableConnections.offer(connection);
            }
        }
    }

    // Pooled connection
    private static class PooledConnection {
        private AsynchronousSocketChannel channel;
        private SSLEngine sslEngine;

        public void init(AsynchronousSocketChannel channel, SSLEngine engine) {
            this.channel = channel;
            this.sslEngine = engine;
        }

        public void reset() {
            this.channel = null;
            this.sslEngine = null;
        }

        public AsynchronousSocketChannel getChannel() {
            return channel;
        }

        public SSLEngine getSSLEngine() {
            return sslEngine;
        }
    }

    // Protocol handler
    private static class ProtocolHandler {
        public byte[] handleRequest(byte[] request) throws Exception {
            // Implement your protocol here
            // This is a simple echo implementation
            return request;
        }
    }

    // Thread factory
    private static class ServerThreadFactory implements ThreadFactory {
        private final AtomicInteger threadCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("ServerWorker-" + threadCount.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }

    // Server configuration
    public static class ServerConfig {
        private final int port;
        private final int workerThreads;
        private final int maxConnections;
        private final String keyStorePath;
        private final String keyStorePassword;

        private ServerConfig(Builder builder) {
            this.port = builder.port;
            this.workerThreads = builder.workerThreads;
            this.maxConnections = builder.maxConnections;
            this.keyStorePath = builder.keyStorePath;
            this.keyStorePassword = builder.keyStorePassword;
        }

        public static class Builder {
            private int port = 8443;
            private int workerThreads = Runtime.getRuntime().availableProcessors();
            private int maxConnections = 1000;
            private String keyStorePath;
            private String keyStorePassword;

            public Builder port(int port) {
                this.port = port;
                return this;
            }

            public Builder workerThreads(int threads) {
                this.workerThreads = threads;
                return this;
            }

            public Builder maxConnections(int connections) {
                this.maxConnections = connections;
                return this;
            }

            public Builder keyStore(String path, String password) {
                this.keyStorePath = path;
                this.keyStorePassword = password;
                return this;
            }

            public ServerConfig build() {
                if (keyStorePath == null || keyStorePassword == null) {
                    throw new IllegalStateException(
                        "KeyStore configuration is required");
                }
                return new ServerConfig(this);
            }
        }
    }

    // Server metrics
    private static class ServerMetrics {
        private final AtomicLong totalConnections = new AtomicLong();
        private final AtomicLong activeConnections = new AtomicLong();
        private final AtomicLong rejectedConnections = new AtomicLong();
        private final AtomicLong bytesWritten = new AtomicLong();
        private final AtomicLong acceptErrors = new AtomicLong();
        private final AtomicLong handleErrors = new AtomicLong();
        private final AtomicLong readErrors = new AtomicLong();
        private final AtomicLong writeErrors = new AtomicLong();
        private final AtomicLong protocolErrors = new AtomicLong();

        public void recordConnectionAccepted() {
            totalConnections.incrementAndGet();
            activeConnections.incrementAndGet();
        }

        public void recordConnectionClosed() {
            activeConnections.decrementAndGet();
        }

        public void recordConnectionRejected() {
            rejectedConnections.incrementAndGet();
        }

        public void recordBytesWritten(long bytes) {
            bytesWritten.addAndGet(bytes);
        }

        public void recordAcceptError() {
            acceptErrors.incrementAndGet();
        }

        public void recordHandleError() {
            handleErrors.incrementAndGet();
        }

        public void recordReadError() {
            readErrors.incrementAndGet();
        }

        public void recordWriteError() {
            writeErrors.incrementAndGet();
        }

        public void recordProtocolError() {
            protocolErrors.incrementAndGet();
        }

        public Snapshot snapshot() {
            return new Snapshot(
                totalConnections.get(),
                activeConnections.get(),
                rejectedConnections.get(),
                bytesWritten.get(),
                acceptErrors.get(),
                handleErrors.get(),
                readErrors.get(),
                writeErrors.get(),
                protocolErrors.get()
            );
        }

        public static class Snapshot {
            private final long totalConnections;
            private final long activeConnections;
            private final long rejectedConnections;
            private final long bytesWritten;
            private final long acceptErrors;
            private final long handleErrors;
            private final long readErrors;
            private final long writeErrors;
            private final long protocolErrors;

            public Snapshot(
                long totalConnections,
                long activeConnections,
                long rejectedConnections,
                long bytesWritten,
                long acceptErrors,
                long handleErrors,
                long readErrors,
                long writeErrors,
                long protocolErrors
            ) {
                this.totalConnections = totalConnections;
                this.activeConnections = activeConnections;
                this.rejectedConnections = rejectedConnections;
                this.bytesWritten = bytesWritten;
                this.acceptErrors = acceptErrors;
                this.handleErrors = handleErrors;
                this.readErrors = readErrors;
                this.writeErrors = writeErrors;
                this.protocolErrors = protocolErrors;
            }

            public long getTotalConnections() { return totalConnections; }
            public long getActiveConnections() { return activeConnections; }
            public long getRejectedConnections() { return rejectedConnections; }
            public long getBytesWritten() { return bytesWritten; }
            public long getAcceptErrors() { return acceptErrors; }
            public long getHandleErrors() { return handleErrors; }
            public long getReadErrors() { return readErrors; }
            public long getWriteErrors() { return writeErrors; }
            public long getProtocolErrors() { return protocolErrors; }
        }
    }

    // Example usage
    public static void main(String[] args) throws Exception {
        // Create keystore for SSL
        String keyStorePath = "server.jks";
        String keyStorePassword = "password";
        
        // Configure and create server
        ServerConfig config = new ServerConfig.Builder()
            .port(8443)
            .workerThreads(4)
            .maxConnections(100)
            .keyStore(keyStorePath, keyStorePassword)
            .build();

        AdvancedNetworkServer server = new AdvancedNetworkServer(config);
        
        // Start server
        server.start();
        
        // Wait for shutdown signal
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        
        // Keep main thread alive
        Thread.currentThread().join();
    }
}