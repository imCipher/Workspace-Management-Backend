package com.cipher.workspace.Services;

import com.cipher.workspace.Config.EmailProperties;
import com.cipher.workspace.Entity.Role;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailProperties emailProperties;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        emailProperties.setFrom("noreply@workspace.com");
        emailProperties.setSenderName("Workspace Hub");
        emailProperties.setInviteUrl("http://localhost:3000/accept-invite");
        emailProperties.setResetPasswordUrl("http://localhost:3000/reset-password");
        emailProperties.setLoginUrl("http://localhost:3000/login");
        emailProperties.setTokenExpirationHours(48);

        emailService = new EmailService(mailSender, emailProperties);
    }

    @Test
    void sendInviteEmail_sendsMimeMessage() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendInviteEmail("worker@test.com", "Worker Name", Role.WORKER, "raw-token-123"));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPasswordResetEmail_sendsMimeMessage() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendPasswordResetEmail("admin@test.com", "Admin Name", "reset-token-456"));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendWelcomeEmail_sendsMimeMessage() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendWelcomeEmail("user@test.com", "username123", Role.WORKER));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendEmail_handlesMailSenderExceptionGracefully() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP connect failed"));

        // Must not throw out of sendInviteEmail (gracefully catches and logs)
        assertDoesNotThrow(() ->
                emailService.sendInviteEmail("user@test.com", "User", Role.WORKER, "tok"));
    }
}
