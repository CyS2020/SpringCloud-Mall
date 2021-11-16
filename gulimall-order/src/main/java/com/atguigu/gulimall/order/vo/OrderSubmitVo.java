package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author: CyS2020
 * @date: 2021/11/16
 * 描述：页面提交的订单数据
 */
@Data
public class OrderSubmitVo {

    // 收货地址
    private Long addrId;

    // 支付方式
    private Integer payType;

    // 无需提交需要购买的商品，去购物车再获取一遍
    // 优惠信息，发票
    // 用户相关信息直接去session中取

    // 应付价格
    private BigDecimal payPrice;

    // 防重令牌
    private String orderToken;

    // 备注信息
    private String note;
}
