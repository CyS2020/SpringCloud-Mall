package com.atguigu.gulimall.auth.controller;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.feign.SendSmsFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author: CyS2020
 * @date: 2021/10/19
 */
@Controller
@Slf4j
public class LoginController {
    /**
     * 发送一个请求直接跳转到一个页面
     * SpringMVC ViewController:将请求和页面映射过来
     *
     * @GetMapping("/login.html")
     * @GetMapping("/reg.html")
     */
    @Autowired
    private SendSmsFeignService sendSmsFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone) {

        // 1. TODO 接口防刷

        // 2. 验证码再次校验, redis存key-phone, value-code sms:code138432722 -> 14515
        String key = AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone;
        String oldCode = redisTemplate.opsForValue().get(key);
        if (!StringUtils.isEmpty(oldCode)) {
            long oldTime = Long.parseLong(oldCode.substring(5));
            if (System.currentTimeMillis() - oldTime < 60000) {
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }


        // redis缓存验证码，防止同一个phone在60s内再次发送验证码
        String code = generateCode();
        redisTemplate.opsForValue().set(key, code, 10, TimeUnit.MINUTES);
        log.info("验证码是:{}", code);
        sendSmsFeignService.sendCode(phone, code);
        return R.ok();
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            sb.append(random.nextInt(10));
        }
        sb.append(System.currentTimeMillis());
        return sb.toString();
    }
}
