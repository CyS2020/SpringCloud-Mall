package com.atguigu.gulimall.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author: CyS2020
 * @date: 2021/10/20
 */
@Configuration
public class GulimallWebConfig implements WebMvcConfigurer {

    /**
     * 发送一个请求直接跳转到一个页面
     * SpringMVC ViewController:将请求和页面映射过来
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
//        registry.addViewController("/login.html").setViewName("login");
        registry.addViewController("/reg.html").setViewName("reg");
    }
}
