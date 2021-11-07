package com.atguigu.common.to;

import lombok.Data;

/**
 * @author: CyS2020
 * @date: 2021/11/7
 */
@Data
public class UserInfoTo {

    private Long userId;

    private String userKey;

    private boolean tempUser = false;
}
