package com.atguigu.gulimall.seckill.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.seckill.feign.CouponFeignService;
import com.atguigu.gulimall.seckill.feign.ProductFeignService;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SecKillSkuRedisTo;
import com.atguigu.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.atguigu.gulimall.seckill.vo.SeckillSkuVo;
import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import com.google.gson.Gson;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author: CyS2020
 * @date: 2021/11/30
 */
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private RedissonClient redissonClient;


    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";

    private final String SKUKILL_CACHE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:"; // 加商品随机码

    @Override
    public void uploadSeckillSkuLatest3Days() {
        R session = couponFeignService.getLatest3DaySession();
        if (session.getCode() == 0) {
            // 上架商品信息
            List<SeckillSessionsWithSkus> sessionData = session.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            // 缓存活动信息
            saveSessionInfos(sessionData);
            // 缓存商品信息
            saveSessionSkuInfos(sessionData);
        }
    }

    @Override
    public List<SecKillSkuRedisTo> getCurrentSeckillSkus() {
        // 1. 确定当前时间属于哪个秒杀场次
        long time = new Date().getTime();
        Set<String> keys = redisTemplate.keys(SESSION_CACHE_PREFIX + "*");
        if (keys == null) {
            return null;
        }
        for (String key : keys) {
            String replace = key.replace(SESSION_CACHE_PREFIX, "");
            String[] s = replace.split("_");
            long start = Long.parseLong(s[0]);
            long end = Long.parseLong(s[1]);
            if (start <= time && time <= end) {
                // 获取这个秒杀场次需要的所有商品信息
                List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                if (range == null) {
                    break;
                }
                BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                List<String> list = ops.multiGet(range);
                if (list != null) {
                    return list.stream().map(item -> {
                        Gson gson = new Gson();
                        return gson.fromJson(item, SecKillSkuRedisTo.class);
                    }).collect(Collectors.toList());
                }
                break;
            }
        }
        return null;
    }

    @Override
    public SecKillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (keys != null && !keys.isEmpty()) {
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                if (Pattern.matches(regx, key)) {
                    String json = hashOps.get(key);
                    Gson gson = new Gson();
                    SecKillSkuRedisTo skuRedisTo = gson.fromJson(json, SecKillSkuRedisTo.class);
                    Long startTime = skuRedisTo.getStartTime();
                    Long endTime = skuRedisTo.getEndTime();
                    long currentTime = new Date().getTime();
                    if (startTime > currentTime || currentTime > endTime) {
                        skuRedisTo.setRandomCode(null);
                    }
                    return skuRedisTo;
                }
            }
        }
        return null;
    }

    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.forEach(session -> {
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();
            String key = SESSION_CACHE_PREFIX + startTime + "_" + endTime;
            // 缓存活动信息
            Boolean hasKey = redisTemplate.hasKey(key);
            if (hasKey != null && !hasKey) {
                List<String> collect = session.getRelationSkus().stream().map(item -> item.getPromotionSessionId() + "_" + item.getSkuId()).collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key, collect);
            }
        });
    }

    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.forEach(session -> {
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            String token = UUID.randomUUID().toString().replace("-", "");
            for (SeckillSkuVo seckillSkuVo : session.getRelationSkus()) {
                Boolean hasKey = ops.hasKey(seckillSkuVo.getPromotionSessionId() + "_" + seckillSkuVo.getSkuId());
                if (hasKey != null && !hasKey) {
                    SecKillSkuRedisTo redisTo = new SecKillSkuRedisTo();
                    // 1. sku基本数据
                    R skuInfo = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
                    if (skuInfo.getCode() == 0) {
                        SkuInfoVo info = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfo(info);
                    }
                    // 2. sku秒杀信息
                    BeanUtils.copyProperties(seckillSkuVo, redisTo);
                    // 3. 设置上当前商品秒杀时间信息
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());
                    // 4. 随机码
                    redisTo.setRandomCode(token);
                    // 5. 设置信号量--库存
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    semaphore.trySetPermits(seckillSkuVo.getSeckillCount());

                    Gson gson = new Gson();
                    String jsonString = gson.toJson(redisTo);
                    ops.put(seckillSkuVo.getPromotionSessionId() + "_" + seckillSkuVo.getSkuId(), jsonString);
                }
            }
        });
    }
}
