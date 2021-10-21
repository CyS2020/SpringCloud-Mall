package com.atguigu.gulimall.member.exception;

/**
 * @author: CyS2020
 * @date: 2021/10/21
 */
public class UsernameExistException extends RuntimeException {

    public UsernameExistException() {
        super("用户名存在");
    }
}
