package com.atguigu.gulimall.order.to;

import lombok.Data;

/**
 * @author: CyS2020
 * @date: 2021/11/7
 * 描述：购物车内商品
 */
@Data
public class OrderItemTo {

    private Long skuId;

    private String title;

    private Integer count;
}
