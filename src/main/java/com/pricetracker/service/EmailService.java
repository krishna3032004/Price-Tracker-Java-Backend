package com.pricetracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendSubscriptionConfirmation(String toEmail, String name, String productTitle) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("\"Product Updates\" <" + fromEmail + ">");
            helper.setTo(toEmail);
            helper.setSubject("You're on the list!");
            helper.setText(
                "<p>Hey " + name + ",</p>" +
                "<p>Thanks for showing interest in <strong>" + productTitle + "</strong>!</p>" +
                "<p>You're now on our notification list. We'll let you know as soon as the price drops.</p>" +
                "<p>Stay tuned,<br>The Team</p>",
                true
            );

            mailSender.send(message);
            log.info("Subscription email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send subscription email to {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendPriceDropAlert(String toEmail, String userName, String productTitle,
                                   Double oldPrice, Double newPrice, String productLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("\"Price Drop Alert\" <" + fromEmail + ">");
            helper.setTo(toEmail);
            helper.setSubject("Price Drop Alert: " + productTitle);
            helper.setText(
                "<h2>Good news!</h2>" +
                "<p>The price of <strong>" + productTitle + "</strong> has dropped from " +
                "<s>Rs." + oldPrice.intValue() + "</s> to <strong>Rs." + newPrice.intValue() + "</strong>.</p>" +
                "<p><a href=\"" + productLink + "\">Click here to buy now</a></p>",
                true
            );

            mailSender.send(message);
            log.info("Price drop email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send price drop email: {}", e.getMessage());
        }
    }
}
