package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.util.List;

/**
 * @author: CyS2020
 * @date: 2021/11/17
 */

@Data
public class WareSkuLockVo {

    private String orderSn;

    private List<OrderItemVo> locks;
}
