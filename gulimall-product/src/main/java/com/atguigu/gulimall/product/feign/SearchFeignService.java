package com.atguigu.gulimall.product.feign;

import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author: CyS2020
 * @date: 2021/10/3
 * 描述：调用es服务存储信息
 */

@FeignClient("gulimall-search")
public interface SearchFeignService {

    @PostMapping("search/save/product")
    R productStatusUp(@RequestBody List<SkuEsModel> skuEsModels);
}
