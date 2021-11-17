package com.atguigu.gulimall.product.service;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.SpuInfoEntity;
import com.atguigu.gulimall.product.vo.SpuSaveVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * spu信息
 *
 * @author suchunyang
 * @email 1440870444@qq.com
 * @date 2021-08-15 23:49:04
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSpuInfo(SpuSaveVo vo);

    void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity);

    PageUtils queryPageByCondition(Map<String, Object> params);

    /**
     * 商品上架
     */
    void up(Long spuId);

    SpuInfoEntity getSpuInfoBySkuId(Long skuId);
}

