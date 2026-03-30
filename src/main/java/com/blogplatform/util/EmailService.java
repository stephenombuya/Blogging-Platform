package com.blogplatform.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Async
    public void sendVerificationEmail(String toEmail, String username, String token) {
        String verifyUrl = frontendUrl + "/verify-email?token=" + token;
        String subject = "Verify your Blog Platform account";
        String body = buildVerificationEmailBody(username, verifyUrl);
        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String username, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        String subject = "Reset your Blog Platform password";
        String body = buildPasswordResetEmailBody(username, resetUrl);
        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String username) {
        String subject = "Welcome to Blog Platform!";
        String body = buildWelcomeEmailBody(username);
        sendHtmlEmail(toEmail, subject, body);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    private String buildVerificationEmailBody(String username, String verifyUrl) {
        return """
                <html><body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                  <h2 style="color:#2c3e50;">Verify Your Email Address</h2>
                  <p>Hello <strong>%s</strong>,</p>
                  <p>Thank you for registering on Blog Platform. Please click the button below to verify your email address:</p>
                  <div style="text-align:center;margin:30px 0;">
                    <a href="%s" style="background:#3498db;color:white;padding:12px 30px;text-decoration:none;border-radius:4px;font-size:16px;">
                      Verify Email
                    </a>
                  </div>
                  <p>This link expires in 24 hours.</p>
                  <p>If you did not create an account, please ignore this email.</p>
                  <hr/><p style="color:#7f8c8d;font-size:12px;">Blog Platform Team</p>
                </body></html>
                """.formatted(username, verifyUrl);
    }

    private String buildPasswordResetEmailBody(String username, String resetUrl) {
        return """
                <html><body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                  <h2 style="color:#2c3e50;">Reset Your Password</h2>
                  <p>Hello <strong>%s</strong>,</p>
                  <p>We received a request to reset your Blog Platform password. Click the button below:</p>
                  <div style="text-align:center;margin:30px 0;">
                    <a href="%s" style="background:#e74c3c;color:white;padding:12px 30px;text-decoration:none;border-radius:4px;font-size:16px;">
                      Reset Password
                    </a>
                  </div>
                  <p>This link expires in 1 hour. If you did not request a password reset, ignore this email.</p>
                  <hr/><p style="color:#7f8c8d;font-size:12px;">Blog Platform Team</p>
                </body></html>
                """.formatted(username, resetUrl);
    }

    private String buildWelcomeEmailBody(String username) {
        return """
                <html><body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                  <h2 style="color:#2c3e50;">Welcome to Blog Platform! 🎉</h2>
                  <p>Hello <strong>%s</strong>,</p>
                  <p>Your account has been verified and is ready to use. Start writing your first blog post today!</p>
                  <div style="text-align:center;margin:30px 0;">
                    <a href="%s" style="background:#27ae60;color:white;padding:12px 30px;text-decoration:none;border-radius:4px;font-size:16px;">
                      Go to Blog Platform
                    </a>
                  </div>
                  <hr/><p style="color:#7f8c8d;font-size:12px;">Blog Platform Team</p>
                </body></html>
                """.formatted(username, frontendUrl);
    }
}
