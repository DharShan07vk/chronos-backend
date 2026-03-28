package com.chronos.chronos.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class MailService {
    @Autowired
    private JavaMailSender mailSender;

    private String getBlueprintHtmlMessage(String title, String badgeText, String content) {
        return "<div style=\"font-family: Arial, sans-serif; background-color: #F2F2F0; padding: 40px; color: #1A1A1A;\">"
                +
                "  <div style=\"background-color: #FFFFFF; border: 3px solid #1A1A1A; box-shadow: 6px 6px 0px #1A1A1A; padding: 30px; max-width: 600px; margin: 0 auto;\">"
                +
                "    <h1 style=\"font-family: 'Impact', 'Oswald', sans-serif; text-transform: uppercase; font-size: 32px; border-bottom: 3px solid #1A1A1A; padding-bottom: 10px; margin-top: 0; margin-bottom: 20px;\">"
                +
                title +
                "    </h1>" +
                "    <div style=\"margin-bottom: 25px;\">" +
                "      <span style=\"font-family: 'JetBrains Mono', Consolas, monospace; font-size: 14px; font-weight: bold; background-color: #FF4500; color: #FFFFFF; padding: 6px 12px; border: 2px solid #1A1A1A; box-shadow: 4px 4px 0px #1A1A1A;\">"
                +
                badgeText +
                "      </span>" +
                "    </div>" +
                "    <p style=\"font-family: 'JetBrains Mono', Consolas, monospace; font-size: 16px; line-height: 1.6; font-weight: bold;\">"
                +
                content +
                "    </p>" +
                "    <div style=\"margin-top: 40px; border-top: 3px solid #1A1A1A; padding-top: 15px;\">" +
                "      <p style=\"font-family: 'JetBrains Mono', Consolas, monospace; font-size: 12px; color: #1A1A1A; font-weight: bold; margin: 0;\">"
                +
                "        > CHRONOS_SYSTEM_NOTIFICATION<br>" +
                "        > STATUS: OK" +
                "      </p>" +
                "    </div>" +
                "  </div>" +
                "</div>";
    }

    public void Share(String to, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("CHRONOS NEW CAPSULE SHARED");

            String title = "NEW CAPSULE INCOMING";
            String badgeText = "SYS_ALERT :: " + LocalDate.now().toString();
            String content = "GREETINGS " + to.split("@")[0].toUpperCase() + ",<br><br>" +
                    "USER [" + name.toUpperCase() + "] HAS SHARED NEW MEMORIES WITH YOU.<br><br>" +
                    "ACCESS YOUR CHRONOS DASHBOARD TO VIEW.";

            helper.setText(getBlueprintHtmlMessage(title, badgeText, content), true);
            helper.setFrom("noreply@gmail.com");
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void UnlockMail(String to, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("CHRONOS // CAPSULE UNLOCKED");

            String title = "CAPSULE UNLOCKED";
            String badgeText = "SYS_ALERT :: " + LocalDate.now().toString();
            String content = "GREETINGS " + name.toUpperCase() + ",<br><br>" +
                    "YOUR TIME CAPSULE HAS OFFICIALLY REACHED ITS UNLOCK DATE.<br><br>" +
                    "THE CONTENTS ARE NOW DECRYPTED AND READY FOR VIEWING.";

            helper.setText(getBlueprintHtmlMessage(title, badgeText, content), true);
            helper.setFrom("noreply@gmail.com");
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
