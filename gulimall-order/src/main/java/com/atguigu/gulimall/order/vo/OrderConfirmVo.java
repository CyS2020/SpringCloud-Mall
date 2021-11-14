package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

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

    // 订单总额
    private BigDecimal total;

    // 应付价格
    private BigDecimal payPrice;

    // 商品数量
    private Integer count;

    public BigDecimal getTotal() {
        BigDecimal amount = new BigDecimal("0");
        if (items == null || items.isEmpty()) {
            return amount;
        }
        return items.stream().map(OrderItemVo::getPrice).reduce(amount, BigDecimal::add);
    }

    public BigDecimal getPayPrice() {
        return getTotal();
    }

}
