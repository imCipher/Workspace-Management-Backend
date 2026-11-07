package com.cipher.workspace.Config;

import com.cipher.workspace.Entity.Role;
import com.cipher.workspace.Entity.User;
import com.cipher.workspace.Repository.PaymentTypeRepository;
import com.cipher.workspace.Repository.ServiceTypeRepository;
import com.cipher.workspace.Repository.UserRepository;
import com.cipher.workspace.Services.ExpenseCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ExpenseCategoryService expenseCategoryService;

    @Mock
    private ServiceTypeRepository serviceTypeRepository;

    @Mock
    private PaymentTypeRepository paymentTypeRepository;

    private DatabaseSeeder databaseSeeder;

    @BeforeEach
    void setUp() {
        databaseSeeder = new DatabaseSeeder(
                userRepository,
                passwordEncoder,
                expenseCategoryService,
                serviceTypeRepository,
                paymentTypeRepository
        );
        ReflectionTestUtils.setField(databaseSeeder, "defaultAdminUsername", "admin");
        ReflectionTestUtils.setField(databaseSeeder, "defaultAdminPassword", "Admin@12345");
        ReflectionTestUtils.setField(databaseSeeder, "defaultAdminEmail", "admin@workspace.com");
        ReflectionTestUtils.setField(databaseSeeder, "defaultAdminFullname", "Workspace Administrator");
    }

    @Test
    void run_whenNoAdminExists_provisionsAdminAndOptions() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(passwordEncoder.encode("Admin@12345")).thenReturn("encoded-password");
        when(serviceTypeRepository.count()).thenReturn(0L);
        when(paymentTypeRepository.count()).thenReturn(0L);

        databaseSeeder.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedAdmin = userCaptor.getValue();

        assertEquals("admin", savedAdmin.getUsername());
        assertEquals("encoded-password", savedAdmin.getPassword());
        assertEquals("admin@workspace.com", savedAdmin.getEmail());
        assertEquals(Role.ADMIN, savedAdmin.getRole());
        assertEquals(com.cipher.workspace.Entity.UserStatus.ACTIVE, savedAdmin.getStatus());
        assertTrue(savedAdmin.isEmailVerified());

        verify(expenseCategoryService).ensureDefaultCategories();
        verify(serviceTypeRepository).saveAll(any());
        verify(paymentTypeRepository).saveAll(any());
    }

    @Test
    void run_whenAdminExists_skipsAdminCreation() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);
        when(serviceTypeRepository.count()).thenReturn(4L);
        when(paymentTypeRepository.count()).thenReturn(4L);

        databaseSeeder.run();

        verify(userRepository, never()).save(any());
        verify(serviceTypeRepository, never()).saveAll(any());
        verify(paymentTypeRepository, never()).saveAll(any());
    }
}
