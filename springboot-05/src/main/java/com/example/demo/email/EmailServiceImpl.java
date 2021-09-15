package com.example.demo.email;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.logging.Logger;

@Service
public class EmailServiceImpl implements EmailService{



   @Autowired(required = false)
   private JavaMailSender javaMailSender;
    @Value("${spring.mail.from}")
    private String from;
    @Override
    public void sendSimpleMail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();

        //发件人
        message.setFrom(from);
        //收件人
        message.setTo(to);
        //邮件主题
        message.setSubject(subject);
        //邮件主图
        message.setText(content);
        //发送邮件
        javaMailSender.send(message);


    }

    @Override
    public void sendHtmlMail(String to, String subject, String content) {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper messageHelper;
        try {
            messageHelper = new MimeMessageHelper(message,true);
            messageHelper.setFrom(from);
            messageHelper.setTo(to);
            messageHelper.setSubject(subject);
            messageHelper.setText(content,true);
            javaMailSender.send(message);


        } catch (MessagingException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void sendAttachmentsMail(String to, String subject, String filePath) {

        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper messageHelper;
        try {
            messageHelper = new MimeMessageHelper(message,true);
            messageHelper.setFrom(from);
            messageHelper.setTo(to);
            messageHelper.setSubject(subject);
            messageHelper.setText(filePath,true);
            //携带附件
            FileSystemResource file = new FileSystemResource(filePath);
            String fileName = filePath.substring(filePath.lastIndexOf(File.separator));
            messageHelper.addAttachment(fileName,file);

            javaMailSender.send(message);

        } catch (MessagingException e) {
            e.printStackTrace();
        }

    }
}
