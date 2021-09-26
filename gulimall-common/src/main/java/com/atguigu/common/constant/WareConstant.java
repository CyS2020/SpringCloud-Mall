package com.atguigu.common.constant;

/**
 * @author: CyS2020
 * @date: 2021/9/24
 * 描述：仓库模块常量
 */
public class WareConstant {

    public enum PurchaseEnum {

        CREATED(0, "新建"),

        ASSIGNED(1, "已分配"),

        RECEIVED(2, "已领取"),

        FINISHED(3, "已完成"),

        HASERROR(4, "有异常"),

        ;

        int code;

        String desc;

        PurchaseEnum(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }

    public enum PurchaseDetailEnum {

        CREATED(0, "新建"),

        ASSIGNED(1, "已分配"),

        BUYING(2, "正在采购"),

        FINISHED(3, "已完成"),

        HASERROR(4, "采购失败"),

        ;

        int code;

        String desc;

        PurchaseDetailEnum(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }
}
