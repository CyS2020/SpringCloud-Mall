package com.atguigu.gulimall.ware.exception;

/**
 * @author: CyS2020
 * @date: 2021/11/17
 */
public class NoStockException extends RuntimeException {

    public NoStockException(Long skuId) {
        super("商品id:" + skuId + "没有足够的库存了");
    }
}
