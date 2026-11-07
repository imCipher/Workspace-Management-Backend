package com.cipher.workspace.Services;

import com.cipher.workspace.Config.EmailProperties;
import com.cipher.workspace.Entity.Role;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    public EmailService(JavaMailSender mailSender, EmailProperties emailProperties) {
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
    }

    @Async
    public void sendInviteEmail(String toEmail, String recipientName, Role role, String rawToken) {
        String inviteBaseUrl = emailProperties.getInviteUrl();
        String separator = inviteBaseUrl.contains("?") ? "&" : "?";
        String inviteUrl = inviteBaseUrl + separator + "token=" + rawToken;

        String name = (recipientName != null && !recipientName.isBlank()) ? recipientName : "there";
        String roleName = role != null ? role.name() : "Staff";
        String subject = "You're Invited to Join " + emailProperties.getSenderName() + " as " + roleName;
        String htmlContent = buildInviteEmailHtml(name, roleName, inviteUrl, emailProperties.getTokenExpirationHours());

        sendMimeEmail(toEmail, subject, htmlContent, "invite email");
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String recipientName, String rawToken) {
        String resetBaseUrl = emailProperties.getResetPasswordUrl();
        String separator = resetBaseUrl.contains("?") ? "&" : "?";
        String resetUrl = resetBaseUrl + separator + "token=" + rawToken;

        String name = (recipientName != null && !recipientName.isBlank()) ? recipientName : "there";
        String subject = "Reset Your Password - " + emailProperties.getSenderName();
        String htmlContent = buildPasswordResetEmailHtml(name, resetUrl);

        sendMimeEmail(toEmail, subject, htmlContent, "password reset email");
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String username, Role role) {
        String displayName = (username != null && !username.isBlank()) ? username : "there";
        String loginUrl = emailProperties.getLoginUrl();
        String subject = "Welcome to " + emailProperties.getSenderName() + "! Your Account is Verified";
        String htmlContent = buildWelcomeEmailHtml(displayName, role, loginUrl);

        sendMimeEmail(toEmail, subject, htmlContent, "welcome email");
    }



    private void sendMimeEmail(String toEmail, String subject, String htmlContent, String emailType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailProperties.getFrom(), emailProperties.getSenderName());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("{} sent successfully to: {}", emailType, toEmail);
        } catch (Exception e) {
            log.error("Failed to send {} to {}: {}", emailType, toEmail, e.getMessage());
        }
    }

    private String buildInviteEmailHtml(String name, String roleName, String inviteUrl, int expirationHours) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Team Invitation</title>
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 0; color: #1e293b; }
                        .container { max-width: 580px; margin: 40px auto; background: #ffffff; border-radius: 8px; overflow: hidden; border: 1px solid #e2e8f0; }
                        .header { background: #0f172a; color: #ffffff; padding: 24px; text-align: center; }
                        .header h1 { margin: 0; font-size: 20px; font-weight: 600; }
                        .content { padding: 32px 28px; line-height: 1.6; }
                        .btn-box { text-align: center; margin: 28px 0; }
                        .btn { display: inline-block; background: #2563eb; color: #ffffff !important; text-decoration: none; padding: 12px 28px; border-radius: 6px; font-weight: 600; font-size: 15px; }
                        .footer { background: #f8fafc; padding: 16px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #e2e8f0; }
                        .link-box { word-break: break-all; font-size: 12px; color: #2563eb; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>%s</h1>
                        </div>
                        <div class="content">
                            <p>Hi <strong>%s</strong>,</p>
                            <p>You have been invited to join the team as a <strong>%s</strong>. Click the button below to choose your unique username, set your password, and activate your account:</p>
                            <div class="btn-box">
                                <a href="%s" class="btn" target="_blank">Accept Invitation & Set Password</a>
                            </div>
                            <p>This invitation link will expire in <strong>%d hours</strong>.</p>
                            <p class="link-box"><a href="%s">%s</a></p>
                        </div>
                        <div class="footer">
                            <p>&copy; %s. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(emailProperties.getSenderName(), name, roleName, inviteUrl, expirationHours, inviteUrl, inviteUrl, emailProperties.getSenderName());
    }

    private String buildPasswordResetEmailHtml(String name, String resetUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Password Reset Request</title>
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 0; color: #1e293b; }
                        .container { max-width: 580px; margin: 40px auto; background: #ffffff; border-radius: 8px; overflow: hidden; border: 1px solid #e2e8f0; }
                        .header { background: #0f172a; color: #ffffff; padding: 24px; text-align: center; }
                        .header h1 { margin: 0; font-size: 20px; font-weight: 600; }
                        .content { padding: 32px 28px; line-height: 1.6; }
                        .btn-box { text-align: center; margin: 28px 0; }
                        .btn { display: inline-block; background: #dc2626; color: #ffffff !important; text-decoration: none; padding: 12px 28px; border-radius: 6px; font-weight: 600; font-size: 15px; }
                        .footer { background: #f8fafc; padding: 16px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #e2e8f0; }
                        .link-box { word-break: break-all; font-size: 12px; color: #2563eb; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>%s</h1>
                        </div>
                        <div class="content">
                            <p>Hi <strong>%s</strong>,</p>
                            <p>We received a request to reset your password. Click the button below to choose a new password:</p>
                            <div class="btn-box">
                                <a href="%s" class="btn" target="_blank">Reset Password</a>
                            </div>
                            <p>This password reset link will expire in <strong>1 hour</strong>.</p>
                            <p class="link-box"><a href="%s">%s</a></p>
                            <p>If you did not request a password reset, you can safely ignore this email.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; %s. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(emailProperties.getSenderName(), name, resetUrl, resetUrl, resetUrl, emailProperties.getSenderName());
    }

    private String buildWelcomeEmailHtml(String username, Role role, String loginUrl) {
        String roleName = role != null ? role.name() : "Staff";
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Welcome</title>
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 0; color: #1e293b; }
                        .container { max-width: 580px; margin: 40px auto; background: #ffffff; border-radius: 8px; overflow: hidden; border: 1px solid #e2e8f0; }
                        .header { background: #0f172a; color: #ffffff; padding: 24px; text-align: center; }
                        .header h1 { margin: 0; font-size: 20px; font-weight: 600; }
                        .content { padding: 32px 28px; line-height: 1.6; }
                        .btn-box { text-align: center; margin: 28px 0; }
                        .btn { display: inline-block; background: #2563eb; color: #ffffff !important; text-decoration: none; padding: 12px 28px; border-radius: 6px; font-weight: 600; font-size: 15px; }
                        .footer { background: #f8fafc; padding: 16px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #e2e8f0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>%s</h1>
                        </div>
                        <div class="content">
                            <p>Hi <strong>%s</strong>,</p>
                            <p>Your account is activated and your <strong>%s</strong> profile is ready!</p>
                            <div class="btn-box">
                                <a href="%s" class="btn" target="_blank">Sign In to Dashboard</a>
                            </div>
                        </div>
                        <div class="footer">
                            <p>&copy; %s. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(emailProperties.getSenderName(), username, roleName, loginUrl, emailProperties.getSenderName());
    }
}
