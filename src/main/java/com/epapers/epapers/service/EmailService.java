package com.epapers.epapers.service;

import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.util.AppUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;

@Component
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class.getName());
    private final JavaMailSender emailSender;

    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void mailPDF(String emailId, Epaper epaper) {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper;

        try {
            logger.info("\nSending mail to: {}, with attachment: {}\n", emailId, epaper.getFile().getName());
            helper = new MimeMessageHelper(message, true);
            helper.setFrom("noreply@htheroku.com");
            helper.setTo(emailId);
            helper.setSubject("🎉🥳🎊 Hey! Your ePaper for: " + epaper.getDate());
            helper.setText("Hi there, PFA the requested ePaper in PDF format.");

            FileSystemResource pdfFile = new FileSystemResource(epaper.getFile());
            helper.addAttachment(epaper.getFile().getName(), pdfFile);

            emailSender.send(message);
            logger.info("EMAIL SENT SUCCESSFULLY!");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            AppUtils.deleteFile(epaper.getFile());
            System.gc();
        }
    }
}
