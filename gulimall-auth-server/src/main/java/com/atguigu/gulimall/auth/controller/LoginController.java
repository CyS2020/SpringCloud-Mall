package com.atguigu.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.feign.MemberFeignService;
import com.atguigu.gulimall.auth.feign.SendSmsFeignService;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import com.atguigu.gulimall.auth.vo.UserRegistVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Autowired
    private MemberFeignService memberFeignService;

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
        log.info("验证码是:{}", code.substring(0, 5));
        sendSmsFeignService.sendCode(phone, code.substring(0, 5));
        return R.ok();
    }

    /**
     * 模拟重定向携带数据
     * TODO 重定向携带数据是利用session原理，将数据放在session中
     * 只要重定向后从session中取，取完之后session里面的数据就会删掉
     */
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            // 如果校验出错转发到注册页
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
        // 1. 校验验证码
        String code = vo.getCode();
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if (!StringUtils.isEmpty(s) && code.equals(s.substring(0, 5))) {
            // 校验通过后删除redis中验证码
            redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
            // 真正的注册，调用远程服务进行
            R r = memberFeignService.regist(vo);
            if (r.getCode() != 0) {
                Map<String, String> errors = new HashMap<>();
                errors.put("msg", r.getData("msg", new TypeReference<String>() {
                }));
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        } else {
            // 如果校验出错转发到注册页
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码错误");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
        return "redirect:http://auth.gulimall.com/login.html";
    }

    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes) {
        // 远程登录
        R login = memberFeignService.login(vo);
        if (login.getCode() != 0) {
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", login.getData("msg", new TypeReference<String>() {
            }));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
        return "redirect:http://gulimall.com";
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
