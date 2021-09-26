package com.atguigu.gulimall.ware.vo;

import lombok.Data;

/**
 * @author: CyS2020
 * @date: 2021/9/24
 * 描述：
 */
@Data
public class PurchaseDoneItemVo {

    private Long itemId;

    private Integer status;

    private String reason;
}
