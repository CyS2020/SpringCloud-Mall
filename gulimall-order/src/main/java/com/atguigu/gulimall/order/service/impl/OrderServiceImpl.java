package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.OrderConstant;
import com.atguigu.common.to.OrderTo;
import com.atguigu.common.to.mq.SeckillOrderTo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.entity.PaymentInfoEntity;
import com.atguigu.gulimall.order.enume.OrderStatusEnum;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WareFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.service.PaymentInfoService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.to.OrderItemTo;
import com.atguigu.gulimall.order.vo.FareVo;
import com.atguigu.gulimall.order.vo.MemberAddressVo;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderItemVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.PayAsyncVo;
import com.atguigu.gulimall.order.vo.PayVo;
import com.atguigu.gulimall.order.vo.SkuHasStockVo;
import com.atguigu.gulimall.order.vo.SpuInfoVo;
import com.atguigu.gulimall.order.vo.SubmitOrderRespVo;
import com.atguigu.gulimall.order.vo.WareSkuLockVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
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

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PaymentInfoService paymentInfoService;

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
            // 1. ????????????????????????
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());
            confirmVo.setAddress(address);
        }, executor);

        CompletableFuture<Void> getCartItemsTask = CompletableFuture.runAsync(() -> {
            // 2. ??????????????????????????????????????????
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

        // 3. ??????????????????
        Integer integration = memberRespVo.getIntegration();
        confirmVo.setIntegration(integration);

        // 4. ????????????????????????
        // 5. ????????????
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
        // ??????lua??????
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long val = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                Lists.newArrayList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()), orderToken);

        if (val == null || val == 0L) {
            // ?????????????????????
            respVo.setCode(1);
            return respVo;
        }
        // ????????????
        OrderCreateTo order = createOrder();
        // ????????????
        BigDecimal payPriceNow = order.getPayPrice();
        BigDecimal payPriceOrigin = vo.getPayPrice();
        if (Math.abs(payPriceNow.subtract(payPriceOrigin).doubleValue()) > 0.01) {
            // ???????????????
            respVo.setCode(2);
            return respVo;
        }
        // ???????????????
        saveOrder(order);
        // ???????????? ?????????????????????skuId, skuName, num
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
        // ???????????????
        R r = wareFeignService.orderLockStock(lockVo);
        if (r.getCode() != 0) {
            // ??????????????????
            respVo.setCode(2);
            return respVo;
        }
        // TODO ????????????????????????????????????MQ
        rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());
        respVo.setOrder(order.getOrder());
        return respVo;
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        return this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
    }

    @Override
    public void closeOrder(OrderEntity entity) {
        // ??????????????????????????????????????????
        OrderEntity orderEntity = this.getById(entity.getId());
        if (orderEntity.getStatus().equals(OrderStatusEnum.CREATE_NEW.getCode())) {
            OrderEntity update = new OrderEntity();
            update.setId(orderEntity.getId());
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);
            // ?????????????????????????????????
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity, orderTo);
            try {
                // TODO ?????????????????????????????????
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
            } catch (Exception e) {
                //TODO ???????????????????????????????????????????????????
            }
        }
    }

    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity order = this.getOrderByOrderSn(orderSn);
        BigDecimal payAmount = order.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(payAmount.toString());
        payVo.setOut_trade_no(order.getOrderSn());

        List<OrderItemEntity> itemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity itemEntity = itemEntities.get(0);
        payVo.setSubject(itemEntity.getSkuName());
        payVo.setBody(itemEntity.getSkuAttrsVals());
        return payVo;
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {

        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", memberRespVo.getId()).orderByDesc("id")
        );

        page.getRecords().forEach(order -> {
            List<OrderItemEntity> itemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setItemEntities(itemEntities);

        });
        return new PageUtils(page);
    }

    @Override
    public String handlePayResult(PayAsyncVo vo) {
        // ??????????????????
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setAlipayTradeNo(vo.getTrade_no());
        infoEntity.setOrderSn(vo.getOut_trade_no());
        infoEntity.setPaymentStatus(vo.getTrade_status());
        infoEntity.setCallbackTime(vo.getNotify_time());
        paymentInfoService.save(infoEntity);

        // ??????????????????
        String tradeStatus = vo.getTrade_status();
        boolean payed = tradeStatus.equals("TRADE_SUCCESS") || tradeStatus.equals("TRADE_FINISHED");
        if (payed) {
            String orderSn = vo.getOut_trade_no();
            this.baseMapper.updateOrderStatus(orderSn, OrderStatusEnum.PAYED.getCode());

        }
        return "success";
    }

    @Override
    public void createSeckillOrder(SeckillOrderTo seckillOrder) {
        // TODO ??????????????????
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(seckillOrder.getOrderSn());
        orderEntity.setMemberId(seckillOrder.getMemberId());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        BigDecimal totalPrice = seckillOrder.getSeckillPrice().multiply(new BigDecimal(seckillOrder.getNum().toString()));
        orderEntity.setPayAmount(totalPrice);
        this.save(orderEntity);

        // TODO ?????????????????????
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrderSn(seckillOrder.getOrderSn());
        orderItemEntity.setRealAmount(totalPrice);
        orderItemEntity.setSkuQuantity(seckillOrder.getNum());
        // TODO ????????????sku???????????????????????? productFeignService.getSpuInfoBySkuId()
        orderItemService.save(orderItemEntity);
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
        // 1. ???????????????
        String orderSn = IdWorker.getTimeId();
        OrderEntity orderEntity = buildOrder(orderSn);
        createTo.setOrder(orderEntity);
        // 2. ?????????????????????
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);
        // 3. ?????????????????????
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
        // ????????????
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        // ????????????id
        entity.setMemberId(respVo.getId());
        // ????????????????????????
        OrderSubmitVo orderSubmitVo = submitVo.get();
        R fare = wareFeignService.getFare(orderSubmitVo.getAddrId());
        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
        });
        // ??????????????????
        entity.setFreightAmount(fareResp.getFare());
        // ?????????????????????
        entity.setReceiverProvince(fareResp.getAddress().getProvince());
        entity.setReceiverCity(fareResp.getAddress().getCity());
        entity.setReceiverRegion(fareResp.getAddress().getRegion());
        entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        entity.setReceiverPostCode(fareResp.getAddress().getPostCode());
        entity.setReceiverPhone(fareResp.getAddress().getPhone());
        entity.setReceiverName(fareResp.getAddress().getName());
        // ????????????????????????
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setAutoConfirmDay(7);
        entity.setDeleteStatus(0);
        return entity;
    }

    private OrderItemEntity buildOrderItem(String orderSn, OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        // ???????????????
        itemEntity.setOrderSn(orderSn);
        // ??????spu??????
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfoVo = r.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(spuInfoVo.getId());
        itemEntity.setSpuBrand(spuInfoVo.getBrandId().toString());
        itemEntity.setSpuName(spuInfoVo.getSpuName());
        itemEntity.setCategoryId(spuInfoVo.getCatalogId());
        // ??????sku??????
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttr = String.join(";", cartItem.getSkuAttr());
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartItem.getCount());
        // ????????????
        BigDecimal count = new BigDecimal(itemEntity.getSkuQuantity().toString());
        // ????????????
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(count).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(count).intValue());
        // ????????????????????????
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