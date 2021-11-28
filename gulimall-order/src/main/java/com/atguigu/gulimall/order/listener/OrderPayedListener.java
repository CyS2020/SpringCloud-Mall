package com.atguigu.gulimall.order.listener;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gulimall.order.config.AlipayTemplate;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.PayAsyncVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: CyS2020
 * @date: 2021/11/28
 */
@Slf4j
@RestController
public class OrderPayedListener {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @PostMapping("/payed/notify")
    public String handleAlipayed(PayAsyncVo vo, HttpServletRequest request) throws AlipayApiException {
        // 只要收到了支付宝的异步通知，告诉我们订单支付成功，返回success，支付宝就不在通知
        log.info("【支付宝异步通知消息】: {}", vo);
        // 验签
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }

        boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayTemplate.getAlipay_public_key(),
                alipayTemplate.getCharset(), alipayTemplate.getSign_type()); //调用SDK验证签名
        if (signVerified) {
            log.info("【支付宝异步通知签名验证成功】");
            return orderService.handlePayResult(vo);
        }
        return "success";
    }
}
