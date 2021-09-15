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

    //允许多请求地址多加斜杠  比如 /msg/list   //msg/list
    @Bean
    public HttpFirewall httpFirewall() {
        return new DefaultHttpFirewall();
    }

    @LoginRequired
    @GetMapping("/setting")
    public String getSettingPage(Model model){
        // 上传文件名称
        String fileName = CommunityUtil.generateUUID();
        // 设置响应信息
        StringMap policy = new StringMap();
        policy.put("returnBody", CommunityUtil.getJSONString(0));
        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);
        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);
        return "/site/setting";
    }

     // 更新头像路径
    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String uploadHeaderUrl(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return CommunityUtil.getJSONString(1, "文件名不能为空！");
        }

        String url = headerBucketUrl + "/" +fileName;
        userService.updateHeader(hostHolder.getUser().getId(), url);

        return CommunityUtil.getJSONString(0);
    }

    // 废弃
    @LoginRequired
    @PostMapping("/upload")
    public String uploadHeader(MultipartFile headerImage, Model model){
        if(headerImage==null){
            model.addAttribute("error","您还没有上传图片!");
            return "/site/setting";
        }

        String filename = headerImage.getOriginalFilename();
        String suffix = filename.substring(filename.lastIndexOf("."));
        if(StringUtils.isEmpty(suffix)){
            model.addAttribute("error","文件的格式不正确!");
            return "/site/setting";
        }
        //生成随机的文件名
       filename = CommunityUtil.generateUUID()+suffix;
        //确定文件存放的路径
        File dest = new File(uploadPath+"/"+filename);
        try {
            //存储文件
            headerImage.transferTo(dest);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("上传文件失败："+e.getMessage());
            throw new RuntimeException("上传文件失败,服务器发生异常！",e);
        }

        //更新当前用户的头像的路径(web访问路径)
        //http://localhost:9999/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain+contextPath+"/user/header/"+filename;
        userService.updateHeader(user.getId(),headerUrl);

        return "redirect:/index";

    }

    // 废弃
    @RequestMapping(path = "/header/{filename}",method = RequestMethod.GET)
    public void getHeader(@PathVariable("filename") String filename, HttpServletResponse response){
        //服务器存放的路径
        filename = uploadPath+"/"+filename;
        //文件后缀
        String suffix = filename.substring(filename.lastIndexOf("."));
        //响应图片
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
            logger.error("读取头像失败:"+e.getMessage());
        }
    }

    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model){
        User user = userService.findUserById(userId);
        if (user ==null) {
            throw new RuntimeException("该用户不存在!");
        }

        //用户
        model.addAttribute("user", user);
        //点赞的数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        //关注数量
        long followeeCount = followService.findFollowerKeyCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        //粉丝数量
        long followerCount = followService.findFollowerKeyCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        //是否已关注
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
    //修改密码，model变量用来向页面返回数据
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
    //修改密码，model变量用来向页面返回数据
    public String changePasswordByCode(String email,String code,String password, Model model,
                                       @CookieValue("codeOwner") String codeOwner) {
        String kaptcha = null;
        try {
            if (!StringUtils.isEmpty(codeOwner)) {
                String redisKey = RedisKeyUtil.getCodeKey(codeOwner);
                kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
            }
        }catch (Exception e) {
            model.addAttribute("codeMsg", "验证码失效!");
            return "/site/forget";
        }


        if (StringUtils.isEmpty(kaptcha) || StringUtils.isEmpty(code) || !kaptcha.equals(code)) {
            model.addAttribute("codeMsg", "验证码不正确!");
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
    //向邮箱发送验证码
    @ResponseBody
    public String sendCode(String email,HttpServletResponse response){
        User user=userService.findUserByEmail(email);
        if(user==null){
            return CommunityUtil.getJSONString(1,"您输入的邮箱格式有误或未注册");
        }
        userService.sendCode(email,response);
        return CommunityUtil.getJSONString(0);
    }
}
