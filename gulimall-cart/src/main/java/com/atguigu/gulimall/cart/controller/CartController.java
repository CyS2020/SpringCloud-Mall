package com.atguigu.gulimall.cart.controller;

import com.atguigu.common.to.UserInfoTo;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author: CyS2020
 * @date: 2021/11/7
 */

@Controller
public class CartController {

    @GetMapping("/cart.html")
    public String cartListPage() {
        // 快速得到用户信息，登录使用userId, 未登录使用userKey
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        System.out.println(userInfoTo);
        return "cartList";

    }
}
