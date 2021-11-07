package com.atguigu.gulimall.cart.service;

import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;

import java.util.concurrent.ExecutionException;

/**
 * @author: CyS2020
 * @date: 2021/11/7
 * 描述：购物车服务
 */
public interface CartService {

    /**
     * 将商品添加到购物车
     */
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    /**
     * 获取购物车的商品
     */
    CartItem getCartItem(Long skuId);

    /**
     * 获取购物车
     */
    Cart getCart() throws ExecutionException, InterruptedException;

    /**
     * 清空购物车数据
     */
    void clearCart(String cartKey);

    /**
     * 勾选购物项
     */
    void checkItem(Long skuId, Integer check);

    /**
     * 修改购物项数量
     */
    void changeItemCount(Long skuId, Integer num);

    /**
     * 删除购物项
     */
    void deleteItem(Long skuId);
}
