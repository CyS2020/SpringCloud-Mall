package com.atguigu.gulimall.order.vo;

import com.atguigu.gulimall.order.to.OrderItemTo;
import lombok.Data;

import java.util.List;

/**
 * @author: CyS2020
 * @date: 2021/11/17
 */

@Data
public class WareSkuLockVo {

    private String orderSn;

    private List<OrderItemTo> locks;
}
