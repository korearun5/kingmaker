package com.kore.king.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kore.king.entity.User;
import com.kore.king.entity.UserRole;
import com.kore.king.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.USER);
        testUser.setAvailablePoints(1000);
    }

    @Test
    void registerUser_WithValidData_ShouldRegisterUser() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("password");
        
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Act
        User result = userService.registerUser(newUser);

        // Assert
        assertThat(result).isEqualTo(newUser);
        verify(userRepository).save(newUser);
        verify(passwordEncoder).encode("password");
    }

    @Test
    void registerUser_WithDuplicateUsername_ShouldThrowException() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("existinguser");
        
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(newUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void registerUser_WithDuplicateEmail_ShouldThrowException() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("existing@example.com");
        
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(newUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void findByUsername_WithExistingUser_ShouldReturnUser() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findByUsername("testuser");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
    }

    @Test
    void findByUsername_WithNonExistingUser_ShouldReturnEmpty() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findByUsername("nonexistent");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findById_WithExistingUser_ShouldReturnUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
    }

    @Test
    void createAdminUser_WithValidData_ShouldCreateAdmin() {
        // Arrange
        when(userRepository.existsByUsername("adminuser")).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("adminpass")).thenReturn("encodedAdminPass");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.createAdminUser("adminuser", "admin@example.com", "adminpass");

        // Assert
        assertThat(result).isEqualTo(testUser);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void promoteToAdmin_WithExistingUser_ShouldPromoteUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // Act
        boolean result = userService.promoteToAdmin(1L);

        // Assert
        assertThat(result).isTrue();
        assertThat(testUser.getRole()).isEqualTo(UserRole.EMPLOYEE_ADMIN);
        verify(userRepository).save(testUser);
    }

    @Test
    void promoteToAdmin_WithNonExistingUser_ShouldReturnFalse() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        boolean result = userService.promoteToAdmin(999L);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void demoteFromAdmin_WithExistingAdmin_ShouldDemoteUser() {
        // Arrange
        testUser.setRole(UserRole.EMPLOYEE_ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // Act
        boolean result = userService.demoteFromAdmin(1L);

        // Assert
        assertThat(result).isTrue();
        assertThat(testUser.getRole()).isEqualTo(UserRole.USER);
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_WithValidData_ShouldChangePassword() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.matches("newPassword", "encodedPassword")).thenReturn(false);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(testUser)).thenReturn(testUser);

        // Act
        boolean result = userService.changePassword("testuser", "oldPassword", "newPassword");

        // Assert
        assertThat(result).isTrue();
        assertThat(testUser.getPassword()).isEqualTo("newEncodedPassword");
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_WithWrongCurrentPassword_ShouldReturnFalse() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // Act
        boolean result = userService.changePassword("testuser", "wrongPassword", "newPassword");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void changePassword_WithSameNewPassword_ShouldThrowException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.changePassword("testuser", "oldPassword", "oldPassword"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("must be different");
    }

    @Test
    void changePassword_WithShortNewPassword_ShouldThrowException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.matches("short", "encodedPassword")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.changePassword("testuser", "oldPassword", "short"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("at least 6 characters");
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        // Arrange
        List<User> users = List.of(testUser);
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).contains(testUser);
    }

    @Test
    void saveUser_ShouldSaveUser() {
        // Arrange
        when(userRepository.save(testUser)).thenReturn(testUser);

        // Act
        User result = userService.saveUser(testUser);

        // Assert
        assertThat(result).isEqualTo(testUser);
        verify(userRepository).save(testUser);
    }
}