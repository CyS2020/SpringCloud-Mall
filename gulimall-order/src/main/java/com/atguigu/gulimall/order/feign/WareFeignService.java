package com.atguigu.gulimall.order.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author: CyS2020
 * @date: 2021/11/15
 */

@FeignClient("gulimall-ware")
public interface WareFeignService {

    @PostMapping("ware/waresku/hasstock")
    R getSkusHasStock(@RequestBody List<Long> skuIds);
}

