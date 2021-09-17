package com.atguigu.common.constant;

/**
 * @author: CyS2020
 * @date: 2021/9/17
 * 描述：常量值
 */
public class ProductConstant {

    public enum AttrEnum {

        ATTR_TYPE_BASE(1, "基本属性"),

        ATTR_TYPE_SALE(0, "销售属性");

        AttrEnum(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        int code;

        String msg;

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }
}
