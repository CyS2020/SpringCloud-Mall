package com.atguigu.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author: CyS2020
 * @date: 2021/10/3
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Catelog2Vo {

    private String catalog1Id;

    private List<Catelog3Vo> catalog3List;

    private String id;

    private String name;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Catelog3Vo {

        private String catalog2Id;

        private String id;

        private String name;
    }
}
