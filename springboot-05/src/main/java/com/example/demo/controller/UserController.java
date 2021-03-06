package com.example.demo.controller;

import com.example.demo.annotation.LoginRequired;
import com.example.demo.entity.User;
import com.example.demo.service.FollowService;
import com.example.demo.service.LikeService;
import com.example.demo.service.UserService;
import com.example.demo.util.CommunityConstant;
import com.example.demo.util.CommunityUtil;
import com.example.demo.util.HostHolder;
import com.example.demo.util.RedisKeyUtil;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.util.StringUtils;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;

    //?????????????????????????????????  ?????? /msg/list   //msg/list
    @Bean
    public HttpFirewall httpFirewall() {
        return new DefaultHttpFirewall();
    }

    @LoginRequired
    @GetMapping("/setting")
    public String getSettingPage(Model model){
        // ??????????????????
        String fileName = CommunityUtil.generateUUID();
        // ??????????????????
        StringMap policy = new StringMap();
        policy.put("returnBody", CommunityUtil.getJSONString(0));
        // ??????????????????
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);
        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);
        return "/site/setting";
    }

     // ??????????????????
    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String uploadHeaderUrl(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return CommunityUtil.getJSONString(1, "????????????????????????");
        }

        String url = headerBucketUrl + "/" +fileName;
        userService.updateHeader(hostHolder.getUser().getId(), url);

        return CommunityUtil.getJSONString(0);
    }

    // ??????
    @LoginRequired
    @PostMapping("/upload")
    public String uploadHeader(MultipartFile headerImage, Model model){
        if(headerImage==null){
            model.addAttribute("error","????????????????????????!");
            return "/site/setting";
        }

        String filename = headerImage.getOriginalFilename();
        String suffix = filename.substring(filename.lastIndexOf("."));
        if(StringUtils.isEmpty(suffix)){
            model.addAttribute("error","????????????????????????!");
            return "/site/setting";
        }
        //????????????????????????
       filename = CommunityUtil.generateUUID()+suffix;
        //???????????????????????????
        File dest = new File(uploadPath+"/"+filename);
        try {
            //????????????
            headerImage.transferTo(dest);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("?????????????????????"+e.getMessage());
            throw new RuntimeException("??????????????????,????????????????????????",e);
        }

        //????????????????????????????????????(web????????????)
        //http://localhost:9999/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain+contextPath+"/user/header/"+filename;
        userService.updateHeader(user.getId(),headerUrl);

        return "redirect:/index";

    }

    // ??????
    @RequestMapping(path = "/header/{filename}",method = RequestMethod.GET)
    public void getHeader(@PathVariable("filename") String filename, HttpServletResponse response){
        //????????????????????????
        filename = uploadPath+"/"+filename;
        //????????????
        String suffix = filename.substring(filename.lastIndexOf("."));
        //????????????
        response.setContentType("image"+suffix);
        try(
                OutputStream os = response.getOutputStream();
                FileInputStream fis = new FileInputStream(filename);
                )
        {

            int b=0;
            byte[] buffer = new byte[1024];
            while ((b=fis.read(buffer))!=-1){
                os.write(buffer,0,b);

            }


        } catch (IOException e) {
            logger.error("??????????????????:"+e.getMessage());
        }
    }

    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model){
        User user = userService.findUserById(userId);
        if (user ==null) {
            throw new RuntimeException("??????????????????!");
        }

        //??????
        model.addAttribute("user", user);
        //???????????????
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        //????????????
        long followeeCount = followService.findFollowerKeyCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        //????????????
        long followerCount = followService.findFollowerKeyCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        //???????????????
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);
        return "/site/profile";
    }


    @RequestMapping(value = "/forgetPassword", method = RequestMethod.GET)
    public String updatePassword() {

        return "/site/forget";
    }


    //    @LoginRequired
    @RequestMapping(path = "/changePassword", method = {RequestMethod.GET,RequestMethod.POST})
    //???????????????model?????????????????????????????????
    public String changePassword(String oldPassword,String newPassword,String confirmPassword, Model model) {
        User user = hostHolder.getUser();
        Map<String, Object> map = userService.changePassword(user,oldPassword, newPassword, confirmPassword);
        if(map == null || map.isEmpty()){
            return "redirect:/index";
        }else {
            model.addAttribute("oldPasswordMsg", map.get("oldPasswordMsg"));
            model.addAttribute("newPasswordMsg", map.get("newPasswordMsg"));
            model.addAttribute("confirmPasswordMsg", map.get("confirmPasswordMsg"));
            return "/site/setting";
        }
    }

    @RequestMapping(path = "/changePasswordByCode", method = {RequestMethod.GET,RequestMethod.POST})
    //???????????????model?????????????????????????????????
    public String changePasswordByCode(String email,String code,String password, Model model,
                                       @CookieValue("codeOwner") String codeOwner) {
        String kaptcha = null;
        try {
            if (!StringUtils.isEmpty(codeOwner)) {
                String redisKey = RedisKeyUtil.getCodeKey(codeOwner);
                kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
            }
        }catch (Exception e) {
            model.addAttribute("codeMsg", "???????????????!");
            return "/site/forget";
        }


        if (StringUtils.isEmpty(kaptcha) || StringUtils.isEmpty(code) || !kaptcha.equals(code)) {
            model.addAttribute("codeMsg", "??????????????????!");
            return "/site/forget";
        }
        Map<String, Object> map = userService.changePasswordByCode(email, password);
        if (map.containsKey("success")) {
            return "redirect:/login";
        } else {
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/forget";
        }
    }


    @RequestMapping(path = "/sendCode", method = RequestMethod.POST)
    //????????????????????????
    @ResponseBody
    public String sendCode(String email,HttpServletResponse response){
        User user=userService.findUserByEmail(email);
        if(user==null){
            return CommunityUtil.getJSONString(1,"??????????????????????????????????????????");
        }
        userService.sendCode(email,response);
        return CommunityUtil.getJSONString(0);
    }
}
