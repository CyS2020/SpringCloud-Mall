package com.atguigu.gulimall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gulimall.order.vo.PayVo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private String app_id = "2021000118654949";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private String merchant_private_key = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCi8P0FdIzKNAAASAbSjVqIP3lO/7N6h5dYLkRJV/SWXEfPtcYliaNkEYfrEh05L0Lw+gCdA0k4qGIxjm7y2HELW6NmNj3JYxqCV+Gwequz3UBWo3i4DTSKO9pDQGctr+76I1/zfeATI07MyrKHfiQ5S5mad6xeIu1qUsjalBn9bc3PJNM8cvhQ/HMlzkEco1GR9uM8g7ovk18xbwP7sjkfEWDOkmK1u8Zg9AOTie9548CyIJF1NiKHKVlDelAfxE9Kd/89SLV36W1gp0MpQ55Qb0Mwwo1hYxtdrjcTIifTMEAkCMsxDZSPmhOti/2/B93jV/O6KKZGeaae4qAzPMQZAgMBAAECggEBAKH4H+veUV1ml2GwN2fxLz8kqXeH4mc4hY8YXrTxg6Y7kP6+WHtT+GEutAdN+FKx0j8spJNvgseZShKs3Uj1Mlnx+9lMrdqUcvOCXExLhIbEtoHo+cUrC0EmAe68BFR8AHgR1f168NTVUi1siE77axKxPb04nNTur0XziMkOU0+A0CI59P3lffAv1Us3uGFWhlOYSdZumxQH/EpvbbUJW5U4NJnZly7PFwH00qSN2ClPGRJLurRrqVSwMDePVUQ285DUq8PHvGaGrpKGsYnHxuXyI1PYdauKldJu4Q4SqV8dZBPVUCfoBLb9+pUlNvErsR0kvAcXYYg+M8pr6xcJFaECgYEA5eebGxe/z6+pr/NpwzqOaLTEKnoIgpX0BOWU0yC0//+1zzj0NluqIOcMUbN0ICDjaKiSEcIAF8RnnnmjlcXpBjK4pQq29sS4SW4K0hIj3pdP39ukjN9kGhS/MYCoovKg2p0O/ou4H1qP6k/uS/hvJLg/Bs4S5vDTj/uZbbflj5UCgYEAtW+c7jEvsoW27lEIJ52ZrXaVyD1X8lt/paxDWOmMVoeUjpT9BdD2gE47VLC/NLI/Cy0RzLpopBgAEtxDPtgneWGHhIqgZNYHINitq0VIwYQhn3a9b+Gqfy2013NCPzqt7NflsASAQ8zCmsdE2lgDn72GJiVvtcNxnqvmnMhUUXUCgYB74csQVEHFNZCoAO+yhTELdqxlfxBq3UZ6BQOmqG8fqrhbCwN87vPDevyGYb5nOZGQaZUcAH9wTyLOoBjnsZcZAefA9v8UBXiQCL3H6IJvhPVDWOVYC1+zNg64K+2ysC7A3fSgcMUsD+6QgbWUNjAdhzJfBlflHUvCv/4ywu7t6QKBgCY9mRDnEwuIfU7ri1F2OS7DYLxsmX2+ZUAQ97zktKyENP58TnwMV/ghBQZLnnFH5FvBqw/Adk+ns1RGUnILcv51XO/FnBrEtYnpwWjo2HXZGJEYoMLVCG93vbbvVxYmkwPpWULpH/OqU5X1zABNLq5bbsvZdhsUT31G8/s1ifgFAoGBAOCrXNCCIWYWRS5B/liTsQZ6sfZjpSmz/QvtLwpwsYsExr9NDd98/3WzeECX/qSVtptBSqIa1QHVVgrJLQzQWjoIp87XvpyiVv1cKBAr5M2dUgPtDv/7q8cws4S21x7IzX+1xYVTyfB7bTlTDSOyGNwSaD43Vo49DJR/1hWQWJR/";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAr5It+EgOE/ZW/xELA+TTTYFJNOCKi+wTvzGRjTz+BWiJSxSn0QmKfly6sS8ilwZSIP+pJ33nmOUaLhMkQOrO2EhP7/b/e65j+Nm68+RMkmVFnxeMszAsd1Bq1SOykC0pbXk7XuKnReqpo/iwXscWKlo4s5pylNMA0V0Us+EnGh5+AIIf7HMi+Xb6VcZYaPMnsqLK6Vthi0ilsHJ3zDD5sb/DPBaybHxIVjvykbyDgCZ4UJU+4FgybEvxQHzAOfSkrk1VPmPb6jY/IsOnwrCyH2cCDP6Ls0Irj/iHTHM+3Fb6nRAiXqBr5IIsQN7EMbijs5BhTXCdSR3wWyI1nhXzBwIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private String notify_url = "http://cys-mall.natapp1.cc/payed/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private String return_url = "http://member.gulimall.com/memberOrder.html";

    // 签名方式
    private String sign_type = "RSA2";

    // 字符编码格式
    private String charset = "utf-8";

    private String timeout = "30m";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\"" + out_trade_no + "\","
                + "\"total_amount\":\"" + total_amount + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"body\":\"" + body + "\","
                + "\"timeout_express\":\"" + timeout + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        log.info("支付宝的响应：" + result);

        return result;

    }
}
