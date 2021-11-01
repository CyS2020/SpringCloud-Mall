package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.vo.MemberRespVo;
import com.atguigu.gulimall.auth.vo.SocialUser;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: CyS2020
 * @date: 2021/11/1
 * 处理社交登录请求
 */
@Slf4j
@Controller
public class Auth2Controller {

    @Autowired
    private MemberFeignService memberFeignService;

    @GetMapping("/oauth2.0/weibo/success")
    public String weibo(@RequestParam("code") String code) throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("client_id", "1384724995");
        map.put("client_secret", "2458cdc6c30b0f2d840d0536f1ed8f0a");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://auth.gulimall.com/oauth2.0/weibo/success");
        map.put("code", code);
        // 1. 根据code换取accessToken
        HttpResponse response = HttpUtils.doPost("https://api.weibo.com", "/oauth2/access_token", "post", Collections.emptyMap(), null, map);
        // 2. 处理accessToken
        if (response.getStatusLine().getStatusCode() == 200) {
            String str = EntityUtils.toString(response.getEntity());
            Gson gson = new Gson();
            SocialUser socialUser = gson.fromJson(str, SocialUser.class);
            // 知道当前是哪个社交用户进行登录或者注册
            R r = memberFeignService.oauthLogin(socialUser);
            if (r.getCode() == 0) {
                MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {
                });
                log.info("登录成功, 用户信息: {}", data.toString());
                return "redirect:http://gulimall.com";
            }
        }
        return "redirect:http://auth.gulimall.com/login.html";
    }
}
