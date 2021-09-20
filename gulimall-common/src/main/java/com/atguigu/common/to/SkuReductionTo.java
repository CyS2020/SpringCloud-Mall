package com.atguigu.common.to;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author: CyS2020
 * @date: 2021/9/20
 * 描述：sku的满减传输对象
 */
@Data
public class SkuReductionTo {

    private Long skuId;

    private int fullCount;

    private BigDecimal discount;

    private int countStatus;

    private BigDecimal fullPrice;

    private BigDecimal reducePrice;

    private int priceStatus;

    private List<MemberPrice> memberPrice;
}
