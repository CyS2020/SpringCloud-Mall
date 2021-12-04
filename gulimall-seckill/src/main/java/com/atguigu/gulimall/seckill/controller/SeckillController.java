package com.atguigu.gulimall.seckill.controller;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SecKillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author: CyS2020
 * @date: 2021/12/2
 */
@RestController
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @GetMapping("/currentSeckillSkus")
    public R getCurrentSeckillSkus() {
        List<SecKillSkuRedisTo> vos = seckillService.getCurrentSeckillSkus();
        return R.ok().setData(vos);
    }

    @GetMapping("/sku/seckill/{skuId}")
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId) {
        SecKillSkuRedisTo secKillSkuRedisTo = seckillService.getSkuSeckillInfo(skuId);
        return R.ok().setData(secKillSkuRedisTo);
    }

    @GetMapping("/kill")
    public R seckill(@RequestParam("killId") String killId, @RequestParam("key") String key, @RequestParam("num") Integer num) {
//        SecKillSkuRedisTo secKillSkuRedisTo = seckillService.getSkuSeckillInfo(skuId);

        return R.ok();
    }
}
