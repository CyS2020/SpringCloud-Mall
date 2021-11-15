package com.atguigu.gulimall.ware.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author: CyS2020
 * @date: 2021/11/14
 */

@FeignClient("gulimall-member")
public interface MemberFeignService {

    @RequestMapping("member/memberreceiveaddress/info/{id}")
    R addrInfo(@PathVariable("id") Long id);
}
