package com.example.demo;

import com.example.demo.dao.DiscussPostMapper;
import com.example.demo.dao.LoginTicketMapper;
import com.example.demo.dao.MessageMapper;
import com.example.demo.entity.DiscussPost;
import com.example.demo.entity.LoginTicket;
import com.example.demo.entity.Message;
import com.example.demo.util.CommunityUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MapperTests {

    @Autowired
    LoginTicketMapper loginTicketMapper;

    @Autowired
    DiscussPostMapper discussPostMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Test
    public void testInsetLoginTicket(){
        LoginTicket loginTicket =new LoginTicket();
        loginTicket.setUserId(132);
        loginTicket.setTicket("周星星");
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis()+1000*60*10));
        int num = loginTicketMapper.insertLoginInsert(loginTicket);
        System.out.println(num);
    }

    @Test
    public void testSelectByTicket(){
        LoginTicket loginTicket = loginTicketMapper.selectByTicket("wenmingliang");
        System.out.println(loginTicket);
    }

    @Test
    public void testUpdateStatus(){
        System.out.println(loginTicketMapper.updateStatus("小亮", "1"));

    }

    @Test
    public void testInsertDiscussPost(){
        DiscussPost post = new DiscussPost();

        post.setTitle("文明亮");
        post.setContent("hahahhahaahahhahh");


        post.setCreateTime(new Date());


        int i = discussPostMapper.insertDiscussPost(post);
        System.out.println(i);
    }

    @Test
    public void testSelectLetters(){
        List<Message> list = messageMapper.selectConversations(111, 0, 20);
        for (Message message : list){
            System.out.println(message);
        }

         int count = messageMapper.selectConversationCount(111);
        System.out.println(count);

        final List<Message> messages = messageMapper.selectLetters("111_112", 0, 10);
        for (Message message : messages){
            System.out.println(message);
        }

         int i = messageMapper.selectLetterCount("111_112");
        System.out.println(i);

        count = messageMapper.selectLetterUnreadCount(131,"111_131");
        System.out.println(count);
    }

    @Test
    public void test(){
        String str = "b8ca3cbc6fd57c78736c66611a7e7044";
        System.out.println(CommunityUtil.md5(str+"167f9"));
    }
}
