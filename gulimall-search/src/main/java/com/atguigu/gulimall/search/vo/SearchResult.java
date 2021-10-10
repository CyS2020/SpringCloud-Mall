package com.atguigu.gulimall.search.vo;

import com.atguigu.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.List;

/**
 * @author: CyS2020
 * @date: 2021/10/6
 * 描述：返回的结果数据
 */

@Data
public class SearchResult {

    private List<SkuEsModel> products;

    /**
     * 以下是分页信息
     */
    private Integer pageNum;

    private Long total;

    private Integer totalPages;

    /*
     * 当前查询结果, 所有涉及到的品牌
     */
    private List<BrandVo> brands;

    /**
     * 当前查询结果, 所涉及到的所有分类
     */
    private List<CatalogVo> catalogs;

    /**
     * 当前查询结果, 所有涉及到的属性
     */
    private List<AttrVo> attrs;

    @Data
    public static class BrandVo {

        private Long brandId;

        private String brandName;

        private String brandImg;
    }

    @Data
    public static class CatalogVo {

        private Long catalogId;

        private String catalogName;
    }

    @Data
    public static class AttrVo {

        private Long attrId;

        private String attrName;

        private List<String> attrValue;
    }
}
