package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author: CyS2020
 * @date: 2021/11/15
 */

@Data
public class FareVo {

    private MemberAddressVo address;

    private BigDecimal fare;
}
