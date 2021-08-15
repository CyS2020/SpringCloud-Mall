package com.atguigu.gulimall.product.service;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.SpuImagesEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * spu图片
 *
 * @author suchunyang
 * @email 1440870444@qq.com
 * @date 2021-08-15 23:49:04
 */
public interface SpuImagesService extends IService<SpuImagesEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

