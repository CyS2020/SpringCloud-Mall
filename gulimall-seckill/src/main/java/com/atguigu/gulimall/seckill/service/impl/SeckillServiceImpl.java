package com.atguigu.gulimall.seckill.service.impl;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.mq.SeckillOrderTo;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.seckill.feign.CouponFeignService;
import com.atguigu.gulimall.seckill.feign.ProductFeignService;
import com.atguigu.gulimall.seckill.intercepter.LoginUserInterceptor;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SecKillSkuRedisTo;
import com.atguigu.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.atguigu.gulimall.seckill.vo.SeckillSkuVo;
import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author: CyS2020
 * @date: 2021/11/30
 */
@Slf4j
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

    @Autowired
    private RabbitTemplate rabbitTemplate;


    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";

    private final String SKUKILL_CACHE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:"; // ??????????????????

    @Override
    public void uploadSeckillSkuLatest3Days() {
        R session = couponFeignService.getLatest3DaySession();
        if (session.getCode() == 0) {
            // ??????????????????
            List<SeckillSessionsWithSkus> sessionData = session.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            // ??????????????????
            saveSessionInfos(sessionData);
            // ??????????????????
            saveSessionSkuInfos(sessionData);
        }
    }

    public List<SecKillSkuRedisTo> blockHandler(BlockException e) {
        log.info("getCurrentSeckillSkusResource??????????????????...");
        return null;
    }

    @SentinelResource(value = "getCurrentSeckillSkusResource", blockHandler = "blockHandler")
    @Override
    public List<SecKillSkuRedisTo> getCurrentSeckillSkus() {
        // 1. ??????????????????????????????????????????
        long time = new Date().getTime();

        try (Entry entry = SphU.entry("SeckillSkus")) {
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
                    // ???????????????????????????????????????????????????
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
        } catch (BlockException e) {
            log.error("???????????????...{}", e.getMessage());
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

    /**
     * TODO ????????????????????????????????????
     * TODO ??????????????????, ?????????????????????????????????????????????
     */
    @Override
    public String kill(String killId, String key, Integer num) {
        long start = System.currentTimeMillis();

        MemberRespVo respVo = LoginUserInterceptor.loginUser.get();
        // ???????????????????????????????????????
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String json = hashOps.get(killId);
        if (!StringUtils.isEmpty(json)) {
            Gson gson = new Gson();
            SecKillSkuRedisTo skuRedisTo = gson.fromJson(json, SecKillSkuRedisTo.class);
            // ???????????????
            // 1. ?????????????????????
            Long startTime = skuRedisTo.getStartTime();
            Long endTime = skuRedisTo.getEndTime();
            long currentTime = new Date().getTime();
            long ttl = endTime - currentTime;
            if (startTime <= currentTime && currentTime <= endTime) {
                // 2. ????????????????????????
                String randomCode = skuRedisTo.getRandomCode();
                String skuId = skuRedisTo.getPromotionSessionId() + "_" + skuRedisTo.getSkuId();
                if (randomCode.equals(key) && killId.equals(skuId)) {
                    // 3. ??????????????????
                    if (num <= skuRedisTo.getSeckillLimit()) {
                        // 4. ??????????????? userId_sessionId_skuId, ??????????????????
                        String redisKey = respVo.getId() + "_" + skuId;
                        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if (aBoolean != null && aBoolean) {
                            // 5. ????????????????????????
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + key);
                            boolean b = semaphore.tryAcquire(num);
                            if (b) {
                                //????????????, ????????????, ??????MQ??????
                                String orderSn = IdWorker.getTimeId();
                                SeckillOrderTo orderTo = new SeckillOrderTo();
                                orderTo.setOrderSn(orderSn);
                                orderTo.setMemberId(respVo.getId());
                                orderTo.setNum(num);
                                orderTo.setPromotionSessionId(skuRedisTo.getPromotionSessionId());
                                orderTo.setSkuId(skuRedisTo.getSkuId());
                                orderTo.setSeckillPrice(skuRedisTo.getSeckillPrice());
                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", orderTo);
                                long end = System.currentTimeMillis();
                                log.info("??????????????????... : {} ms", end - start);
                                return orderSn;
                            }
                        }
                    }
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
            // ??????????????????
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
                    // 1. sku????????????
                    R skuInfo = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
                    if (skuInfo.getCode() == 0) {
                        SkuInfoVo info = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfo(info);
                    }
                    // 2. sku????????????
                    BeanUtils.copyProperties(seckillSkuVo, redisTo);
                    // 3. ???????????????????????????????????????
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());
                    // 4. ?????????
                    redisTo.setRandomCode(token);
                    // 5. ???????????????--??????
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
