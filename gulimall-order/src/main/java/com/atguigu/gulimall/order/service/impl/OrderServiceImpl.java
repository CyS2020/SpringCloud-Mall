package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.OrderConstant;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.enume.OrderStatusEnum;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WareFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.to.OrderItemTo;
import com.atguigu.gulimall.order.vo.FareVo;
import com.atguigu.gulimall.order.vo.MemberAddressVo;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderItemVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.SkuHasStockVo;
import com.atguigu.gulimall.order.vo.SpuInfoVo;
import com.atguigu.gulimall.order.vo.SubmitOrderRespVo;
import com.atguigu.gulimall.order.vo.WareSkuLockVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> submitVo = new ThreadLocal<>();

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private WareFeignService wareFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        OrderConfirmVo confirmVo = new OrderConfirmVo();

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddressTask = CompletableFuture.runAsync(() -> {
            // 1. 远程查询收货地址
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());
            confirmVo.setAddress(address);
        }, executor);

        CompletableFuture<Void> getCartItemsTask = CompletableFuture.runAsync(() -> {
            // 2. 远程查询购物车所有选中的选项
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(items);
        }, executor).thenRunAsync(() -> {
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> itemIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            R hasStock = wareFeignService.getSkusHasStock(itemIds);
            List<SkuHasStockVo> data = hasStock.getData(new TypeReference<List<SkuHasStockVo>>() {
            });
            if (data != null) {
                Map<Long, Boolean> stocks = data.stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::getHasStock));
                confirmVo.setStocks(stocks);
            }
        }, executor);

        // 3. 查询用户积分
        Integer integration = memberRespVo.getIntegration();
        confirmVo.setIntegration(integration);

        // 4. 其他数据自动计算
        // 5. 防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId(), token, 30, TimeUnit.SECONDS);
        confirmVo.setOrderToken(token);

        CompletableFuture.allOf(getAddressTask, getCartItemsTask).get();
        return confirmVo;
    }

    //    @GlobalTransactional
    @Transactional
    @Override
    public SubmitOrderRespVo submitOrder(OrderSubmitVo vo) {
        submitVo.set(vo);
        SubmitOrderRespVo respVo = new SubmitOrderRespVo();
        respVo.setCode(0);

        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        String orderToken = vo.getOrderToken();
        // 执行lua脚本
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long val = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                Lists.newArrayList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()), orderToken);

        if (val == null || val == 0L) {
            // 令牌验证不通过
            respVo.setCode(1);
            return respVo;
        }
        // 创建订单
        OrderCreateTo order = createOrder();
        // 金额对比
        BigDecimal payPriceNow = order.getPayPrice();
        BigDecimal payPriceOrigin = vo.getPayPrice();
        if (Math.abs(payPriceNow.subtract(payPriceOrigin).doubleValue()) > 0.01) {
            // 验价不成功
            respVo.setCode(2);
            return respVo;
        }
        // 保存到订单
        saveOrder(order);
        // 锁定库存 订单号，订单项skuId, skuName, num
        WareSkuLockVo lockVo = new WareSkuLockVo();
        lockVo.setOrderSn(order.getOrder().getOrderSn());
        List<OrderItemTo> itemVos = order.getOrderItems().stream().map(item -> {
            OrderItemTo itemVo = new OrderItemTo();
            itemVo.setSkuId(item.getSkuId());
            itemVo.setCount(item.getSkuQuantity());
            itemVo.setTitle(item.getSkuName());
            return itemVo;
        }).collect(Collectors.toList());
        lockVo.setLocks(itemVos);
        // 远程锁库存
        R r = wareFeignService.orderLockStock(lockVo);
        if (r.getCode() != 0) {
            // 库存锁定失败
            respVo.setCode(2);
            return respVo;
        }
        int i = 10 / 0;
        respVo.setOrder(order.getOrder());
        return respVo;
    }

    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        orderEntity.setCreateTime(new Date());
        this.save(orderEntity);

        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    public OrderCreateTo createOrder() {
        OrderCreateTo createTo = new OrderCreateTo();
        // 1. 生成订单号
        String orderSn = IdWorker.getTimeId();
        OrderEntity orderEntity = buildOrder(orderSn);
        createTo.setOrder(orderEntity);
        // 2. 获取所有订单项
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);
        // 3. 计算价格与积分
        computePrice(orderEntity, itemEntities);
        createTo.setOrderItems(itemEntities);
        createTo.setFare(orderEntity.getFreightAmount());
        createTo.setPayPrice(orderEntity.getPayAmount());
        return createTo;
    }

    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        BigDecimal totalPrice = itemEntities.stream().map(OrderItemEntity::getRealAmount).reduce(new BigDecimal("0"), BigDecimal::add);
        BigDecimal promotionPrice = itemEntities.stream().map(OrderItemEntity::getPromotionAmount).reduce(new BigDecimal("0"), BigDecimal::add);
        BigDecimal couponPrice = itemEntities.stream().map(OrderItemEntity::getCouponAmount).reduce(new BigDecimal("0"), BigDecimal::add);
        BigDecimal integrationPrice = itemEntities.stream().map(OrderItemEntity::getIntegrationAmount).reduce(new BigDecimal("0"), BigDecimal::add);
        Integer giftGrowth = itemEntities.stream().map(OrderItemEntity::getGiftGrowth).reduce(0, Integer::sum);
        Integer giftIntegration = itemEntities.stream().map(OrderItemEntity::getGiftIntegration).reduce(0, Integer::sum);
        orderEntity.setTotalAmount(totalPrice);
        orderEntity.setPayAmount(totalPrice.add(orderEntity.getFreightAmount()));
        orderEntity.setPromotionAmount(promotionPrice);
        orderEntity.setCouponAmount(couponPrice);
        orderEntity.setIntegrationAmount(integrationPrice);
        orderEntity.setGrowth(giftGrowth);
        orderEntity.setIntegration(giftIntegration);
    }

    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if (currentUserCartItems != null && !currentUserCartItems.isEmpty()) {
            return currentUserCartItems.stream().map(cartItem -> buildOrderItem(orderSn, cartItem)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private OrderEntity buildOrder(String orderSn) {
        MemberRespVo respVo = LoginUserInterceptor.loginUser.get();
        // 创建订单
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        // 设置会员id
        entity.setMemberId(respVo.getId());
        // 获取收货地址信息
        OrderSubmitVo orderSubmitVo = submitVo.get();
        R fare = wareFeignService.getFare(orderSubmitVo.getAddrId());
        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
        });
        // 设置运费信息
        entity.setFreightAmount(fareResp.getFare());
        // 设置收货人信息
        entity.setReceiverProvince(fareResp.getAddress().getProvince());
        entity.setReceiverCity(fareResp.getAddress().getCity());
        entity.setReceiverRegion(fareResp.getAddress().getRegion());
        entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        entity.setReceiverPostCode(fareResp.getAddress().getPostCode());
        entity.setReceiverPhone(fareResp.getAddress().getPhone());
        entity.setReceiverName(fareResp.getAddress().getName());
        // 设置订单状态信息
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setAutoConfirmDay(7);
        entity.setDeleteStatus(0);
        return entity;
    }

    private OrderItemEntity buildOrderItem(String orderSn, OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        // 订单号信息
        itemEntity.setOrderSn(orderSn);
        // 商品spu信息
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfoVo = r.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(spuInfoVo.getId());
        itemEntity.setSpuBrand(spuInfoVo.getBrandId().toString());
        itemEntity.setSpuName(spuInfoVo.getSpuName());
        itemEntity.setCategoryId(spuInfoVo.getCatalogId());
        // 商品sku信息
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttr = String.join(";", cartItem.getSkuAttr());
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartItem.getCount());
        // 优惠信息
        BigDecimal count = new BigDecimal(itemEntity.getSkuQuantity().toString());
        // 积分信息
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(count).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(count).intValue());
        // 订单项的价格信息
        itemEntity.setPromotionAmount(new BigDecimal("0"));
        itemEntity.setCouponAmount(new BigDecimal("0"));
        itemEntity.setIntegrationAmount(new BigDecimal("0"));
        BigDecimal originPrice = itemEntity.getSkuPrice().multiply(count);
        BigDecimal realPrice = originPrice.subtract(itemEntity.getPromotionAmount())
                .subtract(itemEntity.getCouponAmount())
                .subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(realPrice);
        return itemEntity;
    }
}