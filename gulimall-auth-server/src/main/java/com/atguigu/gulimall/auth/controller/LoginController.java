package com.atguigu.gulimall.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author: CyS2020
 * @date: 2021/10/19
 */
@Controller
public class LoginController {

    @GetMapping("/login.html")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/reg.html")
    public String regPage() {
        return "reg";
    }
}
