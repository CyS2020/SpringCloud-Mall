package com.atguigu.gulimall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * @author: CyS2020
 * @date: 2021/10/6
 * 描述：前端传来的入参, 所有可能的入参
 * catalog3Id=225&keyword=小米&sort=saleCount_asc&hasStock=1&brandId=1&brand=2&attr=1_IOS:安卓:其他&attr=2_5寸:6寸
 */

@Data
public class SearchParam {

    private String keyword; // 页面传递过来的全文匹配关键字

    private Long catalog3Id; // 三级分类Id

    /**
     * 排序条件
     * sort=saleCount_asc/desc
     * sort=skuPrice_asc/desc
     * sort=hotScore_asc/desc
     */
    private String sort; // 排序

    /**
     * 过滤条件
     * hasStock(是否有货)、skuPrice区间、BrandId、Catalog3Id、attrs
     * hasStock=0/1
     * skuPrice=1_500/_500/500_
     * brandId=1
     * attr=2_5寸:6寸
     */
    private Integer hasStock; // 是否显示有货

    private String skuPrice; // 价格区间

    private List<Long> brandId; // 品牌

    private List<String> attrs; //按照属性进行筛选

    private Integer pageNum; // 页码
}
