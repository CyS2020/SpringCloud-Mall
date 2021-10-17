package com.atguigu.gulimall.product.vo;

import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

/**
 * @author: CyS2020
 * @date: 2021/10/15
 * 描述：详情页需要的对象
 */
@Data
public class SkuItemVo {

    // 1. sku基本信息 pms_sku_info表
    private SkuInfoEntity info;

    // 2. sku的图片信息 pms_sku_images
    private List<SkuImagesEntity> images;

    // 3. spu的销售属性组合
    private List<SkuItemSaleAttrVo> saleAttr;

    // 4. spu的介绍 pms_spu_info_desc
    private SpuInfoDescEntity desp;

    // 5. spu规格参数
    private List<SpuItemAttrGroupVo> groupAttrs;

    // 6. 有货无货信息
    private boolean hasStock = true;
}
