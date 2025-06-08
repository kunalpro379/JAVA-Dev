package testing;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(
            "testuser",
            "test@example.com",
            LocalDate.of(1990, 1, 1)
        );
    }

    @Nested
    @DisplayName("User Creation Tests")
    class UserCreationTests {
        @Test
        @DisplayName("Should create user successfully")
        void shouldCreateUserSuccessfully() {
            // Arrange
            when(userRepository.findByUsername(testUser.getUsername()))
                .thenReturn(Optional.empty());
            when(userRepository.save(any(User.class)))
                .thenReturn(testUser);

            // Act
            User createdUser = userService.createUser(
                testUser.getUsername(),
                testUser.getEmail(),
                testUser.getBirthDate()
            );

            // Assert
            assertNotNull(createdUser);
            assertEquals(testUser.getUsername(), createdUser.getUsername());
            assertEquals(testUser.getEmail(), createdUser.getEmail());
            
            // Verify interactions
            verify(userRepository).findByUsername(testUser.getUsername());
            verify(userRepository).save(any(User.class));
            verify(emailService).sendWelcomeEmail(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception for duplicate username")
        void shouldThrowExceptionForDuplicateUsername() {
            // Arrange
            when(userRepository.findByUsername(testUser.getUsername()))
                .thenReturn(Optional.of(testUser));

            // Act & Assert
            ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.createUser(
                    testUser.getUsername(),
                    testUser.getEmail(),
                    testUser.getBirthDate()
                )
            );

            assertEquals("Username already exists", exception.getMessage());
            verify(userRepository).findByUsername(testUser.getUsername());
            verify(userRepository, never()).save(any(User.class));
            verify(emailService, never()).sendWelcomeEmail(any(User.class));
        }

        @ParameterizedTest
        @DisplayName("Should validate user input")
        @CsvSource({
            ", test@example.com, 1990-01-01, Username cannot be empty",
            "'', test@example.com, 1990-01-01, Username cannot be empty",
            "testuser, invalid-email, 1990-01-01, Invalid email format",
            "testuser, test@example.com, 2025-01-01, Invalid birth date"
        })
        void shouldValidateUserInput(String username, String email, 
                                   LocalDate birthDate, String expectedMessage) {
            ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.createUser(username, email, birthDate)
            );

            assertEquals(expectedMessage, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Role Management Tests")
    class RoleManagementTests {
        @Test
        @DisplayName("Should add role successfully")
        void shouldAddRoleSuccessfully() {
            // Arrange
            String role = "ADMIN";
            when(userRepository.findById(testUser.getId()))
                .thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class)))
                .thenReturn(testUser);

            // Act
            userService.addRole(testUser.getId(), role);

            // Assert
            assertTrue(testUser.getRoles().contains(role));
            verify(userRepository).save(testUser);
            verify(emailService).sendRoleUpdateEmail(testUser, role, true);
        }

        @Test
        @DisplayName("Should remove role successfully")
        void shouldRemoveRoleSuccessfully() {
            // Arrange
            String role = "ADMIN";
            testUser.addRole(role);
            when(userRepository.findById(testUser.getId()))
                .thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class)))
                .thenReturn(testUser);

            // Act
            userService.removeRole(testUser.getId(), role);

            // Assert
            assertFalse(testUser.getRoles().contains(role));
            verify(userRepository).save(testUser);
            verify(emailService).sendRoleUpdateEmail(testUser, role, false);
        }

        @Test
        @DisplayName("Should throw exception when user not found for role operations")
        void shouldThrowExceptionWhenUserNotFoundForRoleOperations() {
            // Arrange
            String userId = "nonexistent";
            when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

            // Act & Assert
            ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.addRole(userId, "ADMIN")
            );

            assertEquals("User not found", exception.getMessage());
            verify(userRepository).findById(userId);
            verify(userRepository, never()).save(any(User.class));
            verify(emailService, never())
                .sendRoleUpdateEmail(any(User.class), anyString(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("User Query Tests")
    class UserQueryTests {
        @Test
        @DisplayName("Should find user by ID")
        void shouldFindUserById() {
            // Arrange
            when(userRepository.findById(testUser.getId()))
                .thenReturn(Optional.of(testUser));

            // Act
            Optional<User> foundUser = userService.findUser(testUser.getId());

            // Assert
            assertTrue(foundUser.isPresent());
            assertEquals(testUser.getId(), foundUser.get().getId());
        }

        @Test
        @DisplayName("Should return empty when user not found")
        void shouldReturnEmptyWhenUserNotFound() {
            // Arrange
            String userId = "nonexistent";
            when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

            // Act
            Optional<User> foundUser = userService.findUser(userId);

            // Assert
            assertTrue(foundUser.isEmpty());
        }
    }

    @Test
    @DisplayName("Should delete user")
    void shouldDeleteUser() {
        // Arrange
        String userId = "test-id";

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(userRepository).delete(userId);
    }
}

// Integration test class
@Tag("integration")
class UserServiceIntegrationTest {
    private UserRepository userRepository;
    private EmailService emailService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        emailService = mock(EmailService.class); // Mock email service for integration tests
        userService = new UserService(userRepository, emailService);
    }

    @Test
    @DisplayName("Should perform full user lifecycle")
    void shouldPerformFullUserLifecycle() {
        // Create user
        User user = userService.createUser(
            "integrationtest",
            "integration@test.com",
            LocalDate.of(1990, 1, 1)
        );
        assertNotNull(user.getId());

        // Add role
        userService.addRole(user.getId(), "ADMIN");
        Optional<User> foundUser = userService.findUser(user.getId());
        assertTrue(foundUser.isPresent());
        assertTrue(foundUser.get().getRoles().contains("ADMIN"));

        // Remove role
        userService.removeRole(user.getId(), "ADMIN");
        foundUser = userService.findUser(user.getId());
        assertTrue(foundUser.isPresent());
        assertFalse(foundUser.get().getRoles().contains("ADMIN"));

        // Delete user
        userService.deleteUser(user.getId());
        assertTrue(userService.findUser(user.getId()).isEmpty());
    }

    @Test
    @DisplayName("Should prevent duplicate usernames")
    void shouldPreventDuplicateUsernames() {
        // Create first user
        userService.createUser(
            "duplicate",
            "first@test.com",
            LocalDate.of(1990, 1, 1)
        );

        // Try to create second user with same username
        assertThrows(ValidationException.class, () ->
            userService.createUser(
                "duplicate",
                "second@test.com",
                LocalDate.of(1990, 1, 1)
            )
        );
    }
}