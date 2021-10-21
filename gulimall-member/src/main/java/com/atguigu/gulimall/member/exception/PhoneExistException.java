package com.atguigu.gulimall.member.exception;

/**
 * @author: CyS2020
 * @date: 2021/10/21
 */
public class PhoneExistException extends RuntimeException {
    public PhoneExistException() {
        super("手机号存在");
    }
}
