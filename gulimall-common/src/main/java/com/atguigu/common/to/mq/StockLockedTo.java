package com.atguigu.common.to.mq;

import lombok.Data;

/**
 * @author: CyS2020
 * @date: 2021/11/23
 */

@Data
public class StockLockedTo {

    /**
     * 库存工作单的id
     */
    private Long id;

    /**
     * 工作单详情的所有id
     */
    private StockDetailTo detailTo;

}
