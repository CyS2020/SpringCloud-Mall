package com.atguigu.gulimall.order.vo;

import com.atguigu.gulimall.order.entity.OrderEntity;
import lombok.Data;

/**
 * @author: CyS2020
 * @date: 2021/11/16
 */
@Data
public class SubmitOrderRespVo {

    private OrderEntity order;

    // 0 成功，错误码
    private Integer code;
}
