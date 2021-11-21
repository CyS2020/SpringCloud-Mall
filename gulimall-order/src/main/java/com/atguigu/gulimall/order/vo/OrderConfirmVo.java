package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author: CyS2020
 * @date: 2021/11/14
 */

@Data
public class OrderConfirmVo {

    // 防重令牌
    private String orderToken;

    // 收货地址列表
    private List<MemberAddressVo> address;

    // 所有选中的购物项
    private List<OrderItemVo> items;

    // 发票记录...

    // 优惠券信息
    private Integer integration;

    // 应付价格
    private BigDecimal payPrice;

    // 库存
    private Map<Long, Boolean> stocks;

    public Integer getCount() {
        return items.stream().map(OrderItemVo::getCount).reduce(0, Integer::sum);
    }

    public BigDecimal getTotal() {
        BigDecimal amount = new BigDecimal("0");
        if (items == null || items.isEmpty()) {
            return amount;
        }
        return items.stream().map(OrderItemVo::getTotalPrice).reduce(amount, BigDecimal::add);
    }

    public BigDecimal getPayPrice() {
        return getTotal();
    }

}
