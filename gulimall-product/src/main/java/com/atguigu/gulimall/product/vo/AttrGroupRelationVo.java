package com.atguigu.gulimall.product.vo;

import lombok.Data;

/**
 * @author: CyS2020
 * @date: 2021/9/18
 * 描述：分组关联关系对象
 */
@Data
public class AttrGroupRelationVo {

    //[{"attrId":1,"attrGroupId":2}]
    private Long attrId;

    private Long attrGroupId;

}
