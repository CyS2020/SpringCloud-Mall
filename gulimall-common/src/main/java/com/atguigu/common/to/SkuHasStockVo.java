package com.atguigu.common.to;

import lombok.Data;

/**
 * @author: CyS2020
 * @date: 2021/10/3
 * 描述：远程调用ware查询有无库存
 */

@Data
public class SkuHasStockVo {

    private Long skuId;

    private Boolean hasStock;
}
