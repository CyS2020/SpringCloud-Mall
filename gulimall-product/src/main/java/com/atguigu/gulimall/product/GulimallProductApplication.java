package com.atguigu.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 1. 整个Mybatis-plus依赖
 * 1) 导入依赖
 * <dependency>
 * <groupId>com.baomidou</groupId>
 * <artifactId>mybatis-plus-boot-starter</artifactId>
 * <version>3.2.0</version>
 * </dependency>
 * 2) 配置
 * - 导入mysql驱动的依赖，放在gulimall-common模块了
 * - 在application.yml配置mysql数据源相关信息
 * - 配置mybatis-plus: 配置@MapperScan扫描的包路径
 * - 配置mybatis-plus: 配置classpath扫描的xml文件路径
 */
@EnableFeignClients(basePackages = "com.atguigu.gulimall.product.feign")
@MapperScan("com.atguigu.gulimall.product.dao")
@SpringBootApplication
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
