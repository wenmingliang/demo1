package com.example.demo;

import com.example.demo.dao.DiscussPostMapper;
import com.example.demo.dao.UserMapper;
import com.example.demo.entity.DiscussPost;
import com.example.demo.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class DemoApplicationTests {

    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private UserMapper mapper;
    @Test
    void contextLoads() {
    }

    @Test
    public void testSelectPosts(){
        List<DiscussPost> list = discussPostMapper.selectDiscussPosts(103, 0, 10, 0);
        for(DiscussPost post : list){
            System.out.println(post);
        }

        int rows = discussPostMapper.selectDiscussPostRow(149);
        System.out.println(rows);
    }
    @Test
    public void testSelectById(){
        User user = mapper.selectById(103);
        System.out.println(user);


    }

}
