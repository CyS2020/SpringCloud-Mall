package com.atguigu.gulimall.product.vo;

import lombok.Data;

import java.util.List;

/**
 * @author: CyS2020
 * @date: 2021/10/16
 */
@Data
public class SpuItemAttrGroupVo {

    private String groupName;

    private List<Attr> attrs;
}
