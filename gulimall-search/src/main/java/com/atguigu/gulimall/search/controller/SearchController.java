package com.atguigu.gulimall.search.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author: CyS2020
 * @date: 2021/10/5
 * 描述：搜索页面
 */

@Controller
public class SearchController {

    @GetMapping("/list.html")
    public String listPage() {
        return "list";
    }
}
