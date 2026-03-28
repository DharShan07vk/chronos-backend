package com.chronos.chronos.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


@Service
public class MailService {
    @Autowired
    private  JavaMailSender mailSender;

    public void Share(String to, String name) {
        SimpleMailMessage mail = new SimpleMailMessage();

        mail.setTo(to);
        mail.setSubject("Hello " + to.split("@")[0] + ", New Capsule was shared");
        mail.setText("Hello " + name + " was shared new memories with you");
        mailSender.send(mail);
    }

    public void UnlockMail(String to, String name) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(to);
        mail.setText("Capsule was unlocked");
        mailSender.send(mail);
    }
}
