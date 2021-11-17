package com.atguigu.gulimall.order.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author: CyS2020
 * @date: 2021/11/17
 */

@FeignClient("gulimall-product")
public interface ProductFeignService {

    @GetMapping("product/spuinfo/skuId/{id}")
    R getSpuInfoBySkuId(@PathVariable("id") Long skuId);
}
