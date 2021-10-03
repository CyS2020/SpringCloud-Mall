package com.atguigu.gulimall.product.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author: CyS2020
 * @date: 2021/10/3
 * 描述：调用库存系统查库存
 */

@FeignClient("gulimall-ware")
public interface WareFeignService {

    @PostMapping("ware/waresku/hasstock")
    R getSkusHasStock(@RequestBody List<Long> skuIds);
}
