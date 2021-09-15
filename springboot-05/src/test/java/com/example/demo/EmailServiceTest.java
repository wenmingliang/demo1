package com.example.demo;


import com.example.demo.email.EmailService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @Test
    public void sendSimpleEmail(){
        String content = "您好,恭喜您....";
        emailService.sendSimpleMail("2848763719@qq.com","祝福邮件",content);
    }

    @Test
    public void sendMimeEmail(){
        String content = "<a href='//blog.csdn.net/'>您好，欢迎访问本网站,请点击链接激活</a>";
        emailService.sendHtmlMail("3461995832@qq.com","小姐姐请你加一下",content);

    }

    @Test
    public void sendAttachment(){
        emailService.sendAttachmentsMail("2848763719@qq.com","发送附件",
                "E:\\wml\\jetbrains无限重置插件\\idea-plugin-2.1.6.zip");
    }


}
