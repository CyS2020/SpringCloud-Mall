package com.atguigu.gulimall.ware.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author: CyS2020
 * @date: 2021/9/24
 * 描述：完成采购表单数据
 */
@Data
public class PurchaseDoneVo {

    @NotNull
    private Long id;

    List<PurchaseDoneItemVo> items;
}
