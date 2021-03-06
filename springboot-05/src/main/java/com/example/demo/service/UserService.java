package com.example.demo.service;


import com.example.demo.dao.LoginTicketMapper;
import com.example.demo.dao.UserMapper;
import com.example.demo.email.EmailService;
import com.example.demo.entity.LoginTicket;
import com.example.demo.entity.User;
import com.example.demo.util.CommunityConstant;
import com.example.demo.util.CommunityUtil;
import com.example.demo.util.MailClient;
import com.example.demo.util.RedisKeyUtil;
import org.apache.ibatis.annotations.Param;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.converter.json.GsonBuilderUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.net.HttpCookie;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;


    @Autowired
    private EmailService emailService;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

//    @Autowired
//    private LoginTicketMapper loginTicketMapper;

    public User findUserById(int id){
//        return userMapper.selectById(id);
        User user = getCache(id);
        if (user == null) {
            user = initCache(id);

        }
        return user;
    }

    public Map<String,Object> register(User user){
        Map<String,Object> map = new HashMap<>();

        if(user==null){
            throw new IllegalArgumentException("??????????????????!");

        }
        if(StringUtils.isEmpty(user.getUsername())){
            map.put("usernameMsg","??????????????????");
            return map;
        }
        if(StringUtils.isEmpty(user.getPassword())){
            map.put("passwordMsg","??????????????????");
            return map;
        }
        if(StringUtils.isEmpty(user.getEmail())){
            map.put("emailMsg","??????????????????");
            return map;
        }

        //????????????
        User u = userMapper.selectByName(user.getUsername());
        if(u != null){
            map.put("usernameMsg","?????????????????????!");
            return map;
        }
        //????????????
        u = userMapper.selectByEmail(user.getEmail());
        if(u != null){
            map.put("emailMsg","????????????????????????");
            return map;
        }

        //????????????
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png",
                new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //????????????
        Context context = new Context();
        context.setVariable("email",user.getEmail());
        //http://localhost:9999/community/activation/101/code
        String url = domain+contextPath+"/activation/"+user.getId()+"/"+user.getActivationCode();
        context.setVariable("url",url);
        //???????????????????????????thymeleaf??????context??????/mail/activation.html???????????????????????????mail??????????????????
        String content = templateEngine.process("/mail/activation", context);
//        mailClient.sendMail(user.getEmail(), "????????????", content);
        emailService.sendSimpleMail(user.getEmail(),"????????????",content);



        return map;
    }

    public int activation(int userId,String code){
        User user = userMapper.selectById(userId);
        if(user.getStatus()==1){
            return ACTIVATION_REPEAT;
        }else if(user.getActivationCode().equals(code)){
            userMapper.updateStatus(userId,1);
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        }else{
            return ACTIVATION_FAILURE;

        }

    }

    public Map<String,Object> login(String username,
                                  String password,
                                    int expiredSeconds){
        Map<String,Object> map = new HashMap<>();
        if(StringUtils.isEmpty(username)){
            map.put("userNameMessage","?????????????????????");
            return map;
        }
        if(StringUtils.isEmpty(password)){
            map.put("userPasswordMessage","??????????????????");
            return map;
        }



        // ????????????
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "??????????????????!");
            return map;
        }

        // ????????????
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "??????????????????!");
            return map;
        }
        // ????????????
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "???????????????!");
            return map;
        }


        //??????????????????
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis()+expiredSeconds*1000l));
//        loginTicketMapper.insertLoginInsert(loginTicket);

        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey, loginTicket);

        map.put("ticket",loginTicket.getTicket());

        return map;

    }

    public void logout(String ticket){
//        loginTicketMapper.updateStatus(ticket,"1");
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey, loginTicket);

    }

    public LoginTicket findLoginTicket(String ticket){
//        LoginTicket loginTicket = loginTicketMapper.selectByTicket(ticket);
//        return loginTicket;
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }

    public int updateHeader(int userId,String headerUrl){
//       return  userMapper.updateHeader(userId,headerUrl);
        int rows = userMapper.updateHeader(userId,headerUrl);
        clearCache(userId);
        return rows;
    }

    public Map<String, Object> changePassword(User user, String oldPassword, String newPassword,
                                              String confirmPassword) {
        Map<String, Object> map = new HashMap<>();
        // ????????????
        oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
        if (!user.getPassword().equals(oldPassword)) {
            map.put("oldPasswordMsg", "???????????????!");
            return map;
        }
        if (StringUtils.isEmpty(newPassword)) {
            map.put("newPasswordMsg", "??????????????????!");
            return map;
        }
        if(!newPassword.equals(confirmPassword)){
            map.put("confirmPasswordMsg", "??????????????????????????????!");
            return map;
        }
        int id=user.getId();
        newPassword=CommunityUtil.md5(newPassword + user.getSalt());
        if(oldPassword.equals(newPassword)){
            map.put("newPasswordMsg", "???????????????????????????!");
            return map;
        }
        userMapper.updatePassword(id,newPassword);
        clearCache(user.getId());
        return map;
    }



    public User findUserByName(String username){
        return userMapper.selectByName(username);
    }

    // 1.????????????????????????
    private User getCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }
    // 2.?????????????????????????????????
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user,3600, TimeUnit.SECONDS);
        return user;
    }

    // 3.?????????????????????????????????
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    // ???????????????????????????
    public User findUserByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    public void sendCode(String email, HttpServletResponse response) {
        String text = CommunityUtil.generateUUID().substring(0,6);
        // ??????????????????
        String codeOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("codeOwner", codeOwner);
        //??????????????????10min
        cookie.setMaxAge(60*10);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        // ??????????????????Redis,??????????????????10min
        String redisKey = RedisKeyUtil.getCodeKey(codeOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60*10, TimeUnit.SECONDS);

        // ????????????,??????thymeleaf???????????????????????????
        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("text", text);
        //???????????????????????????thymeleaf??????context??????/mail/activation.html???????????????????????????mail??????????????????
        String content = templateEngine.process("/mail/forget", context);
        emailService.sendSimpleMail(email, "????????????", content);

    }

    public Map<String, Object> changePasswordByCode(String email, String password) {
        Map<String, Object> map = new HashMap<>();
        // ????????????
        if (StringUtils.isEmpty(password)) {
            map.put("passwordMsg", "??????????????????!");
            return map;
        }
        User user = userMapper.selectByEmail(email);
        password = CommunityUtil.md5(password + user.getSalt());
        userMapper.updatePassword(user.getId(),password);
        map.put("success","success");
        clearCache(user.getId());
        return map;
    }

    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.findUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }
}
