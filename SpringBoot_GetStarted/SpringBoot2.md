# Spring Boot 2 - Complete In-Depth Guide

## Table of Contents
1. [Configuration](#configuration)
2. [Spring Boot Starters](#spring-boot-starters)
3. [Web Development](#web-development)
4. [Data Access](#data-access)
5. [Database Configuration](#database-configuration)
6. [Validation](#validation)
7. [Spring Boot Actuator](#spring-boot-actuator)
8. [Caching](#caching)
9. [Scheduling](#scheduling)
10. [Async Processing](#async-processing)
11. [Testing](#testing)
12. [Logging](#logging)
13. [Advanced Features](#advanced-features)

---

## Configuration

### application.properties vs application.yml

#### application.properties
- **Format**: Key-value pairs
- **Syntax**: `key=value`
- **Use Case**: Simple configurations, less readable for nested values

**Example:**
```properties
server.port=8080
spring.application.name=MyApp
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update
```

#### application.yml
- **Format**: Hierarchical, indentation-based
- **Syntax**: YAML syntax using indentation
- **Use Case**: More readable for nested/grouped configurations

**Example:**
```yaml
server:
  port: 8080
spring:
  application:
    name: MyApp
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

### External Configuration Priority

Spring Boot follows a specific order for configuration precedence (highest to lowest):

1. **Command-line arguments** (Highest priority)
   ```bash
   java -jar app.jar --server.port=9090
   ```

2. **Environment variables**
   ```bash
   export SERVER_PORT=9090
   # Or in Windows
   set SERVER_PORT=9090
   ```

3. **Profile-specific files** (e.g., `application-prod.yml`)

4. **application.properties/yml** (Default file-based config)

5. **Default values** (Lowest priority)

### Profiles

Spring Profiles allow you to define environment-specific configurations (e.g., dev, test, prod).

#### Creating Profile-Specific Files

Create separate property files for each environment:
- `application-dev.properties` or `application-dev.yml`
- `application-test.properties` or `application-test.yml`
- `application-prod.properties` or `application-prod.yml`

#### Activating Profiles

**Method 1: Via application.properties**
```properties
spring.profiles.active=dev
```

**Method 2: Via Command Line**
```bash
java -jar app.jar --spring.profiles.active=prod
```

**Method 3: Via Environment Variable**
```bash
export SPRING_PROFILES_ACTIVE=prod
```

**Method 4: Programmatically**
```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Application.class);
        app.setAdditionalProfiles("prod");
        app.run(args);
    }
}
```

#### Conditional Beans with @Profile

```java
@Configuration
@Profile("dev")
public class DevConfig {
    @Bean
    public DataSource dataSource() {
        return new H2DataSource(); // H2 In-Memory DB for Dev
    }
    
    @Bean
    public String environment() {
        return "Development Environment";
    }
}

@Configuration
@Profile("prod")
public class ProdConfig {
    @Bean
    public DataSource dataSource() {
        return new MySQLDataSource(); // MySQL DB for Production
    }
    
    @Bean
    public String environment() {
        return "Production Environment";
    }
}
```

#### Multiple Active Profiles

```properties
spring.profiles.active=dev,db-h2,cache-redis
```

### Property Injection: @Value vs @ConfigurationProperties

Spring provides two ways to inject properties into Java classes.

#### 1. @Value Annotation

- Injects individual values from properties or YAML files
- Lightweight and direct
- Supports SpEL (Spring Expression Language)
- Supports default values

**Example:**
```java
@Component
public class AppSettings {
    @Value("${server.port}")
    private int port;
    
    @Value("${custom.message:Default Message}")
    private String message; // Fallback value if not defined
    
    @Value("${app.timeout:30}")
    private int timeout;
    
    // Using SpEL
    @Value("#{systemProperties['user.name']}")
    private String systemUser;
    
    @Value("#{T(java.lang.Math).random() * 100}")
    private double randomValue;
    
    // Getters and Setters
}
```

**application.properties:**
```properties
server.port=8080
custom.message=Hello Spring Boot
```

#### 2. @ConfigurationProperties Annotation

- Maps a group of related properties into a POJO
- Cleaner for structured configs
- Supports validation with JSR-303 annotations
- Type-safe property binding

**YAML Example:**
```yaml
app:
  name: MyApp
  version: 1.0
  database:
    host: localhost
    port: 3306
    name: mydb
  features:
    enabled: true
    max-users: 100
```

**Java Class:**
```java
@Component
@ConfigurationProperties(prefix = "app")
@Validated
public class AppProperties {
    @NotBlank
    private String name;
    
    private String version;
    
    private Database database;
    
    private Features features;
    
    // Nested class
    public static class Database {
        private String host;
        private int port;
        private String name;
        
        // Getters and Setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
    public static class Features {
        private boolean enabled;
        private int maxUsers;
        
        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxUsers() { return maxUsers; }
        public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }
    }
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Database getDatabase() { return database; }
    public void setDatabase(Database database) { this.database = database; }
    public Features getFeatures() { return features; }
    public void setFeatures(Features features) { this.features = features; }
}
```

**Enable Binding (if not using @SpringBootApplication):**
```java
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {
}
```

**Usage in Service:**
```java
@Service
public class AppService {
    @Autowired
    private AppProperties appProperties;
    
    public void printConfig() {
        System.out.println("App: " + appProperties.getName());
        System.out.println("DB Host: " + appProperties.getDatabase().getHost());
    }
}
```

#### @Value vs @ConfigurationProperties Comparison

| Feature | @Value | @ConfigurationProperties |
|---------|--------|--------------------------|
| **Purpose** | Inject individual values | Bind multiple related properties |
| **Use Case** | Simple property access | Structured/grouped configuration |
| **Default Values** | Yes (`@Value("${key:default}")`) | No (use validation instead) |
| **Validation** | Limited | Strong (with JSR-303 annotations) |
| **Type Safety** | Basic | Strong |
| **Relaxed Binding** | No | Yes (kebab-case, snake_case, etc.) |
| **SpEL Support** | Yes | No |

---

## Spring Boot Starters

A Starter is a pre-defined set of dependencies bundled under a single starter module, designed to support specific application needs (e.g., web, JPA, security).

### Key Benefits
- Simplifies Maven/Gradle configuration
- Reduces boilerplate dependency management
- Ensures compatible versions for Spring Boot
- Provides auto-configuration

### Common Starters

#### spring-boot-starter-web
Used to build web applications and RESTful services using Spring MVC.

**Includes:**
- Spring MVC
- Jackson (for JSON binding)
- Embedded Tomcat (by default)
- Spring Web

**Maven Dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

**Example Controller:**
```java
@RestController
@RequestMapping("/api")
public class HelloController {
    @GetMapping("/hello")
    public String hello() {
        return "Hello Spring Boot!";
    }
}
```

#### spring-boot-starter-data-jpa
Used to enable Spring Data JPA for data access with relational databases.

**Includes:**
- Spring Data JPA
- Hibernate
- Spring ORM
- HikariCP (connection pooling)

**Maven Dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

**Example Repository:**
```java
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByName(String name);
    Optional<User> findByEmail(String email);
}
```

#### spring-boot-starter-security
Secures all endpoints by default. Provides authentication and authorization.

**Maven Dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**Example Configuration (Spring Boot 2.x):**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .formLogin()
            .and()
            .httpBasic();
    }
    
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
            .withUser("user")
            .password(passwordEncoder().encode("password"))
            .roles("USER");
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Example Configuration (Spring Boot 3.x):**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
    
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
            .username("user")
            .password("password")
            .roles("USER")
            .build();
        return new InMemoryUserDetailsManager(user);
    }
}
```

#### spring-boot-starter-test
Used to write unit and integration tests.

**Includes:**
- JUnit 5
- Mockito
- Spring Test
- AssertJ
- Hamcrest

**Maven Dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Example Test:**
```java
@SpringBootTest
class MyServiceTest {
    @Autowired
    private MyService myService;
    
    @MockBean
    private UserRepository userRepository;
    
    @Test
    void testLogic() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        assertEquals("expected", myService.doSomething());
    }
}
```

#### Other Important Starters

**spring-boot-starter-actuator**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**spring-boot-starter-cache**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

**spring-boot-starter-mail**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

**spring-boot-starter-validation**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

---

## Web Development

Spring Boot supports full-stack web development using Spring MVC, allowing you to build RESTful APIs and web applications easily.

### Creating REST APIs with @RestController

`@RestController` combines `@Controller` and `@ResponseBody`, enabling RESTful endpoints.

**Example:**
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }
    
    @GetMapping
    public List<User> getAllUsers() {
        return userService.findAll();
    }
    
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        User savedUser = userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }
    
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @Valid @RequestBody User user) {
        return userService.update(id, user);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Handling Request Parameters

Spring Boot provides several annotations to extract data from incoming HTTP requests. Here's a comparison of the main annotations:

| Annotation | Description | Example Usage |
|------------|-------------|---------------|
| **@PathVariable** | Binds a URL segment to a method parameter. Used when a value is part of the URL path itself. | For URL `/users/{id}`, bind `{id}` segment: `@PathVariable Long id` |
| **@RequestParam** | Extracts query parameters from the URL. These are typically found after `?` in the URL. | For URL `/users?role=admin`, bind query parameter: `@RequestParam String role` |
| **@RequestBody** | Binds the request's JSON/XML body to an object. Commonly used for POST or PUT requests. | For JSON POST request, map entire body: `@RequestBody User user` |

#### @PathVariable
Extracts values from the URI path.

```java
@GetMapping("/users/{id}/posts/{postId}")
public Post getUserPost(
    @PathVariable Long id,
    @PathVariable Long postId
) {
    return postService.findByUserIdAndPostId(id, postId);
}

// With custom name
@GetMapping("/users/{userId}")
public User getUser(@PathVariable("userId") Long id) {
    return userService.findById(id);
}
```

#### @RequestParam
Extracts query parameters from the request.

```java
@GetMapping("/users")
public List<User> getUsers(
    @RequestParam(required = false, defaultValue = "0") int page,
    @RequestParam(required = false, defaultValue = "10") int size,
    @RequestParam(required = false) String name
) {
    return userService.findUsers(page, size, name);
}

// Multiple values
@GetMapping("/search")
public List<User> searchUsers(@RequestParam List<String> tags) {
    return userService.findByTags(tags);
}
```

#### @RequestBody
Binds the HTTP request body to a Java object.

```java
@PostMapping("/users")
public User createUser(@Valid @RequestBody UserDto userDto) {
    return userService.create(userDto);
}
```

#### @RequestHeader
Extracts values from HTTP headers.

```java
@GetMapping("/info")
public String getInfo(@RequestHeader("User-Agent") String userAgent) {
    return "User Agent: " + userAgent;
}

@GetMapping("/auth")
public String authenticate(@RequestHeader Map<String, String> headers) {
    return headers.get("Authorization");
}
```

#### @CookieValue
Extracts values from cookies.

```java
@GetMapping("/profile")
public String getProfile(@CookieValue("sessionId") String sessionId) {
    return "Session: " + sessionId;
}
```

### ResponseEntity and Status Codes

`ResponseEntity` allows customizing the response body, headers, and HTTP status.

**Example:**
```java
@GetMapping("/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    User user = userService.findById(id)
        .orElse(null);
    
    if (user == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
    
    return ResponseEntity.ok(user);
}

@PostMapping
public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
    User savedUser = userService.save(user);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .header("Location", "/api/users/" + savedUser.getId())
        .body(savedUser);
}

// Using static methods
@GetMapping("/custom")
public ResponseEntity<Map<String, String>> customResponse() {
    Map<String, String> body = Map.of("message", "Success");
    return ResponseEntity
        .status(HttpStatus.OK)
        .header("Custom-Header", "value")
        .body(body);
}
```

### Exception Handling

#### @ControllerAdvice and @ExceptionHandler

Global exception handling using `@ControllerAdvice` and specific error response logic using `@ExceptionHandler`.

**Custom Exception:**
```java
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}

public class ValidationException extends RuntimeException {
    private Map<String, String> errors;
    
    public ValidationException(Map<String, String> errors) {
        super("Validation failed");
        this.errors = errors;
    }
    
    public Map<String, String> getErrors() {
        return errors;
    }
}
```

**Global Exception Handler:**
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
        UserNotFoundException ex
    ) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
        MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
        ValidationException ex
    ) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ex.getErrors());
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred",
            LocalDateTime.now()
        );
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }
}

// Error Response DTO
public class ErrorResponse {
    private int status;
    private String message;
    private LocalDateTime timestamp;
    
    public ErrorResponse(int status, String message, LocalDateTime timestamp) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
}
```

#### ResponseStatusException (Spring 5+)

For simpler exception handling without creating custom exception classes:

```java
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) {
    return userService.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "User not found: " + id
        ));
}
```

### Content Negotiation

Spring automatically supports content negotiation, allowing responses in different formats (e.g., JSON, XML) based on Accept headers.

**Example:**
```java
@GetMapping(
    value = "/info",
    produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
    }
)
public User getInfo() {
    return new User(1L, "Alice", "alice@example.com");
}
```

- Request with `Accept: application/json` → Returns JSON
- Request with `Accept: application/xml` → Returns XML

**Configuration:**
```properties
spring.mvc.contentnegotiation.favor-parameter=true
spring.mvc.contentnegotiation.parameter-name=format
```

Then access via: `/api/users?format=xml` or `/api/users?format=json`

### CORS Configuration

Cross-Origin Resource Sharing (CORS) allows web applications to access resources from different domains.

**Global CORS Configuration:**
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000", "http://localhost:4200")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

**Method-Level CORS:**
```java
@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/users")
public class UserController {
    // ...
}
```

**Fine-Grained Control:**
```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
```

### File Upload and Download

#### File Upload

```java
@RestController
@RequestMapping("/api/files")
public class FileController {
    
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
        @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }
        
        try {
            String fileName = file.getOriginalFilename();
            Path path = Paths.get("uploads/" + fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());
            
            return ResponseEntity.ok("File uploaded: " + fileName);
        } catch (IOException e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Upload failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/upload-multiple")
    public ResponseEntity<String> uploadMultipleFiles(
        @RequestParam("files") MultipartFile[] files
    ) {
        List<String> uploadedFiles = new ArrayList<>();
        
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    String fileName = file.getOriginalFilename();
                    Path path = Paths.get("uploads/" + fileName);
                    Files.write(path, file.getBytes());
                    uploadedFiles.add(fileName);
                } catch (IOException e) {
                    return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Upload failed: " + e.getMessage());
                }
            }
        }
        
        return ResponseEntity.ok("Uploaded: " + uploadedFiles);
    }
}
```

**Configuration:**
```properties
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

#### File Download

```java
@GetMapping("/download/{fileName}")
public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
    try {
        Path filePath = Paths.get("uploads/" + fileName);
        Resource resource = new UrlResource(filePath.toUri());
        
        if (resource.exists() && resource.isReadable()) {
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
        } else {
            return ResponseEntity.notFound().build();
        }
    } catch (Exception e) {
        return ResponseEntity.internalServerError().build();
    }
}
```

### Interceptors

Interceptors allow you to intercept HTTP requests and responses.

**Creating an Interceptor:**
```java
@Component
public class LoggingInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);
    
    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) {
        logger.info("Request: {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }
    
    @Override
    public void postHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        ModelAndView modelAndView
    ) {
        logger.info("Response Status: {}", response.getStatus());
    }
    
    @Override
    public void afterCompletion(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        Exception ex
    ) {
        if (ex != null) {
            logger.error("Exception occurred: ", ex);
        }
    }
}
```

**Registering the Interceptor:**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private LoggingInterceptor loggingInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/public/**");
    }
}
```

---

## Data Access

Spring Boot simplifies data access by providing integration with various data sources like relational databases (via JPA and JDBC) and NoSQL databases (e.g., MongoDB). It also supports robust transaction management.

### Spring Data JPA

Spring Data JPA abstracts data access layers by providing ready-to-use repositories and powerful query capabilities.

#### 1. Entity Classes (@Entity)

`@Entity` marks a Java class as a table in the database.

**Basic Entity:**
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    private LocalDateTime createdAt;
    
    // Getters and Setters
}
```

**Entity with Relationships:**
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String email;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Post> posts = new ArrayList<>();
    
    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;
    
    @ManyToMany
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
    
    // Getters and Setters
}

@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    private String content;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    // Getters and Setters
}
```

**JPA Auditing (Automatic Timestamps):**
```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    @CreatedBy
    private String createdBy;
    
    @LastModifiedBy
    private String updatedBy;
}

// Enable JPA Auditing
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

#### 2. Repositories

Spring Data provides interfaces for CRUD operations without boilerplate code.

**Basic Repository:**
```java
public interface UserRepository extends JpaRepository<User, Long> {
    // Inherits: save, findById, findAll, delete, etc.
}
```

**Query Methods:**
```java
public interface UserRepository extends JpaRepository<User, Long> {
    // Find by single field
    List<User> findByName(String name);
    Optional<User> findByEmail(String email);
    
    // Find with conditions
    List<User> findByNameContaining(String name);
    List<User> findByEmailEndingWith(String domain);
    
    // Find with multiple conditions
    List<User> findByNameAndEmail(String name, String email);
    List<User> findByNameOrEmail(String name, String email);
    
    // Find with comparison
    List<User> findByAgeGreaterThan(int age);
    List<User> findByAgeBetween(int min, int max);
    
    // Find with sorting
    List<User> findByNameOrderByCreatedAtDesc(String name);
    
    // Count
    long countByName(String name);
    
    // Exists
    boolean existsByEmail(String email);
    
    // Delete
    void deleteByEmail(String email);
}
```

**Custom Repository:**
```java
public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {
    // Standard methods
}

public interface UserRepositoryCustom {
    List<User> findActiveUsers();
    void updateUserStatus(Long id, boolean active);
}

@Repository
public class UserRepositoryImpl implements UserRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public List<User> findActiveUsers() {
        return entityManager.createQuery(
            "SELECT u FROM User u WHERE u.active = true", User.class
        ).getResultList();
    }
    
    @Override
    public void updateUserStatus(Long id, boolean active) {
        User user = entityManager.find(User.class, id);
        if (user != null) {
            user.setActive(active);
            entityManager.merge(user);
        }
    }
}
```

#### 3. Custom Queries (@Query)

Define complex queries using JPQL or native SQL.

**JPQL Query:**
```java
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.name LIKE %:name%")
    List<User> searchByName(@Param("name") String name);
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.active = true")
    Optional<User> findActiveUserByEmail(@Param("email") String email);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.department.id = :deptId")
    long countByDepartment(@Param("deptId") Long deptId);
}
```

**Native SQL Query:**
```java
@Query(
    value = "SELECT * FROM users WHERE name LIKE %:name%",
    nativeQuery = true
)
List<User> searchByNameNative(@Param("name") String name);
```

**Modifying Queries:**
```java
@Modifying
@Query("UPDATE User u SET u.active = :active WHERE u.id = :id")
@Transactional
void updateUserStatus(@Param("id") Long id, @Param("active") boolean active);

@Modifying
@Query("DELETE FROM User u WHERE u.email = :email")
@Transactional
void deleteByEmail(@Param("email") String email);
```

#### 4. Pagination and Sorting

Built-in support via `Pageable` and `Sort`.

**Pagination:**
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findByNameContaining(String name, Pageable pageable);
}

// Usage in Service
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public Page<User> getUsers(int page, int size, String name) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findByNameContaining(name, pageable);
    }
    
    public Page<User> getUsersWithSort(int page, int size, String sortBy) {
        Sort sort = Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return userRepository.findAll(pageable);
    }
}
```

**Sorting:**
```java
List<User> users = userRepository.findAll(Sort.by("name").ascending());
List<User> users = userRepository.findAll(
    Sort.by("name").ascending().and(Sort.by("createdAt").descending())
);
```

### JDBC with Spring Boot

Use `JdbcTemplate` for fine-grained SQL operations.

**Example:**
```java
@Repository
public class UserDao {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public List<User> findAll() {
        return jdbcTemplate.query(
            "SELECT * FROM users",
            (rs, rowNum) -> new User(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email")
            )
        );
    }
    
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(
                sql,
                new Object[]{id},
                (rs, rowNum) -> new User(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("email")
                )
            );
            return Optional.of(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    public void save(User user) {
        String sql = "INSERT INTO users (name, email) VALUES (?, ?)";
        jdbcTemplate.update(sql, user.getName(), user.getEmail());
    }
    
    public void update(User user) {
        String sql = "UPDATE users SET name = ?, email = ? WHERE id = ?";
        jdbcTemplate.update(sql, user.getName(), user.getEmail(), user.getId());
    }
    
    public void deleteById(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
```

**NamedParameterJdbcTemplate:**
```java
@Repository
public class UserDao {
    
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    
    public List<User> findByName(String name) {
        String sql = "SELECT * FROM users WHERE name LIKE :name";
        Map<String, Object> params = Map.of("name", "%" + name + "%");
        
        return namedParameterJdbcTemplate.query(
            sql,
            params,
            (rs, rowNum) -> new User(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email")
            )
        );
    }
}
```

### MongoDB / NoSQL

Spring Boot provides `spring-boot-starter-data-mongodb` for integrating MongoDB.

**Maven Dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

**Configuration:**
```properties
spring.data.mongodb.uri=mongodb://localhost:27017/mydb
# Or
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=mydb
```

**Document Class:**
```java
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    
    private String name;
    private double price;
    private String category;
    
    // Getters and Setters
}
```

**Repository:**
```java
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByName(String name);
    List<Product> findByCategory(String category);
    List<Product> findByPriceBetween(double min, double max);
}
```

**Usage:**
```java
@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;
    
    public Product save(Product product) {
        return productRepository.save(product);
    }
    
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }
}
```

### Transactions (@Transactional)

Ensures atomicity for multiple database operations.

**Basic Usage:**
```java
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Transactional
    public void registerUser(User user) {
        userRepository.save(user);
        // Additional DB operations - all or nothing
    }
}
```

**Transaction Attributes:**
```java
@Service
public class UserService {
    
    // Read-only transaction (optimization)
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    // Required (default) - uses existing or creates new
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateUser(User user) {
        userRepository.save(user);
    }
    
    // Requires new - always creates new transaction
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActivity(String activity) {
        // Log in separate transaction
    }
    
    // Rollback on specific exception
    @Transactional(rollbackFor = {IllegalArgumentException.class})
    public void processPayment(Payment payment) {
        // Rolls back if IllegalArgumentException occurs
    }
    
    // No rollback on specific exception
    @Transactional(noRollbackFor = {BusinessException.class})
    public void processOrder(Order order) {
        // Doesn't rollback on BusinessException
    }
    
    // Isolation level
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void updateBalance(Long userId, double amount) {
        // Uses READ_COMMITTED isolation level
    }
    
    // Timeout
    @Transactional(timeout = 30)
    public void longRunningOperation() {
        // Transaction times out after 30 seconds
    }
}
```

**Transaction Propagation Types:**
- `REQUIRED` (default): Uses existing transaction or creates new
- `REQUIRES_NEW`: Always creates new transaction
- `SUPPORTS`: Uses existing transaction if available, otherwise non-transactional
- `NOT_SUPPORTED`: Suspends current transaction if exists
- `MANDATORY`: Must have existing transaction, throws exception otherwise
- `NEVER`: Must not have transaction, throws exception if exists
- `NESTED`: Creates nested transaction if supported

---

## Database Configuration

Spring Boot offers seamless integration with relational databases such as H2, MySQL, PostgreSQL, and supports configuration for connection pooling and schema initialization through simple properties or SQL files.

### H2 In-Memory Database

The H2 database is an in-memory relational database ideal for development and testing. It runs in memory and is destroyed when the application stops.

**Add Dependency:**
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Configure (application.properties):**
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

**H2 Console:**
- Access at: `http://localhost:8080/h2-console`
- Driver Class: `org.h2.Driver`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (empty)

**File-based H2 (persistent):**
```properties
spring.datasource.url=jdbc:h2:file:./data/mydb
```

### MySQL / PostgreSQL Setup

#### MySQL Configuration

**Maven Dependency:**
```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

**application.properties:**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.format_sql=true
```

#### PostgreSQL Configuration

**Maven Dependency:**
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

**application.properties:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
```

**Hibernate DDL Auto Options:**
- `none`: No schema management
- `validate`: Validates schema, doesn't make changes
- `update`: Updates schema if needed
- `create`: Creates schema, drops on shutdown
- `create-drop`: Creates schema, drops on shutdown

### Connection Pooling (HikariCP)

Spring Boot uses HikariCP as the default connection pool for better performance.

**Default Settings (optional to override):**
```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=30000
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.pool-name=SpringBootHikariCP
spring.datasource.hikari.max-lifetime=1800000
```

**Custom HikariCP Configuration:**
```java
@Configuration
public class DatabaseConfig {
    
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource() {
        return DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .build();
    }
}
```

### Schema Initialization (schema.sql, data.sql)

Spring Boot executes these SQL files automatically on startup to initialize the database.

**Usage:**
Place these files in `src/main/resources/`:
- `schema.sql` → For creating tables
- `data.sql` → For inserting initial data

**Example: schema.sql**
```sql
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    user_id BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

**Example: data.sql**
```sql
INSERT INTO users (name, email) VALUES 
    ('John Doe', 'john@example.com'),
    ('Alice Smith', 'alice@example.com');

INSERT INTO posts (title, content, user_id) VALUES 
    ('First Post', 'Content of first post', 1),
    ('Second Post', 'Content of second post', 1);
```

**Configuration:**
```properties
spring.sql.init.mode=always
spring.sql.init.continue-on-error=false
```

**Profile-Specific SQL:**
- `schema-dev.sql` and `data-dev.sql` for dev profile
- `schema-prod.sql` and `data-prod.sql` for prod profile

---

## Validation

Spring Boot supports bean validation using JSR-380 (Jakarta Bean Validation / javax.validation). It allows automatic input validation for REST APIs and form data using annotations like `@NotNull`, `@Email`, and more.

### Bean Validation (javax.validation)

Spring Boot auto-configures bean validation when `spring-boot-starter-validation` is on the classpath.

**Add Dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### Common Validation Annotations

| Annotation | Description |
|------------|-------------|
| `@NotNull` | Field must not be null |
| `@NotBlank` | Field must not be null or empty (String) |
| `@NotEmpty` | Field must not be null or empty (Collection, Map, Array) |
| `@Email` | Must be a valid email address |
| `@Size(min, max)` | String/collection size constraints |
| `@Min(value)` | Numeric value must be >= value |
| `@Max(value)` | Numeric value must be <= value |
| `@DecimalMin(value)` | Decimal value must be >= value |
| `@DecimalMax(value)` | Decimal value must be <= value |
| `@Positive` | Must be positive (excludes zero) |
| `@PositiveOrZero` | Must be positive or zero |
| `@Negative` | Must be negative (excludes zero) |
| `@NegativeOrZero` | Must be negative or zero |
| `@Past` | Date must be in the past |
| `@PastOrPresent` | Date must be in the past or present |
| `@Future` | Date must be in the future |
| `@FutureOrPresent` | Date must be in the future or present |
| `@Pattern(regexp)` | String must match regex pattern |
| `@Digits(integer, fraction)` | Numeric value digit constraints |
| `@AssertTrue` | Boolean must be true |
| `@AssertFalse` | Boolean must be false |

**Example:**
```java
public class User {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @Min(value = 18, message = "Age must be at least 18")
    @Max(value = 120, message = "Age must be at most 120")
    private int age;
    
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;
    
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;
    
    @DecimalMin(value = "0.0", message = "Salary must be positive")
    private BigDecimal salary;
    
    // Getters and Setters
}
```

### Using @Valid and @Validated

#### @Valid

Used to trigger validation on a method parameter (commonly in controllers).

```java
@RestController
@RequestMapping("/users")
public class UserController {
    
    @PostMapping
    public ResponseEntity<String> addUser(@Valid @RequestBody User user) {
        // User is validated before this method executes
        return ResponseEntity.ok("User is valid");
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody User user
    ) {
        // Validation happens automatically
        return ResponseEntity.ok(user);
    }
}
```

Spring will automatically return a `400 Bad Request` if validation fails, along with error messages.

**Custom Error Response:**
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, Object>> handleValidationExceptions(
    MethodArgumentNotValidException ex
) {
    Map<String, Object> errors = new HashMap<>();
    List<String> errorMessages = new ArrayList<>();
    
    ex.getBindingResult().getFieldErrors().forEach(error -> {
        errorMessages.add(error.getDefaultMessage());
    });
    
    errors.put("status", HttpStatus.BAD_REQUEST.value());
    errors.put("message", "Validation failed");
    errors.put("errors", errorMessages);
    errors.put("timestamp", LocalDateTime.now());
    
    return ResponseEntity.badRequest().body(errors);
}
```

#### @Validated

Used on a class to validate method-level constraints or with groups for conditional validation.

```java
@Validated
@Service
public class PaymentService {
    
    public void processPayment(@Min(100) int amount) {
        // Only processes if amount >= 100
        // Throws ConstraintViolationException if validation fails
    }
    
    public void transfer(
        @Min(1) @Max(10000) int amount,
        @NotBlank String accountNumber
    ) {
        // Method parameters are validated
    }
}
```

**Validation Groups:**
```java
public interface CreateGroup {}
public interface UpdateGroup {}

public class User {
    @NotNull(groups = UpdateGroup.class)
    private Long id;
    
    @NotBlank(groups = {CreateGroup.class, UpdateGroup.class})
    private String name;
    
    @Email(groups = {CreateGroup.class, UpdateGroup.class})
    private String email;
}

// Usage
@PostMapping
public ResponseEntity<String> createUser(
    @Validated(CreateGroup.class) @RequestBody User user
) {
    return ResponseEntity.ok("User created");
}

@PutMapping("/{id}")
public ResponseEntity<String> updateUser(
    @PathVariable Long id,
    @Validated(UpdateGroup.class) @RequestBody User user
) {
    return ResponseEntity.ok("User updated");
}
```

### Custom Validators

You can create custom constraints using `@Constraint`.

#### Steps to Create a Custom Validator

**1. Define the annotation:**
```java
@Constraint(validatedBy = UsernameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUsername {
    String message() default "Invalid username";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    int minLength() default 5;
    int maxLength() default 20;
}
```

**2. Create the validator class:**
```java
public class UsernameValidator implements ConstraintValidator<ValidUsername, String> {
    
    private int minLength;
    private int maxLength;
    
    @Override
    public void initialize(ValidUsername constraintAnnotation) {
        this.minLength = constraintAnnotation.minLength();
        this.maxLength = constraintAnnotation.maxLength();
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Use @NotNull for null checks
        }
        
        // Custom validation logic
        boolean isValid = value.matches("^[a-zA-Z0-9_]+$") 
            && value.length() >= minLength 
            && value.length() <= maxLength;
        
        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Username must be alphanumeric with underscores, " +
                "between " + minLength + " and " + maxLength + " characters"
            ).addConstraintViolation();
        }
        
        return isValid;
    }
}
```

**3. Use the annotation:**
```java
public class User {
    @ValidUsername(minLength = 5, maxLength = 20)
    private String username;
}
```

**Complex Custom Validator Example:**
```java
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "Password must contain uppercase, lowercase, digit, and special character";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*".indexOf(ch) >= 0);
        
        return hasUpper && hasLower && hasDigit && hasSpecial && password.length() >= 8;
    }
}
```

---

## Spring Boot Actuator

Spring Boot Actuator provides production-ready features to help you monitor and manage your application.

### Setup

**Maven Dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Configuration:**
```properties
# Expose all endpoints
management.endpoints.web.exposure.include=*

# Or expose specific endpoints
management.endpoints.web.exposure.include=health,info,metrics

# Change base path
management.endpoints.web.base-path=/actuator

# Enable specific endpoints
management.endpoint.health.enabled=true
management.endpoint.info.enabled=true
```

### Common Endpoints

| Endpoint | Description | Default |
|----------|-------------|---------|
| `/actuator/health` | Application health information | Enabled |
| `/actuator/info` | Application information | Enabled |
| `/actuator/metrics` | Application metrics | Enabled |
| `/actuator/env` | Environment properties | Disabled |
| `/actuator/beans` | Spring beans | Disabled |
| `/actuator/configprops` | Configuration properties | Disabled |
| `/actuator/mappings` | Request mappings | Disabled |
| `/actuator/loggers` | Logger configuration | Disabled |
| `/actuator/threaddump` | Thread dump | Disabled |
| `/actuator/heapdump` | Heap dump | Disabled |

### Health Endpoint

**Basic Configuration:**
```properties
management.endpoint.health.show-details=always
management.health.defaults.enabled=true
```

**Custom Health Indicator:**
```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // Check custom condition
        boolean isHealthy = checkCustomCondition();
        
        if (isHealthy) {
            return Health.up()
                .withDetail("status", "Service is running")
                .withDetail("timestamp", LocalDateTime.now())
                .build();
        } else {
            return Health.down()
                .withDetail("status", "Service is down")
                .withDetail("error", "Custom error message")
                .build();
        }
    }
    
    private boolean checkCustomCondition() {
        // Your custom health check logic
        return true;
    }
}
```

**Database Health Check:**
```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return Health.up()
                    .withDetail("database", "Available")
                    .build();
            }
        } catch (SQLException e) {
            return Health.down()
                .withDetail("database", "Unavailable")
                .withException(e)
                .build();
        }
        return Health.down().build();
    }
}
```

### Info Endpoint

**Configuration:**
```properties
info.app.name=My Application
info.app.version=1.0.0
info.app.description=Spring Boot Application
info.java.version=${java.version}
```

**Programmatic Info:**
```java
@Component
public class CustomInfoContributor implements InfoContributor {
    
    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("custom", Map.of(
            "author", "Your Name",
            "build-time", LocalDateTime.now().toString()
        ));
    }
}
```

### Metrics

**Custom Metrics:**
```java
@Service
public class UserService {
    
    private final Counter userCreationCounter;
    private final Timer userCreationTimer;
    
    public UserService(MeterRegistry meterRegistry) {
        this.userCreationCounter = Counter.builder("user.creation.count")
            .description("Number of users created")
            .register(meterRegistry);
        
        this.userCreationTimer = Timer.builder("user.creation.time")
            .description("Time taken to create user")
            .register(meterRegistry);
    }
    
    public User createUser(User user) {
        return userCreationTimer.recordCallable(() -> {
            userCreationCounter.increment();
            // User creation logic
            return userRepository.save(user);
        });
    }
}
```

---

## Caching

Spring Boot provides caching abstraction to improve application performance.

### Setup

**Maven Dependency:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

**Enable Caching:**
```java
@SpringBootApplication
@EnableCaching
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### Cache Providers

#### Simple Cache (Default - In-Memory)
```properties
spring.cache.type=simple
```

#### Caffeine Cache
```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

```properties
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=5m
```

#### Redis Cache
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```properties
spring.cache.type=redis
spring.redis.host=localhost
spring.redis.port=6379
```

### Using Caching

#### @Cacheable

Caches the result of a method.

```java
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#id")
    public User findById(Long id) {
        // This method result will be cached
        return userRepository.findById(id).orElse(null);
    }
    
    @Cacheable(value = "users", key = "#name")
    public List<User> findByName(String name) {
        return userRepository.findByName(name);
    }
    
    @Cacheable(value = "users", condition = "#id > 10")
    public User findByIdConditional(Long id) {
        // Only cache if id > 10
        return userRepository.findById(id).orElse(null);
    }
}
```

#### @CacheEvict

Removes entries from cache.

```java
@CacheEvict(value = "users", key = "#id")
public void deleteUser(Long id) {
    userRepository.deleteById(id);
}

@CacheEvict(value = "users", allEntries = true)
public void clearAllUsersCache() {
    // Clears all entries in "users" cache
}
```

#### @CachePut

Updates cache with new value.

```java
@CachePut(value = "users", key = "#user.id")
public User updateUser(User user) {
    return userRepository.save(user);
}
```

#### @Caching

Multiple cache operations.

```java
@Caching(
    evict = {
        @CacheEvict(value = "users", key = "#user.id"),
        @CacheEvict(value = "userList", allEntries = true)
    },
    put = {
        @CachePut(value = "users", key = "#user.id")
    }
)
public User updateUser(User user) {
    return userRepository.save(user);
}
```

**Cache Configuration:**
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache("users"),
            new ConcurrentMapCache("products"),
            new ConcurrentMapCache("orders")
        ));
        return cacheManager;
    }
}
```

**Caffeine Cache Configuration:**
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats());
        return cacheManager;
    }
}
```

**Redis Cache Configuration:**
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
```

---

## Scheduling

Spring Boot provides support for task scheduling using `@Scheduled` annotation.

### Setup

**Enable Scheduling:**
```java
@SpringBootApplication
@EnableScheduling
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### Fixed Rate Scheduling

Executes at fixed intervals regardless of execution time.

```java
@Component
public class ScheduledTasks {
    
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void reportCurrentTime() {
        System.out.println("Current time: " + LocalDateTime.now());
    }
    
    @Scheduled(fixedRate = 60000, initialDelay = 10000) // Every minute, start after 10 seconds
    public void processData() {
        // Process data every minute
    }
}
```

### Fixed Delay Scheduling

Executes with a fixed delay between the end of one execution and the start of the next.

```java
@Component
public class ScheduledTasks {
    
    @Scheduled(fixedDelay = 5000) // 5 seconds after previous execution completes
    public void cleanupTask() {
        // Cleanup task
    }
}
```

### Cron Expression Scheduling

Schedule tasks using cron expressions.

```java
@Component
public class ScheduledTasks {
    
    @Scheduled(cron = "0 0 12 * * ?") // Every day at noon
    public void dailyReport() {
        // Generate daily report
    }
    
    @Scheduled(cron = "0 0 0 * * MON") // Every Monday at midnight
    public void weeklyReport() {
        // Generate weekly report
    }
    
    @Scheduled(cron = "0 */15 * * * ?") // Every 15 minutes
    public void checkStatus() {
        // Check system status
    }
    
    @Scheduled(cron = "0 0 2 1 * ?") // First day of every month at 2 AM
    public void monthlyBackup() {
        // Monthly backup
    }
}
```

**Cron Expression Format:**
```
┌───────────── second (0-59)
│ ┌───────────── minute (0-59)
│ │ ┌───────────── hour (0-23)
│ │ │ ┌───────────── day of month (1-31)
│ │ │ │ ┌───────────── month (1-12)
│ │ │ │ │ ┌───────────── day of week (0-7) (Sunday = 0 or 7)
│ │ │ │ │ │
* * * * * *
```

**Common Cron Examples:**
- `0 0 * * * ?` - Every hour
- `0 0 0 * * ?` - Every day at midnight
- `0 0 12 * * ?` - Every day at noon
- `0 0 0 ? * MON` - Every Monday at midnight
- `0 0 0 1 * ?` - First day of every month
- `0 */30 * * * ?` - Every 30 minutes

### Task Scheduling with Configuration

```java
@Component
public class ScheduledTasks {
    
    @Scheduled(cron = "${app.schedule.daily-report:0 0 12 * * ?}")
    public void dailyReport() {
        // Uses cron from properties, defaults to noon if not set
    }
}
```

**application.properties:**
```properties
app.schedule.daily-report=0 0 12 * * ?
app.schedule.cleanup=0 0 2 * * ?
```

### Conditional Scheduling

```java
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = false)
public class ScheduledTasks {
    
    @Scheduled(fixedRate = 5000)
    public void scheduledTask() {
        // Only runs if app.scheduling.enabled=true
    }
}
```

### Async Scheduling

For long-running scheduled tasks, use `@Async`:

```java
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
}

@Component
public class ScheduledTasks {
    
    @Async
    @Scheduled(fixedRate = 5000)
    public void asyncScheduledTask() {
        // Runs asynchronously
    }
}
```

---

## Async Processing

Spring Boot supports asynchronous method execution using `@Async` annotation.

### Setup

**Enable Async Processing:**
```java
@SpringBootApplication
@EnableAsync
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### Basic Async Method

```java
@Service
public class AsyncService {
    
    @Async
    public CompletableFuture<String> processAsync(String data) {
        // Simulate long-running task
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return CompletableFuture.completedFuture("Processed: " + data);
    }
    
    @Async
    public void processAsyncVoid(String data) {
        // Fire and forget
        System.out.println("Processing: " + data);
    }
}
```

**Usage:**
```java
@RestController
@RequestMapping("/api")
public class AsyncController {
    
    @Autowired
    private AsyncService asyncService;
    
    @GetMapping("/process")
    public ResponseEntity<String> process() {
        CompletableFuture<String> future = asyncService.processAsync("data");
        return ResponseEntity.ok("Processing started");
    }
    
    @GetMapping("/process-wait")
    public ResponseEntity<String> processAndWait() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = asyncService.processAsync("data");
        String result = future.get(); // Wait for completion
        return ResponseEntity.ok(result);
    }
}
```

### Custom Executor Configuration

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
    
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }
}

public class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
    
    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        System.err.println("Exception in async method: " + method.getName());
        System.err.println("Exception: " + ex.getMessage());
    }
}
```

**Using Custom Executor:**
```java
@Service
public class AsyncService {
    
    @Async("taskExecutor")
    public CompletableFuture<String> processWithCustomExecutor(String data) {
        return CompletableFuture.completedFuture("Processed: " + data);
    }
}
```

### Multiple Executors

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("email-");
        executor.initialize();
        return executor;
    }
    
    @Bean(name = "reportExecutor")
    public Executor reportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("report-");
        executor.initialize();
        return executor;
    }
}

@Service
public class AsyncService {
    
    @Async("emailExecutor")
    public void sendEmail(String to, String subject) {
        // Send email
    }
    
    @Async("reportExecutor")
    public void generateReport(String reportType) {
        // Generate report
    }
}
```

### Exception Handling in Async Methods

```java
@Service
public class AsyncService {
    
    @Async
    public CompletableFuture<String> processWithException(String data) {
        try {
            // Process data
            return CompletableFuture.completedFuture("Success");
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Async
    public CompletableFuture<String> processWithExceptionHandler(String data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        return CompletableFuture.completedFuture("Processed: " + data);
    }
}

@ControllerAdvice
public class AsyncExceptionHandler {
    
    @ExceptionHandler(IllegalArgumentException.class)
    public void handleAsyncException(IllegalArgumentException ex) {
        System.err.println("Async exception: " + ex.getMessage());
    }
}
```

---

## Testing

Spring Boot provides comprehensive testing support with `spring-boot-starter-test`.

### Unit Testing

**Simple Unit Test:**
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void testFindById() {
        // Given
        User user = new User(1L, "John", "john@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        // When
        Optional<User> result = userService.findById(1L);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals("John", result.get().getName());
        verify(userRepository).findById(1L);
    }
}
```

### Integration Testing

**Spring Boot Test:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void testGetUser() throws Exception {
        // Setup
        User user = new User(1L, "John", "john@example.com");
        userRepository.save(user);
        
        // Execute and Verify
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("John"))
            .andExpect(jsonPath("$.email").value("john@example.com"));
    }
    
    @Test
    void testCreateUser() throws Exception {
        String userJson = """
            {
                "name": "Alice",
                "email": "alice@example.com"
            }
            """;
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Alice"));
    }
}
```

### Web Layer Testing

```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    void testGetUser() throws Exception {
        User user = new User(1L, "John", "john@example.com");
        when(userService.findById(1L)).thenReturn(Optional.of(user));
        
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("John"));
    }
}
```

### Data Layer Testing

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void testFindByEmail() {
        // Given
        User user = new User("John", "john@example.com");
        entityManager.persistAndFlush(user);
        
        // When
        Optional<User> found = userRepository.findByEmail("john@example.com");
        
        // Then
        assertTrue(found.isPresent());
        assertEquals("John", found.get().getName());
    }
}
```

### Testing with TestContainers

```java
@SpringBootTest
@Testcontainers
class UserServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private UserRepository userRepository;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Test
    void testSaveUser() {
        User user = new User("John", "john@example.com");
        User saved = userRepository.save(user);
        
        assertNotNull(saved.getId());
        assertEquals("John", saved.getName());
    }
}
```

### MockMvc Advanced Usage

```java
@SpringBootTest
@AutoConfigureMockMvc
class AdvancedMockMvcTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testWithAuthentication() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer token123"))
            .andExpect(status().isOk());
    }
    
    @Test
    void testWithCookies() throws Exception {
        mockMvc.perform(get("/api/profile")
                .cookie(new Cookie("sessionId", "abc123")))
            .andExpect(status().isOk());
    }
    
    @Test
    void testFileUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.txt", "text/plain", "content".getBytes()
        );
        
        mockMvc.perform(multipart("/api/files/upload")
                .file(file))
            .andExpect(status().isOk());
    }
}
```

---

## Logging

Spring Boot uses Logback by default and provides easy configuration for logging.

### Default Logging

Spring Boot configures logging automatically. Use SLF4J API:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    public void processUser(User user) {
        logger.info("Processing user: {}", user.getName());
        logger.debug("User details: {}", user);
        logger.error("Error processing user", exception);
    }
}
```

### Logging Configuration

**application.properties:**
```properties
# Root logging level
logging.level.root=INFO

# Package-specific logging
logging.level.com.example=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG

# Log file configuration
logging.file.name=application.log
logging.file.path=/var/log

# Log pattern
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# Log file size and rotation
logging.file.max-size=10MB
logging.file.max-history=30
```

**application.yml:**
```yaml
logging:
  level:
    root: INFO
    com.example: DEBUG
    org.springframework.web: DEBUG
  file:
    name: application.log
    path: /var/log
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 30
```

### Custom Logback Configuration

**logback-spring.xml:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <springProfile name="dev">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>
    
    <springProfile name="prod">
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/application.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/application-%d{yyyy-MM-dd}.log</fileNamePattern>
                <maxHistory>30</maxHistory>
            </rollingPolicy>
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>
</configuration>
```

### Structured Logging (JSON)

**For production:**
```xml
<appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/application.json</file>
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
        <providers>
            <timestamp/>
            <version/>
            <logLevel/>
            <message/>
            <mdc/>
            <stackTrace/>
        </providers>
    </encoder>
</appender>
```

---

## Advanced Features

### Command Line Runner and Application Runner

Execute code after application startup:

```java
@Component
public class StartupRunner implements CommandLineRunner {
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("Application started with arguments: " + Arrays.toString(args));
        // Initialize data, start background processes, etc.
    }
}

@Component
@Order(1)
public class ApplicationStartupRunner implements ApplicationRunner {
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("Application started");
        System.out.println("Non-option args: " + args.getNonOptionArgs());
        System.out.println("Option names: " + args.getOptionNames());
    }
}
```

### Application Events

```java
@Component
public class UserEventListener {
    
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        System.out.println("User created: " + event.getUser().getName());
    }
    
    @Async
    @EventListener
    public void handleUserUpdated(UserUpdatedEvent event) {
        // Async event handling
    }
}

@Service
public class UserService {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public User createUser(User user) {
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserCreatedEvent(saved));
        return saved;
    }
}

public class UserCreatedEvent {
    private final User user;
    
    public UserCreatedEvent(User user) {
        this.user = user;
    }
    
    public User getUser() {
        return user;
    }
}
```

### Conditional Beans

```java
@Configuration
public class ConditionalConfig {
    
    @Bean
    @ConditionalOnProperty(name = "feature.cache.enabled", havingValue = "true")
    public CacheManager cacheManager() {
        return new SimpleCacheManager();
    }
    
    @Bean
    @ConditionalOnClass(name = "com.example.ExternalService")
    public ExternalService externalService() {
        return new ExternalService();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public DefaultService defaultService() {
        return new DefaultService();
    }
    
    @Bean
    @ConditionalOnExpression("${app.feature.enabled:false} == true")
    public FeatureService featureService() {
        return new FeatureService();
    }
}
```

### Custom Auto-Configuration

```java
@Configuration
@ConditionalOnClass(MyService.class)
@EnableConfigurationProperties(MyProperties.class)
public class MyAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public MyService myService(MyProperties properties) {
        return new MyService(properties);
    }
}

@ConfigurationProperties(prefix = "my")
public class MyProperties {
    private String name;
    private int timeout;
    // Getters and Setters
}

// META-INF/spring.factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.MyAutoConfiguration
```

### WebSocket Support

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new MyWebSocketHandler(), "/ws")
            .setAllowedOrigins("*");
    }
}

@Component
public class MyWebSocketHandler extends TextWebSocketHandler {
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Connection established: " + session.getId());
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        System.out.println("Received: " + message.getPayload());
        try {
            session.sendMessage(new TextMessage("Echo: " + message.getPayload()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### Internationalization (i18n)

```java
@Configuration
public class I18nConfig implements WebMvcConfigurer {
    
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(Locale.US);
        return resolver;
    }
    
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
    
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }
}

@RestController
public class I18nController {
    
    @Autowired
    private MessageSource messageSource;
    
    @GetMapping("/greeting")
    public String greeting(@RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        return messageSource.getMessage("greeting", null, locale);
    }
}
```

**messages.properties:**
```properties
greeting=Hello
welcome=Welcome
```

**messages_fr.properties:**
```properties
greeting=Bonjour
welcome=Bienvenue
```

### Custom Error Pages

```java
@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {
    
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "error-404";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                return "error-500";
            }
        }
        return "error";
    }
}
```

### Health Check Customization

```java
@Component
public class DatabaseHealthIndicator extends AbstractHealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                builder.up()
                    .withDetail("database", "Available")
                    .withDetail("validationQuery", "isValid()");
            } else {
                builder.down()
                    .withDetail("database", "Unavailable");
            }
        }
    }
}
```

---

## Conclusion

This guide covers the essential features of Spring Boot 2.x. For production applications, always consider:

- **Security**: Implement proper authentication and authorization
- **Monitoring**: Use Actuator endpoints for health checks and metrics
- **Error Handling**: Implement comprehensive exception handling
- **Testing**: Write unit and integration tests
- **Documentation**: Use Swagger/OpenAPI for API documentation
- **Performance**: Optimize database queries, use caching, and connection pooling
- **Logging**: Implement proper logging for debugging and monitoring

For more information, visit the [Spring Boot Official Documentation](https://spring.io/projects/spring-boot).
