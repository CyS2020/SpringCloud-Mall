package com.atguigu.gulimall.product.vo;

import com.atguigu.gulimall.product.entity.AttrEntity;
import lombok.Data;

import java.util.List;

/**
 * @author: CyS2020
 * @date: 2021/9/20
 * 描述：属性分组及属性分组下的属性信息
 */
@Data
public class AttrGroupWithAttrsVo {

    /**
     * 分组id
     */
    private Long attrGroupId;
    /**
     * 组名
     */
    private String attrGroupName;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 描述
     */
    private String descript;
    /**
     * 组图标
     */
    private String icon;
    /**
     * 所属分类id
     */
    private Long catelogId;

    /**
     * 该属性分组下的所有属性
     */
    private List<AttrEntity> attrs;
}
