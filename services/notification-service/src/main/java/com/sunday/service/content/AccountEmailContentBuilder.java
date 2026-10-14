package com.sunday.service.content;

import com.sunday.common_lib.event.EmailVerificationRequestedEvent;
import com.sunday.common_lib.event.ExistingAccountSignupAttemptedEvent;
import com.sunday.common_lib.event.PasswordResetRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Subjects and HTML bodies for account emails: verification and password reset. */
@Service
public class AccountEmailContentBuilder {

    private static final DateTimeFormatter EXPIRY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    private final TemplateEngine templateEngine;
    private final String verifyEmailUrl;
    private final String resetPasswordUrl;
    private final String signInUrl;
    private final String forgotPasswordUrl;

    public AccountEmailContentBuilder(TemplateEngine templateEngine,
                                      @Value("${notification.verify-email-url}") String verifyEmailUrl,
                                      @Value("${notification.reset-password-url}") String resetPasswordUrl,
                                      @Value("${notification.sign-in-url}") String signInUrl,
                                      @Value("${notification.forgot-password-url}") String forgotPasswordUrl) {
        this.templateEngine = templateEngine;
        this.verifyEmailUrl = verifyEmailUrl;
        this.resetPasswordUrl = resetPasswordUrl;
        this.signInUrl = signInUrl;
        this.forgotPasswordUrl = forgotPasswordUrl;
    }

    public String subjectForEmailVerification() {
        return "Verify your email address";
    }

    public String bodyForEmailVerification(EmailVerificationRequestedEvent event) {
        return render("email/verify-email", event.getFirstName(), link(verifyEmailUrl, event.getToken()),
                event.getExpiresAt() != null ? event.getExpiresAt().format(EXPIRY_FMT) : null);
    }

    public String subjectForPasswordReset() {
        return "Reset your password";
    }

    public String bodyForPasswordReset(PasswordResetRequestedEvent event) {
        return render("email/password-reset", event.getFirstName(), link(resetPasswordUrl, event.getToken()),
                event.getExpiresAt() != null ? event.getExpiresAt().format(EXPIRY_FMT) : null);
    }

    public String subjectForExistingAccountSignup() {
        return "Someone tried to create an account with your email";
    }

    public String bodyForExistingAccountSignup(ExistingAccountSignupAttemptedEvent event) {
        Context ctx = new Context(Locale.ENGLISH);
        ctx.setVariable("firstName", event.getFirstName());
        ctx.setVariable("signInLink", signInUrl);
        ctx.setVariable("forgotPasswordLink", forgotPasswordUrl);
        return templateEngine.process("email/existing-account-signup", ctx);
    }

    private String render(String template, String firstName, String link, String expiresAt) {
        Context ctx = new Context(Locale.ENGLISH);
        ctx.setVariable("firstName", firstName);
        ctx.setVariable("link", link);
        ctx.setVariable("expiresAt", expiresAt);
        return templateEngine.process(template, ctx);
    }

    private static String link(String base, String token) {
        return base + (base.contains("?") ? "&" : "?") + "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
