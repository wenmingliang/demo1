package com.example.demo.email;

public interface EmailService {

    void sendSimpleMail(String to,String subject,String content);

    void sendHtmlMail(String to,String subject,String content);

    void sendAttachmentsMail(String to,String subject,String filePath);
}
