package com.epapers.epapers.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.epapers.epapers.service.EmailService;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class MailController {
    
    @Autowired
    EmailService emailService;

    @PostMapping("/sendMail")
    public void notifyMyself(@RequestBody Map<String, String> metaData) {
        if (metaData.containsKey("subject") && metaData.containsKey("content")) {
            emailService.sendGenericMails(metaData.get("subject"), metaData.get("content"));
        }
    }
}
