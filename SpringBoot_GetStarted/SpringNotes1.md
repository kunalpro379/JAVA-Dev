# Spring Boot?

Spring Boot is built on the top of the Spring Framework and simplifies the process of developing production-ready applications with minimal configuration.

---

## Features and Advantages

1. **Auto-Configuration**

   Spring Boot automatically configures your application based on the dependencies you have added. It uses sensible defaults and reduces the need for manual configuration.

2. **Rapid Application Development**

   With Spring Boot, you can quickly create production-ready applications with minimal setup time. The framework handles most of the boilerplate code.

3. **Standalone Applications**

   Spring Boot applications can run as standalone JAR files with embedded servers, eliminating the need for external deployment descriptors.

4. **Production Ready**

   Built-in features like health checks, metrics, and externalized configuration make Spring Boot applications production-ready out of the box.

5. **Opinionated Defaults**

   Spring Boot provides opinionated defaults that work well for most applications, but can be overridden when needed.

6. **No XML Configuration**

   Spring Boot uses Java-based configuration and annotations, eliminating the need for XML configuration files.

7. **Embedded Web Servers**

   Supports Tomcat, Jetty, and Undertow as embedded servers, allowing applications to run without deploying to external servers.

8. **Reduced Boilerplate Code**

   Spring Boot eliminates boilerplate code through auto-configuration and starter dependencies.

9. **Easy Dependency Management**

   Starter dependencies provide a curated set of dependencies for common use cases, simplifying dependency management.

10. **Seamless Integration with Ecosystem**

    Compatible with Spring Data JPA, Spring Security, Spring WebFlux, Spring Batch, and other Spring ecosystem projects.

11. **Flexible Configuration**

    Supports multiple configuration sources including properties files, YAML files, environment variables, and command-line arguments.

12. **Community and Ecosystem Support**

    Large community, extensive documentation, and rich ecosystem of third-party libraries and tools.

---

## Use-Cases

- **RESTful API Development**

  Spring Boot is ideal for building RESTful web services and APIs with minimal configuration.

- **Microservice Architecture**

  Lightweight nature and embedded servers make Spring Boot perfect for microservice architectures.

- **Rapid Prototyping**

  Quick setup and auto-configuration enable rapid development and prototyping of applications.

- **Enterprise Web Applications**

  Suitable for building scalable enterprise-level web applications with complex business logic.

- **Cloud-Native Applications**

  Spring Boot applications are well-suited for cloud deployment and containerization.

### Language Support

Java, Kotlin, Groovy, Scala

---

## Spring Boot Flow Architecture

<img width="914" height="500" alt="image" src="https://github.com/user-attachments/assets/baa0c8a3-fe49-4039-9f8f-3990e813c9ec" />

Spring Boot consists of the following layers:

- **Spring Core**: Base dependency injection and AOP framework

- **Auto Configuration**: Automatically configures beans based on classpath

- **Starter Dependencies**: Predefined sets of dependencies for various features (web, data, etc.)

- **Spring Boot CLI**: Command-line tool to run and test applications

- **Embedded Server**: Tomcat, Jetty, or Undertow

- **Spring Boot Actuator**: Provides operational endpoints for monitoring and management

<img width="919" height="464" alt="image" src="https://github.com/user-attachments/assets/5fd8d22d-8204-47bc-97f3-6e20a8c6e57b" />

Create a Spring Boot project using Spring Initializer.

**Application Entry Point**: `@SpringBootApplication`

---

# Core Concepts

## Spring Boot Annotations

### 1. @SpringBootApplication

The primary entry point of any Spring Boot application.

**Composition**:

`@SpringBootApplication` = `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`

**Declaration**:

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

**Explanation**:

- `@Configuration`: Indicates that the class contains bean definitions
- `@EnableAutoConfiguration`: Enables Spring Boot's auto-configuration mechanism
- `@ComponentScan`: Enables component scanning to discover and register beans

**Example with Detailed Flow**:

```java
@SpringBootApplication
public class ECommerceApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = 
            SpringApplication.run(ECommerceApplication.class, args);
        
        // Application context is now ready
        System.out.println("Application started successfully");
    }
}
```

---

## Stereotype Annotations

### 1. @Component

Generic stereotype annotation that marks a class as a Spring-managed component.

**Usage**:

Used when the class doesn't clearly fall into service, repository, or controller layers.

**Example**:

```java
@Component
public class MyComponent {
    public void work() {
        System.out.println("Component is working");
    }
}
```

**Detailed Example**:

```java
@Component
public class EmailValidator {
    public boolean isValidEmail(String email) {
        return email != null && email.contains("@");
    }
}

// Usage in another component
@Service
public class UserService {
    private final EmailValidator emailValidator;
    
    @Autowired
    public UserService(EmailValidator emailValidator) {
        this.emailValidator = emailValidator;
    }
    
    public void registerUser(String email) {
        if (emailValidator.isValidEmail(email)) {
            // Register user
        }
    }
}
```

### 2. @Service

`@Service` is a specialization of `@Component` used to annotate service layer classes that contain business logic.

**Benefits**:

- Makes the code more readable and structured
- Can integrate with AOP (for transaction, logging, etc.)

**Example**:

```java
@Service
public class OrderService {
    public void processOrder() {
        // Business logic
        System.out.println("Processing order");
    }
}
```

**Detailed Example with Dependencies**:

```java
@Service
public class OrderService {
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    
    @Autowired
    public OrderService(PaymentService paymentService, 
                       InventoryService inventoryService) {
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
    }
    
    public void processOrder(Order order) {
        // Check inventory
        if (inventoryService.isAvailable(order.getProductId())) {
            // Process payment
            paymentService.processPayment(order.getAmount());
            // Update inventory
            inventoryService.updateStock(order.getProductId());
            System.out.println("Order processed successfully");
        }
    }
}
```

### 3. @Repository

`@Repository` is a specialization of `@Component` used for DAO (Data Access Object) classes. It also provides exception translation for database errors.

**Benefits**:

- Used for data persistence
- Automatically translates exceptions to Spring's DataAccessException

**Example**:

```java
@Repository
public class ProductRepository {
    public void save(Product product) {
        // Save to DB
        System.out.println("Product saved: " + product.getName());
    }
}
```

**Detailed Example with JPA**:

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByName(String name);
    List<Product> findByPriceGreaterThan(double price);
}

// Usage
@Service
public class ProductService {
    private final ProductRepository productRepository;
    
    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }
    
    public List<Product> findExpensiveProducts(double minPrice) {
        return productRepository.findByPriceGreaterThan(minPrice);
    }
}
```

### 4. @Controller

`@Controller` is used to define web layer classes in Spring MVC. It is used for rendering views (HTML) with model data.

**Usage**:

Used when working with traditional web applications (Thymeleaf, JSP, etc.)

**Example**:

```java
@Controller
public class HomeController {
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("message", "Welcome");
        return "index"; // returns view name
    }
}
```

**Detailed Example with Multiple Endpoints**:

```java
@Controller
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;
    
    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    @GetMapping("/list")
    public String listProducts(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        return "product-list";
    }
    
    @GetMapping("/{id}")
    public String viewProduct(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.getProductById(id));
        return "product-detail";
    }
    
    @PostMapping("/create")
    public String createProduct(@ModelAttribute Product product) {
        productService.createProduct(product);
        return "redirect:/products/list";
    }
}
```

### 5. @RestController

`@RestController` = `@Controller` + `@ResponseBody`

Used to define RESTful web services. The return values are serialized as JSON or XML and sent directly to the client.

**Usage**:

- Used in REST APIs
- Automatically serializes Java objects to JSON

**Example**:

```java
@RestController
public class UserController {
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userService.getAll();
    }
}
```

**Detailed Example with CRUD Operations**:

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }
    
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        User updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(updatedUser);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## @Autowired and @Qualifier

### @Autowired

`@Autowired` is used to automatically inject a bean from the Spring container into another bean by type.

**Where It Can Be Used**:

- On fields
- On constructors
- On setter methods

**Example - Field Injection**:

```java
@Component
public class CarService {
    @Autowired
    private Engine engine;
    // engine will be injected automatically
}
```

**Example - Constructor Injection (Recommended)**:

```java
@Service
public class OrderService {
    private final PaymentService paymentService;
    
    @Autowired
    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    public void placeOrder() {
        paymentService.processPayment();
    }
}
```

**Example - Setter Injection**:

```java
@Component
public class NotificationService {
    private EmailService emailService;
    
    @Autowired
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }
    
    public void sendNotification(String message) {
        emailService.sendEmail(message);
    }
}
```

**Constructor Injection vs Manual Instantiation**:

**Manual Instantiation (Tightly Coupled)**:

```java
public class OrderService {
    private PaymentService paymentService;
    
    public OrderService() {
        this.paymentService = new PaymentService(); // Tightly coupled
    }
}
```

**With Spring DI (Loosely Coupled)**:

```java
@Service
public class OrderService {
    private final PaymentService paymentService;
    
    @Autowired
    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService; // Loosely coupled, easily testable
    }
}
```

### @Qualifier

`@Qualifier` is used with `@Autowired` to specify which bean to inject when multiple candidates of the same type are available.

**Usage**:

- Disambiguating among multiple beans of the same type
- Explicitly specifying the bean to be injected

**Example**:

```java
// Multiple implementations
@Component("dieselEngine")
public class DieselEngine implements Engine {
    @Override
    public void start() {
        System.out.println("Diesel engine started");
    }
}

@Component("petrolEngine")
public class PetrolEngine implements Engine {
    @Override
    public void start() {
        System.out.println("Petrol engine started");
    }
}

// Using @Qualifier to specify which implementation
@Service
public class CarService {
    @Autowired
    @Qualifier("dieselEngine")
    private Engine engine;
    
    public void startCar() {
        engine.start();
    }
}
```

**Detailed Example with Multiple Implementations**:

```java
// Interface
public interface PaymentProcessor {
    void processPayment(double amount);
}

// Implementation 1
@Component("creditCardProcessor")
public class CreditCardProcessor implements PaymentProcessor {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing credit card payment: $" + amount);
    }
}

// Implementation 2
@Component("paypalProcessor")
public class PayPalProcessor implements PaymentProcessor {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing PayPal payment: $" + amount);
    }
}

// Service using @Qualifier
@Service
public class PaymentService {
    private final PaymentProcessor paymentProcessor;
    
    @Autowired
    public PaymentService(@Qualifier("creditCardProcessor") PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }
    
    public void makePayment(double amount) {
        paymentProcessor.processPayment(amount);
    }
}
```

---

## @Configuration and @Bean

### @Configuration

`@Configuration` is a class-level annotation that indicates the class contains bean definitions for the Spring IoC container. It serves as a replacement for XML-based configuration.

**Example**:

```java
@Configuration
public class AppConfig {
    // Bean definitions go here
}
```

**Detailed Example**:

```java
@Configuration
@ComponentScan(basePackages = "com.example")
public class AppConfig {
    // Configuration for the application
}
```

### @Bean

`@Bean` is a method-level annotation used inside a `@Configuration`-annotated class to define a bean explicitly.

**Example**:

```java
@Configuration
public class AppConfig {
    @Bean
    public DataSource dataSource() {
        return new HikariDataSource();
    }
    
    @Bean
    public UserService userService() {
        return new UserServiceImpl(userRepository());
    }
    
    @Bean
    public UserRepository userRepository() {
        return new InMemoryUserRepository();
    }
}
```

**Detailed Example with Dependencies**:

```java
@Configuration
public class DatabaseConfig {
    
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/mydb");
        config.setUsername("root");
        config.setPassword("password");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return new HikariDataSource(config);
    }
    
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    
    @Bean
    public UserRepository userRepository(JdbcTemplate jdbcTemplate) {
        return new UserRepositoryImpl(jdbcTemplate);
    }
    
    @Bean
    public UserService userService(UserRepository userRepository) {
        return new UserServiceImpl(userRepository);
    }
}
```

**Example with Conditional Bean Creation**:

```java
@Configuration
public class AppConfig {
    
    @Bean
    @ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
    public CacheManager cacheManager() {
        return new SimpleCacheManager();
    }
    
    @Bean
    @Profile("dev")
    public DataSource devDataSource() {
        return new HikariDataSource();
    }
    
    @Bean
    @Profile("prod")
    public DataSource prodDataSource() {
        // Production data source configuration
        return new HikariDataSource();
    }
}
```

---

## Dependency Injection (DI)

Dependency Injection is a technique where an object receives its dependencies from an external source rather than creating them internally.

### Why Use Dependency Injection?

- Promotes loose coupling between classes
- Improves testability and maintainability
- Encourages modular and reusable code
- Follows SOLID principles (especially the "D" — Dependency Inversion Principle)

### Types of Dependency Injection in Spring

#### a) Constructor Injection

Dependencies are passed through a constructor. This is the recommended approach by Spring.

**Example**:

```java
@Service
public class OrderService {
    private final PaymentService paymentService;
    
    @Autowired
    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    public void placeOrder() {
        paymentService.processPayment();
    }
}
```

**Explanation**:

- `OrderService` depends on `PaymentService`
- The Spring container injects `PaymentService` at runtime
- The dependency is marked as `final`, making it immutable after construction

**Complete Example**:

```java
// Service interface
public interface PaymentService {
    void processPayment(double amount);
}

// Service implementation
@Service
public class PaymentServiceImpl implements PaymentService {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing payment: $" + amount);
    }
}

// Service using constructor injection
@Service
public class OrderService {
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    
    @Autowired
    public OrderService(PaymentService paymentService, 
                       InventoryService inventoryService) {
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
    }
    
    public void placeOrder(Order order) {
        if (inventoryService.checkAvailability(order.getProductId())) {
            paymentService.processPayment(order.getAmount());
            inventoryService.updateStock(order.getProductId());
            System.out.println("Order placed successfully");
        }
    }
}
```

#### b) Setter Injection

Dependencies are set via setter methods.

**Example**:

```java
@Component
public class Student {
    private Address address;
    
    @Autowired
    public void setAddress(Address address) {
        this.address = address;
    }
    
    public Address getAddress() {
        return address;
    }
}
```

**Complete Example**:

```java
@Component
public class NotificationService {
    private EmailService emailService;
    private SMSService smsService;
    
    @Autowired
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }
    
    @Autowired
    public void setSmsService(SMSService smsService) {
        this.smsService = smsService;
    }
    
    public void sendNotification(String message) {
        emailService.sendEmail(message);
        smsService.sendSMS(message);
    }
}
```

#### c) Field Injection

Dependencies are injected directly into fields using `@Autowired`. This approach is not recommended but is still commonly used.

**Example**:

```java
@Component
public class Car {
    @Autowired
    private Engine engine;
    
    public void start() {
        engine.start();
    }
}
```

**Note on Field Injection**:

While field injection works, it has drawbacks:
- Makes testing harder (requires reflection or Spring context)
- Hides dependencies (not visible in constructor)
- Cannot make fields final

**Complete Example**:

```java
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    public void registerUser(User user) {
        userRepository.save(user);
        emailService.sendWelcomeEmail(user.getEmail());
    }
}
```

### Spring Annotations for DI

| Annotation | Purpose |
|------------|---------|
| `@Autowired` | Marks a dependency to be injected by Spring |
| `@Qualifier` | Resolves ambiguity when multiple beans of same type |
| `@Inject` (JSR-330) | Java standard equivalent of `@Autowired` |
| `@Value` | Injects values from properties or expressions |

**Example with @Value**:

```java
@Component
public class AppConfig {
    @Value("${app.name}")
    private String appName;
    
    @Value("${app.version}")
    private String appVersion;
    
    @Value("${server.port:8080}")
    private int serverPort;
    
    public void printConfig() {
        System.out.println("App: " + appName + ", Version: " + appVersion);
        System.out.println("Server Port: " + serverPort);
    }
}
```

### Benefits of Dependency Injection

- **Loose Coupling**: Classes are not tightly bound to specific implementations
- **Testability**: Easier to write unit tests by injecting mock dependencies
- **Readability**: Clearly defines class dependencies
- **Maintainability**: Changes to one class do not affect dependent classes directly

**Testing Example**:

```java
// Production code
@Service
public class OrderService {
    private final PaymentService paymentService;
    
    @Autowired
    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    public void processOrder(Order order) {
        paymentService.processPayment(order.getAmount());
    }
}

// Test code with mock
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private PaymentService paymentService;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    void testProcessOrder() {
        Order order = new Order(100.0);
        orderService.processOrder(order);
        verify(paymentService).processPayment(100.0);
    }
}
```

---

## Inversion of Control (IOC)

Inversion of Control (IoC) is a design principle in which the control of object creation and dependency resolution is transferred from the program (developer) to a framework or container.

### Working of IOC Containers in Spring

1. The developer defines application components (beans) using annotations or configuration
2. Spring's IoC container:
   - Scans the classpath
   - Detects annotated components
   - Creates and wires dependencies automatically
3. The container manages the entire lifecycle of those objects

<img width="913" height="337" alt="image" src="https://github.com/user-attachments/assets/c24fca69-9d8f-4422-875b-1f4c26c4bf0f" />

<img width="884" height="386" alt="image" src="https://github.com/user-attachments/assets/125fc2c0-46bd-40d0-a23a-37e1f3e97eed" />

<img width="832" height="347" alt="image" src="https://github.com/user-attachments/assets/45bdaa45-b1a7-4617-9a7a-89ca6c620114" />

### Deep Explanation: How IOC Container Works Internally

The Spring IoC container follows a sophisticated process to manage beans and their dependencies. Let's break down the internal working:

#### Phase 1: Application Context Initialization

When a Spring Boot application starts, the `ApplicationContext` is created:

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        // This triggers the entire IoC container initialization
        ApplicationContext context = SpringApplication.run(Application.class, args);
    }
}
```

**Internal Process**:

1. **Context Creation**: Spring creates an `ApplicationContext` instance
2. **Configuration Loading**: Loads all `@Configuration` classes and configuration metadata
3. **Component Scanning Setup**: Prepares component scanning based on `@ComponentScan` or package structure

#### Phase 2: Component Scanning and Bean Discovery

Spring scans the classpath to discover components:

```java
@Component
public class UserService {
    // Discovered during component scanning
}

@Service
public class OrderService {
    // Discovered during component scanning
}

@Repository
public class UserRepository {
    // Discovered during component scanning
}
```

**Internal Process**:

1. **Package Scanning**: Spring scans packages specified in `@ComponentScan` or the package containing `@SpringBootApplication`
2. **Class Loading**: Loads all classes in the scanned packages
3. **Annotation Detection**: Identifies classes annotated with:
   - `@Component`
   - `@Service`
   - `@Repository`
   - `@Controller`
   - `@RestController`
   - `@Configuration`
4. **Bean Definition Creation**: Creates `BeanDefinition` objects for each discovered component

**Example of Bean Definition Creation**:

```java
// Spring internally creates something like:
BeanDefinition userServiceDefinition = new BeanDefinition();
userServiceDefinition.setBeanClassName("com.example.UserService");
userServiceDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
userServiceDefinition.setLazyInit(false);
```

#### Phase 3: Bean Definition Registration

All discovered bean definitions are registered in the container:

**Internal Process**:

1. **BeanDefinitionRegistry**: All bean definitions are stored in a registry
2. **Metadata Storage**: Spring stores metadata about each bean:
   - Class name
   - Scope (singleton, prototype, etc.)
   - Dependencies
   - Initialization and destruction methods
   - Property values

**Example**:

```java
// Spring internally maintains a registry
Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
beanDefinitions.put("userService", userServiceDefinition);
beanDefinitions.put("orderService", orderServiceDefinition);
```

#### Phase 4: Dependency Analysis and Graph Building

Spring analyzes dependencies between beans:

```java
@Service
public class OrderService {
    private final UserService userService;
    private final PaymentService paymentService;
    
    @Autowired
    public OrderService(UserService userService, PaymentService paymentService) {
        this.userService = userService;
        this.paymentService = paymentService;
    }
}
```

**Internal Process**:

1. **Dependency Detection**: Spring identifies dependencies through:
   - Constructor parameters
   - Setter methods with `@Autowired`
   - Fields with `@Autowired`
2. **Dependency Graph**: Builds a dependency graph to determine creation order
3. **Circular Dependency Detection**: Detects and handles circular dependencies

**Dependency Graph Example**:

```
OrderService
    ├── depends on: UserService
    └── depends on: PaymentService

PaymentService
    └── depends on: PaymentGateway

UserService
    └── depends on: UserRepository
```

#### Phase 5: Bean Instantiation

Spring creates bean instances in the correct order:

**Internal Process**:

1. **Ordering**: Beans are created in dependency order (dependencies first)
2. **Instantiation**: Uses reflection to create instances:
   ```java
   // Spring internally does something like:
   Class<?> beanClass = Class.forName("com.example.UserService");
   Object beanInstance = beanClass.getDeclaredConstructor().newInstance();
   ```
3. **Post-Processing**: Applies post-processors (e.g., `@Autowired` injection)

**Step-by-Step Instantiation**:

```java
// Step 1: Create UserRepository (no dependencies)
UserRepository userRepository = new UserRepository();

// Step 2: Create UserService (depends on UserRepository)
UserService userService = new UserService(userRepository);

// Step 3: Create PaymentService (no dependencies)
PaymentService paymentService = new PaymentService();

// Step 4: Create OrderService (depends on UserService and PaymentService)
OrderService orderService = new OrderService(userService, paymentService);
```

#### Phase 6: Dependency Injection

Spring injects dependencies into beans:

**Internal Process**:

1. **Constructor Injection**: Injects dependencies through constructors
2. **Setter Injection**: Calls setter methods with dependencies
3. **Field Injection**: Uses reflection to set field values

**Example of Constructor Injection**:

```java
// Spring internally does:
Constructor<?> constructor = OrderService.class.getDeclaredConstructor(
    UserService.class, 
    PaymentService.class
);
OrderService orderService = (OrderService) constructor.newInstance(
    userServiceBean, 
    paymentServiceBean
);
```

#### Phase 7: Bean Initialization

Spring initializes beans after dependency injection:

**Internal Process**:

1. **@PostConstruct Methods**: Calls methods annotated with `@PostConstruct`
2. **InitializingBean**: Calls `afterPropertiesSet()` if bean implements `InitializingBean`
3. **Custom Init Methods**: Calls custom initialization methods

**Example**:

```java
@Service
public class OrderService implements InitializingBean {
    private final UserService userService;
    
    @Autowired
    public OrderService(UserService userService) {
        this.userService = userService;
    }
    
    @PostConstruct
    public void init() {
        System.out.println("OrderService initialized");
    }
    
    @Override
    public void afterPropertiesSet() {
        System.out.println("Properties set for OrderService");
    }
}
```

#### Phase 8: Bean Registration in Container

Initialized beans are stored in the container:

**Internal Process**:

1. **Singleton Storage**: Singleton beans are stored in a cache
2. **Bean Name Registration**: Beans are registered with their names
3. **Type Registration**: Beans are also registered by their types

**Internal Storage**:

```java
// Spring internally maintains:
Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
singletonObjects.put("userService", userServiceInstance);
singletonObjects.put("orderService", orderServiceInstance);

Map<Class<?>, Object> beansByType = new HashMap<>();
beansByType.put(UserService.class, userServiceInstance);
```

#### Phase 9: Application Ready

The application is ready to use:

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(Application.class, args);
        
        // All beans are now available
        OrderService orderService = context.getBean(OrderService.class);
        orderService.processOrder(new Order());
    }
}
```

### Complete Flow Diagram

```
Application Starts
        |
        v
Spring Boot loads ApplicationContext
        |
        v
Component Scan begins
        |
        v
Spring identifies beans (@Component, @Service, @Repository)
        |
        v
Creates BeanDefinition objects
        |
        v
Registers BeanDefinitions in registry
        |
        v
Analyzes dependencies and builds dependency graph
        |
        v
Determines bean creation order
        |
        v
Instantiates beans (using reflection)
        |
        v
Injects dependencies (@Autowired, @Qualifier)
        |
        v
Calls initialization methods (@PostConstruct, afterPropertiesSet)
        |
        v
Registers beans in container (singleton cache)
        |
        v
Application is ready
```

### IOC Container Types

#### BeanFactory

Basic container with lazy initialization:

```java
BeanFactory factory = new XmlBeanFactory(new ClassPathResource("beans.xml"));
UserService userService = factory.getBean("userService", UserService.class);
```

**Characteristics**:
- Lazy initialization (beans created on demand)
- Lightweight
- Basic functionality

#### ApplicationContext

Advanced container used by Spring Boot:

```java
ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
UserService userService = context.getBean(UserService.class);
```

**Characteristics**:
- Eager initialization (all singleton beans created at startup)
- Supports AOP
- Event Handling
- Internationalization
- Autowiring
- More features than BeanFactory

### Short Example: Complete Flow

```java
// Component 1: Engine
@Component
public class Engine {
    public void start() {
        System.out.println("Engine started");
    }
}

// Component 2: Transmission
@Component
public class Transmission {
    public void shiftGear(int gear) {
        System.out.println("Shifted to gear: " + gear);
    }
}

// Service: CarService (depends on Engine and Transmission)
@Service
public class CarService {
    private final Engine engine;
    private final Transmission transmission;
    
    @Autowired
    public CarService(Engine engine, Transmission transmission) {
        this.engine = engine;
        this.transmission = transmission;
    }
    
    public void drive() {
        engine.start();
        transmission.shiftGear(1);
        System.out.println("Car is running");
    }
}

// Main Application
@SpringBootApplication
public class DemoApp {
    public static void main(String[] args) {
        // This triggers the entire IoC container initialization
        ApplicationContext context = SpringApplication.run(DemoApp.class, args);
        
        // Retrieve bean from container
        CarService car = context.getBean(CarService.class);
        car.drive();
    }
}
```

**What Happens Internally**:

1. Spring scans the package and finds `@Component` and `@Service` annotations
2. Creates `BeanDefinition` for `Engine`, `Transmission`, and `CarService`
3. Analyzes that `CarService` depends on `Engine` and `Transmission`
4. Creates `Engine` instance first (no dependencies)
5. Creates `Transmission` instance (no dependencies)
6. Creates `CarService` instance, injecting `Engine` and `Transmission`
7. Stores all beans in the container
8. Application is ready

### Benefits of IOC

- **Separation of Concerns**: Business logic is separated from object creation
- **Loose Coupling**: Components depend on abstractions, not concrete implementations
- **Testability**: Easy to mock dependencies for testing
- **Flexibility**: Can swap implementations without changing dependent code
- **Maintainability**: Centralized configuration makes changes easier

---

## Summary

Spring Boot's IoC container is a powerful mechanism that:

1. **Discovers** components through scanning
2. **Creates** bean definitions
3. **Analyzes** dependencies
4. **Instantiates** beans in the correct order
5. **Injects** dependencies automatically
6. **Manages** the entire lifecycle of beans

This inversion of control allows developers to focus on business logic while Spring handles the complexity of object creation and dependency management.
