package com.atguigu.gulimall.thirdparty.component;

import com.atguigu.gulimall.thirdparty.util.HttpUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: CyS2020
 * @date: 2021/10/20
 */
@ConfigurationProperties(prefix = "spring.cloud.alicloud.sms")
@Data
@Component
@Slf4j
public class SmsComponent {


    private String host;

    private String path;

    private String appcode;

    private String templateId;

    public void sendSmsCode(String phone, String code) {
        String method = "GET";
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> query = new HashMap<>();
        query.put("mobile", phone);
        query.put("param", code);
        query.put("templateId", templateId);

        try {
            HttpResponse response = HttpUtils.doGet(host, path, method, headers, query);
            log.info(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

