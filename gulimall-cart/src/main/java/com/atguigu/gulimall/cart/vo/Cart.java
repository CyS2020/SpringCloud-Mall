package com.atguigu.gulimall.cart.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author: CyS2020
 * @date: 2021/11/7
 * 描述：购物车
 */
@Data
public class Cart {

    private List<CartItem> items;

    private Integer countNum;

    private Integer countType;

    private BigDecimal totalAmount;

    private BigDecimal reduce = new BigDecimal("0.0");

    public Integer getCountNum() {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return items.stream().map(CartItem::getCount).reduce(0, Integer::sum);
    }

    public Integer getCountType() {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return items.size();
    }

    public BigDecimal getTotalAmount() {
        BigDecimal amount = new BigDecimal("0.0");
        if (items == null || items.isEmpty()) {
            return amount;
        }
        amount = items.stream().filter(CartItem::getCheck).map(CartItem::getTotalPrice).reduce(amount, BigDecimal::add);
        return amount.subtract(reduce);
    }
}
