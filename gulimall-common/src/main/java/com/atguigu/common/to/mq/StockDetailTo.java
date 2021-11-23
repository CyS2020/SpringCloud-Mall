package com.atguigu.common.to.mq;

import lombok.Data;

/**
 * 库存工作单
 *
 * @author suchunyang
 * @email 1440870444@qq.com
 * @date 2021-08-17 22:26:12
 */

@Data
public class StockDetailTo {

    /**
     * id
     */
    private Long id;
    /**
     * sku_id
     */
    private Long skuId;
    /**
     * sku_name
     */
    private String skuName;
    /**
     * 购买个数
     */
    private Integer skuNum;
    /**
     * 工作单id
     */
    private Long taskId;

    /**
     * 仓库id
     */
    private Long wareId;

    /**
     * 锁定状态
     */
    private Integer lockStatus;

}
