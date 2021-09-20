package com.atguigu.common.to;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author: CyS2020
 * @date: 2021/9/20
 * 描述：传输对象，用于product与coupon模块传输
 */
@Data
public class SpuBoundTo {

    private Long spuId;

    private BigDecimal buyBounds;

    private BigDecimal growBounds;
}
