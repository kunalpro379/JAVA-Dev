package modernjava;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import java.time.LocalDate;
import java.util.function.*;

class Order {
    private int id;
    private String customerName;
    private List<Product> products;
    private LocalDate orderDate;
    private OrderStatus status;

    public enum OrderStatus {
        NEW, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    }

    public Order(int id, String customerName, List<Product> products) {
        this.id = id;
        this.customerName = customerName;
        this.products = new ArrayList<>(products);
        this.orderDate = LocalDate.now();
        this.status = OrderStatus.NEW;
    }

    // Getters
    public int getId() { return id; }
    public String getCustomerName() { return customerName; }
    public List<Product> getProducts() { return new ArrayList<>(products); }
    public LocalDate getOrderDate() { return orderDate; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public double getTotalAmount() {
        return products.stream()
                      .mapToDouble(Product::getPrice)
                      .sum();
    }
}

class Product {
    private int id;
    private String name;
    private double price;
    private String category;

    public Product(int id, String name, double price, String category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getCategory() { return category; }
}

public class DataProcessingSystem {
    private List<Order> orders = new ArrayList<>();
    private Map<String, Double> categoryDiscounts = new HashMap<>();
    private ExecutorService executorService = Executors.newFixedThreadPool(4);

    // Demonstrating Supplier functional interface
    private final Supplier<String> orderIdGenerator = () -> 
        String.format("ORD-%d", ThreadLocalRandom.current().nextInt(10000, 100000));

    // Demonstrating Consumer functional interface
    private final Consumer<Order> orderLogger = order -> 
        System.out.println("Processing order: " + order.getId() + " for " + order.getCustomerName());

    // Demonstrating Predicate functional interface
    private final Predicate<Order> isLargeOrder = order -> 
        order.getTotalAmount() > 1000.0;

    // Demonstrating Function functional interface
    private final Function<Order, String> orderSummary = order ->
        String.format("Order #%d: Customer=%s, Total=%.2f, Status=%s",
            order.getId(), order.getCustomerName(), order.getTotalAmount(), order.getStatus());

    public void addOrder(Order order) {
        orders.add(order);
        // Demonstrate Consumer usage
        orderLogger.accept(order);
    }

    // Demonstrating Stream API
    public List<Order> getHighValueOrders() {
        return orders.stream()
            .filter(isLargeOrder)
            .sorted(Comparator.comparingDouble(Order::getTotalAmount).reversed())
            .collect(Collectors.toList());
    }

    // Demonstrating Optional
    public Optional<Order> findOrderById(int id) {
        return orders.stream()
            .filter(order -> order.getId() == id)
            .findFirst();
    }

    // Demonstrating CompletableFuture
    public CompletableFuture<Double> calculateOrderTotalAsync(int orderId) {
        return CompletableFuture.supplyAsync(() -> 
            findOrderById(orderId)
                .map(Order::getTotalAmount)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId))
        , executorService);
    }

    // Demonstrating Stream collectors and grouping
    public Map<Order.OrderStatus, List<Order>> getOrdersByStatus() {
        return orders.stream()
            .collect(Collectors.groupingBy(Order::getStatus));
    }

    public Map<String, Double> getRevenueByCategory() {
        return orders.stream()
            .flatMap(order -> order.getProducts().stream())
            .collect(Collectors.groupingBy(
                Product::getCategory,
                Collectors.summingDouble(Product::getPrice)
            ));
    }

    // Demonstrating parallel streams
    public double calculateTotalRevenue() {
        return orders.parallelStream()
            .mapToDouble(Order::getTotalAmount)
            .sum();
    }

    // Demonstrating method reference
    public void processOrders() {
        orders.forEach(this::processOrder);
    }

    private void processOrder(Order order) {
        // Demonstrate CompletableFuture with multiple async operations
        CompletableFuture<Void> processing = CompletableFuture
            .runAsync(() -> validateOrder(order))
            .thenRunAsync(() -> updateInventory(order))
            .thenRunAsync(() -> sendNotification(order));

        processing.exceptionally(throwable -> {
            System.err.println("Error processing order " + order.getId() + ": " + throwable.getMessage());
            return null;
        });
    }

    private void validateOrder(Order order) {
        // Simulate validation
        try {
            Thread.sleep(100);
            order.setStatus(Order.OrderStatus.PROCESSING);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Validation interrupted", e);
        }
    }

    private void updateInventory(Order order) {
        // Simulate inventory update
        try {
            Thread.sleep(200);
            order.setStatus(Order.OrderStatus.SHIPPED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Inventory update interrupted", e);
        }
    }

    private void sendNotification(Order order) {
        // Simulate sending notification
        try {
            Thread.sleep(100);
            order.setStatus(Order.OrderStatus.DELIVERED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Notification interrupted", e);
        }
    }

    public void cleanup() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        DataProcessingSystem system = new DataProcessingSystem();

        // Create sample products
        List<Product> products1 = Arrays.asList(
            new Product(1, "Laptop", 1200.0, "Electronics"),
            new Product(2, "Mouse", 25.0, "Electronics")
        );

        List<Product> products2 = Arrays.asList(
            new Product(3, "Book", 35.0, "Books"),
            new Product(4, "Pen", 5.0, "Stationery")
        );

        // Create and add orders
        system.addOrder(new Order(1, "John Doe", products1));
        system.addOrder(new Order(2, "Jane Smith", products2));

        // Demonstrate various operations
        System.out.println("\nHigh value orders:");
        system.getHighValueOrders().forEach(order -> 
            System.out.println(system.orderSummary.apply(order)));

        System.out.println("\nOrder lookup:");
        system.findOrderById(1).ifPresent(order -> 
            System.out.println("Found order: " + system.orderSummary.apply(order)));

        System.out.println("\nRevenue by category:");
        system.getRevenueByCategory().forEach((category, revenue) -> 
            System.out.println(category + ": $" + revenue));

        System.out.println("\nProcessing orders asynchronously...");
        system.processOrders();

        // Calculate total revenue asynchronously
        CompletableFuture<Double> totalRevenue = CompletableFuture
            .supplyAsync(() -> system.calculateTotalRevenue())
            .thenApply(total -> {
                System.out.println("\nTotal revenue: $" + total);
                return total;
            });

        // Wait for async operations to complete
        try {
            totalRevenue.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Error calculating total revenue: " + e.getMessage());
        }

        // Cleanup
        system.cleanup();
    }
}