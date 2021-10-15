package com.atguigu.gulimall.product.vo;

import lombok.Data;

import java.util.List;

/**
 * @author: CyS2020
 * @date: 2021/10/16
 */
@Data
public class SkuItemSaleAttrVo {

    private Long attrId;

    private String attrName;

    private List<String> attrValues;
}
