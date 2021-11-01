package com.atguigu.gulimall.auth.vo;

import lombok.Data;

/**
 * @author: CyS2020
 * @date: 2021/11/1
 */
@Data
public class SocialUser {

    private String access_token;

    private String remind_in;

    private long expires_in;

    private String uid;

    private String isRealName;
}
