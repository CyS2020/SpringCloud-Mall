package com.atguigu.gulimall.product.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author: CyS2020
 * @date: 2021/10/14
 * 描述：详情信息
 */

@Controller
public class ItemController {

    @GetMapping("/{skuId}.html")
    public String skuItem(@PathVariable("skuId") Long skuId) {
        System.out.println("准备查询: " + skuId);
        return "item";
    }
}
