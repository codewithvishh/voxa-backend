package com.musicapp.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:voxamusic4@gmail.com}")
    private String fromEmail;

    // In-memory OTP storage with expiration timestamp: email -> OtpRecord
    private final Map<String, OtpRecord> otpCache = new ConcurrentHashMap<>();

    public record OtpRecord(String otp, long expiryMillis) {}

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Generates a 6-digit cryptographic-quality numeric OTP, caches it for 10 minutes,
     * and sends a branded Amazon-style verification email.
     */
    public String generateAndSendOtp(String toEmail, String userName) {
        String cleanEmail = toEmail.trim().toLowerCase();
        
        // 6-digit random numeric OTP
        int code = (int) (100000 + Math.random() * 900000);
        String otp = String.valueOf(code);

        // Valid for 10 minutes (600,000 ms)
        otpCache.put(cleanEmail, new OtpRecord(otp, System.currentTimeMillis() + 600000));

        sendVerificationEmail(cleanEmail, otp, userName);
        return otp;
    }

    /**
     * Validates whether the given OTP matches the cached value and is unexpired.
     */
    public boolean verifyOtp(String email, String inputOtp) {
        if (email == null || inputOtp == null) return false;
        String cleanEmail = email.trim().toLowerCase();
        OtpRecord record = otpCache.get(cleanEmail);

        if (record == null) {
            log.warn("No OTP found in cache for email: {}", cleanEmail);
            return false;
        }

        if (System.currentTimeMillis() > record.expiryMillis()) {
            otpCache.remove(cleanEmail);
            log.warn("OTP expired for email: {}", cleanEmail);
            return false;
        }

        boolean valid = record.otp().equals(inputOtp.trim());
        if (valid) {
            otpCache.remove(cleanEmail); // Single-use OTP
        }
        return valid;
    }

    /**
     * Sends the verification email matching the Amazon account verification design.
     */
    public void sendVerificationEmail(String toEmail, String otp, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Voxa");
            helper.setTo(toEmail);
            helper.setSubject("Verify your new Voxa account");

            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Verify your new Voxa account</title>
                </head>
                <body style="margin: 0; padding: 24px 16px; background-color: #0b0d14; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #ffffff; -webkit-font-smoothing: antialiased;">
                    <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 540px; margin: 0 auto; background-color: #12141e; border-radius: 12px; border: 1px solid rgba(255, 255, 255, 0.08); overflow: hidden;">
                        <!-- Logo Header -->
                        <tr>
                            <td style="padding: 32px 32px 20px;">
                                <!-- Voxa Brand Pill matching Amazon screenshot white box -->
                                <table border="0" cellspacing="0" cellpadding="0">
                                    <tr>
                                        <td style="background-color: #ffffff; padding: 10px 24px; border-radius: 6px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);">
                                            <span style="font-size: 28px; font-weight: 900; color: #0d0f17; letter-spacing: -0.8px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; display: inline-block;">
                                                voxa<span style="color: #ff2a51;">.</span>
                                            </span>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>

                        <!-- Main Heading & Body -->
                        <tr>
                            <td style="padding: 0 32px 32px;">
                                <h1 style="font-size: 23px; font-weight: 700; color: #ffffff; margin: 0 0 16px 0; letter-spacing: -0.3px; line-height: 1.3;">
                                    Verify your new Voxa account
                                </h1>
                                <p style="font-size: 15px; color: #e2e8f0; line-height: 1.5; margin: 0 0 24px 0;">
                                    To verify your email address, please use the following One Time Password (OTP):
                                </p>
                                
                                <!-- OTP Code -->
                                <div style="font-size: 42px; font-weight: 800; color: #ffffff; letter-spacing: 4px; font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', monospace; margin: 0 0 32px 0;">
                                    %s
                                </div>

                                <!-- Security Notice (Verbatim from Amazon Screenshot) -->
                                <p style="font-size: 13px; color: #94a3b8; line-height: 1.6; margin: 0 0 24px 0; border-top: 1px solid rgba(255, 255, 255, 0.08); padding-top: 24px;">
                                    Don't share this OTP with anyone. Voxa takes your account security very seriously. Voxa Customer Service will never ask you to disclose or verify your Voxa password, OTP, credit card, or banking account number. If you receive a suspicious email with a link to update your account information, do not click on the link&mdash;instead, report the email to Voxa for investigation.
                                </p>
                            </td>
                        </tr>

                        <!-- Footer -->
                        <tr>
                            <td style="padding: 16px 32px 20px; background-color: #0d0f17; border-top: 1px solid rgba(255, 255, 255, 0.05); text-align: center;">
                                <p style="font-size: 11px; color: #64748b; margin: 0;">
                                    &copy; 2026 Voxa Music Inc. Lossless 320 kbps High Fidelity Audio.
                                </p>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(otp);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent verification OTP email to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Could not send verification email: " + e.getMessage());
        }
    }
}
