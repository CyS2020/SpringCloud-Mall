package com.atguigu.gulimall.product.feign.fallback;

import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.feign.SeckillFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: CyS2020
 * @date: 2021/12/5
 */
@Slf4j
@Component
public class SeckillFeignServiceFallback implements SeckillFeignService {

    @Override
    public R getSkuSeckillInfo(Long skuId) {
        log.info("熔断方法调用...getSkuSeckillInfo");
        return R.error(BizCodeEnum.TOO_MANY_REQUEST.getCode(), BizCodeEnum.TOO_MANY_REQUEST.getMsg());
    }
}
