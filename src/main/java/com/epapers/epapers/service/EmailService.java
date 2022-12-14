package com.epapers.epapers.service;

import javax.mail.internet.MimeMessage;

import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.util.AppUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EmailService {

    private static final String FILE_ACCESS_URL = "https://epapers.onrender.com/api/file?name=%s";
    private final JavaMailSender emailSender;

    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void mailPDF(String emailId, Epaper epaper) {
        FileSystemResource pdfFile = new FileSystemResource(epaper.getFile());
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper;

        try {
            log.info("\nSending mail to: {}, with attachment: {}\n", emailId, epaper.getFile().getName());
            helper = new MimeMessageHelper(message, true);
            helper.setFrom("ePapers <noreply@epapersonrender.com>");
            helper.setTo(emailId);
            helper.setBcc("kowligi1998@gmail.com");
            helper.setSubject("🎉🥳🎊 Hey! Your requested ePaper on : " + epaper.getDate() + " is ready.");
            String link = "Hello there,\n\nYou can access your ePaper from here: " + String.format(FILE_ACCESS_URL, epaper.getFile().getName());
            helper.setText("<html><body><p>"+ link +"</p><i>Now you can get in touch with our Telegram Bot: https://t.me/HtToi_bot</i> !</body></html>", true);

            if(!AppUtils.isLargeFile(epaper.getFile(), "GMAIL")) {
                helper.addAttachment(epaper.getFile().getName(), pdfFile);
            }

            emailSender.send(message);
            log.info("EMAIL SENT SUCCESSFULLY!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void mailSOS() {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper;

        try {
            helper = new MimeMessageHelper(message, true);
            helper.setFrom("ePapers <noreply@epapersonrender.com>");
            helper.setTo("suhasbk42@gmail.com");
            helper.setBcc("kowligi1998@gmail.com");
            helper.setSubject("E-Paper service is going down...");
            helper.setText(
                """
                Hey!

                Do not fret. I reckon you have around 5 minutes until service goes completely down.

                Follow these two instructions to restore availibility:

                1) Ping the website until its back again: https://epapers.onrender.com/ (check logs: https://dashboard.render.com/web/srv-ccmupnmn6mpqc1iul3jg/logs?mode=maximize)

                2) If it's already too late, trigger epapers to Telegram subscribers: https://epapers.onrender.com/api/trigger

                See you on the other side! 🙈
                """);

            emailSender.send(message);
            log.info("MONITOR EMAIL SENT SUCCESSFULLY!");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
