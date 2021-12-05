package com.atguigu.common.to.mq;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author: CyS2020
 * @date: 2021/12/5
 */
@Data
public class SeckillOrderTo {
    /**
     * 订单号
     */
    private String orderSn;
    /**
     * 活动场次id
     */
    private Long promotionSessionId;
    /**
     * 商品id
     */
    private Long skuId;
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    /**
     * 秒杀总量
     */
    private Integer num;
    /**
     * 会员id
     */
    private Long memberId;
}
