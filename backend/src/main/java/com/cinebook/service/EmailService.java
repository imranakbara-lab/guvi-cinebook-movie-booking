package com.cinebook.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Notification module: registration, booking confirmation and cancellation emails (Spring Boot Starter Mail).
 * When MAIL_ENABLED is false (default) emails are written to the log instead.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean enabled;
    private final String from;
    private final String frontendUrl;

    public EmailService(ObjectProvider<JavaMailSender> mailSender,
                        @Value("${app.mail.enabled:false}") boolean enabled,
                        @Value("${app.mail.from:CineBook <no-reply@cinebook.app>}") String from,
                        @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.frontendUrl = frontendUrl.replaceAll("/$", "");
    }

    /** Ticket details passed as plain values so the email can be sent after the transaction ends. */
    public record Ticket(String email, String name, String reference, Long bookingId, String movie, String theater,
                         String city, String showTime, String seats, String amount) {}

    @Async
    public void sendWelcome(String toEmail, String name) {
        send(toEmail, "Welcome to CineBook!",
                "<h2>Hi " + esc(name) + ",</h2><p>Your CineBook account is ready. Grab your popcorn!</p>"
                        + "<p><a href=\"" + frontendUrl + "\">Browse movies</a></p>");
    }

    @Async
    public void sendBookingConfirmed(Ticket t) {
        send(t.email(), "Booking confirmed: " + t.movie() + " (" + t.reference() + ")",
                "<h2>Your tickets are confirmed 🎬</h2>" + ticketTable(t)
                        + "<p><a href=\"" + frontendUrl + "/bookings/" + t.bookingId() + "\">View your ticket</a></p>"
                        + "<p>Please arrive 15 minutes before the show.</p>");
    }

    @Async
    public void sendBookingCancelled(Ticket t, boolean refunded) {
        send(t.email(), "Booking cancelled: " + t.movie() + " (" + t.reference() + ")",
                "<h2>Your booking has been cancelled</h2>" + ticketTable(t)
                        + (refunded ? "<p>A refund of <b>₹" + esc(t.amount()) + "</b> has been initiated to your original payment method.</p>" : ""));
    }

    private String ticketTable(Ticket t) {
        return "<p>Hi " + esc(t.name()) + ",</p><table cellpadding=\"4\">"
                + row("Booking ID", t.reference()) + row("Movie", t.movie())
                + row("Theater", t.theater() + ", " + t.city()) + row("Show time", t.showTime())
                + row("Seats", t.seats()) + row("Amount", "₹" + t.amount()) + "</table>";
    }

    private static String row(String k, String v) {
        return "<tr><td><b>" + esc(k) + "</b></td><td>" + esc(v) + "</td></tr>";
    }

    void send(String to, String subject, String html) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!enabled || sender == null) {
            log.info("[email disabled] To: {} | Subject: {} | Body: {}", to, subject, html.replaceAll("<[^>]+>", " "));
            return;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
            log.info("Email sent to {} ({})", to, subject);
        } catch (Exception e) {
            log.warn("Could not send email to {}: {}", to, e.getMessage());
        }
    }

    private static String esc(String s) { return s == null ? "" : HtmlUtils.htmlEscape(s); }
}
