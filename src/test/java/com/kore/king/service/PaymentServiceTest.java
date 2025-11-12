package com.kore.king.service;

import com.kore.king.entity.PaymentRequest;
import com.kore.king.entity.PaymentStatus;
import com.kore.king.entity.PaymentMethod;
import com.kore.king.entity.User;
import com.kore.king.repository.PaymentRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRequestRepository paymentRequestRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private UserService userService;

    @Mock
    private MultipartFile screenshot;

    @InjectMocks
    private PaymentService paymentService;

    private User testUser;
    private PaymentRequest testPaymentRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setAvailablePoints(1000);

        testPaymentRequest = new PaymentRequest();
        testPaymentRequest.setId(1L);
        testPaymentRequest.setUser(testUser);
        testPaymentRequest.setAmount(BigDecimal.valueOf(1000));
        testPaymentRequest.setPaymentMethod(PaymentMethod.MANUAL_UPI);
        testPaymentRequest.setStatus(PaymentStatus.PENDING);
        testPaymentRequest.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createPaymentRequest_WithValidData_ShouldCreateRequest() {
        // Arrange
        when(screenshot.isEmpty()).thenReturn(false);
        when(fileStorageService.storeFile(screenshot)).thenReturn("screenshot.jpg");
        when(paymentRequestRepository.save(any(PaymentRequest.class))).thenReturn(testPaymentRequest);

        // Act
        PaymentRequest result = paymentService.createPaymentRequest(testPaymentRequest, testUser, screenshot);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getScreenshotPath()).isEqualTo("screenshot.jpg");
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRequestRepository).save(any(PaymentRequest.class));
    }

    @Test
    void createPaymentRequest_WithInvalidAmount_ShouldThrowException() {
        // Arrange
        testPaymentRequest.setAmount(BigDecimal.ZERO);

        // Act & Assert
        assertThatThrownBy(() -> paymentService.createPaymentRequest(testPaymentRequest, testUser, screenshot))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Amount must be greater than zero");
    }

    @Test
    void createPaymentRequest_WithEmptyFile_ShouldThrowException() {
        // Arrange
        when(screenshot.isEmpty()).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> paymentService.createPaymentRequest(testPaymentRequest, testUser, screenshot))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Screenshot is required");
    }

    @Test
    void approvePaymentRequest_WithPendingRequest_ShouldApprove() {
        // Arrange
        when(paymentRequestRepository.findById(1L)).thenReturn(Optional.of(testPaymentRequest));
        when(userService.saveUser(testUser)).thenReturn(testUser);
        when(paymentRequestRepository.save(any(PaymentRequest.class))).thenReturn(testPaymentRequest);

        // Act
        PaymentRequest result = paymentService.approvePaymentRequest(1L, "admin", "TXN123");

        // Assert
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.getTransactionId()).isEqualTo("TXN123");
        assertThat(testUser.getAvailablePoints()).isEqualTo(2000); // 1000 initial + 1000 from payment
        verify(paymentRequestRepository).save(testPaymentRequest);
    }

    @Test
    void approvePaymentRequest_WithNonPendingRequest_ShouldThrowException() {
        // Arrange
        testPaymentRequest.setStatus(PaymentStatus.APPROVED);
        when(paymentRequestRepository.findById(1L)).thenReturn(Optional.of(testPaymentRequest));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.approvePaymentRequest(1L, "admin", "TXN123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not pending");
    }

    @Test
    void rejectPaymentRequest_WithPendingRequest_ShouldReject() {
        // Arrange
        when(paymentRequestRepository.findById(1L)).thenReturn(Optional.of(testPaymentRequest));
        when(paymentRequestRepository.save(any(PaymentRequest.class))).thenReturn(testPaymentRequest);

        // Act
        PaymentRequest result = paymentService.rejectPaymentRequest(1L, "admin", "Invalid screenshot");

        // Assert
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(result.getNotes()).contains("Invalid screenshot");
        verify(paymentRequestRepository).save(testPaymentRequest);
    }

    @Test
    void getUserPaymentRequests_ShouldReturnUserRequests() {
        // Arrange
        List<PaymentRequest> requests = List.of(testPaymentRequest);
        when(paymentRequestRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(requests);

        // Act
        List<PaymentRequest> result = paymentService.getUserPaymentRequests(1L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).contains(testPaymentRequest);
    }

    @Test
    void getPendingPaymentRequests_ShouldReturnPendingRequests() {
        // Arrange
        List<PaymentRequest> requests = List.of(testPaymentRequest);
        when(paymentRequestRepository.findByStatusOrderByCreatedAtDesc(PaymentStatus.PENDING)).thenReturn(requests);

        // Act
        List<PaymentRequest> result = paymentService.getPendingPaymentRequests();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).contains(testPaymentRequest);
    }

    @Test
    void findById_WithExistingRequest_ShouldReturnRequest() {
        // Arrange
        when(paymentRequestRepository.findById(1L)).thenReturn(Optional.of(testPaymentRequest));

        // Act
        Optional<PaymentRequest> result = paymentService.findById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testPaymentRequest);
    }
}