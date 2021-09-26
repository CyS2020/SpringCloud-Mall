package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * @author: CyS2020
 * @date: 2021/9/24
 * 描述：采购单表单提交数据
 */
@Data
public class MergeVo {

    private Long purchaseId;

    private List<Long> items;
}
