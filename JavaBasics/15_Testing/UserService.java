package testing;

import java.util.*;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;

class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}

class User {
    private String id;
    private String username;
    private String email;
    private LocalDate birthDate;
    private Set<String> roles;

    public User(String username, String email, LocalDate birthDate) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.email = email;
        this.birthDate = birthDate;
        this.roles = new HashSet<>();
    }

    // Getters and setters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public LocalDate getBirthDate() { return birthDate; }
    public Set<String> getRoles() { return new HashSet<>(roles); }
    
    public void addRole(String role) {
        roles.add(role);
    }

    public void removeRole(String role) {
        roles.remove(role);
    }
}

interface UserRepository {
    User save(User user);
    Optional<User> findById(String id);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    void delete(String id);
}

class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> users = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return users.values().stream()
            .filter(user -> user.getUsername().equals(username))
            .findFirst();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public void delete(String id) {
        users.remove(id);
    }
}

interface EmailService {
    void sendWelcomeEmail(User user);
    void sendRoleUpdateEmail(User user, String role, boolean added);
}

class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public User createUser(String username, String email, LocalDate birthDate) {
        validateNewUser(username, email, birthDate);
        
        User user = new User(username, email, birthDate);
        user = userRepository.save(user);
        emailService.sendWelcomeEmail(user);
        
        return user;
    }

    private void validateNewUser(String username, String email, LocalDate birthDate) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username cannot be empty");
        }

        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ValidationException("Invalid email format");
        }

        if (birthDate == null || birthDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Invalid birth date");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new ValidationException("Username already exists");
        }
    }

    public void addRole(String userId, String role) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ValidationException("User not found"));
        
        user.addRole(role);
        userRepository.save(user);
        emailService.sendRoleUpdateEmail(user, role, true);
    }

    public void removeRole(String userId, String role) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ValidationException("User not found"));
        
        user.removeRole(role);
        userRepository.save(user);
        emailService.sendRoleUpdateEmail(user, role, false);
    }

    public Optional<User> findUser(String userId) {
        return userRepository.findById(userId);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(String userId) {
        userRepository.delete(userId);
    }
}