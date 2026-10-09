package com.sunday.service.channel;

import com.sunday.service.model.NotificationChannel;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailChannelProvider extends ResilientChannelSupport implements NotificationChannelProvider {

    private final JavaMailSender mailSender;

    @Value("${notification.from-email}")
    private String fromEmail;

    @Value("${notification.from-name}")
    private String fromName;

    public EmailChannelProvider(JavaMailSender mailSender, CircuitBreakerRegistry circuitBreakerRegistry,
                                 RateLimiterRegistry rateLimiterRegistry) {
        super("email", circuitBreakerRegistry, rateLimiterRegistry);
        this.mailSender = mailSender;
    }

    @Override
    public NotificationChannel channelType() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public String send(String recipient, String subject, String htmlBody) throws Exception {
        return guarded(() -> {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            return null; // plain SMTP has no provider message id to report back
        });
    }
}
