package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author: CyS2020
 * @date: 2021/11/7
 * 描述：购物车内商品
 */
@Data
public class OrderItemVo {

    private Long skuId;

    private String title;

    private String image;

    private List<String> skuAttr;

    private BigDecimal price;

    private Integer count;

    private BigDecimal totalPrice;

    private BigDecimal weight;

    public BigDecimal getTotalPrice() {
        return this.price.multiply(new BigDecimal("" + this.count));
    }
}
