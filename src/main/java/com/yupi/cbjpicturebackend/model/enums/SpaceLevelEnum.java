package com.yupi.cbjpicturebackend.model.enums;


import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum SpaceLevelEnum {

    //    1.定义枚举值
    COMMON("普通版", 0, 100, 100L * 1024 * 1024),
    PROFESSIONAL("专业版", 1, 1000, 1000L * 1024 * 1024),
    FLAGSHIP("旗舰版", 2, 10000, 10000L * 1024 * 1024);
    //  2.枚举属性
    private final String text;
    //    final该属性只能被赋值一次，
    private final int value;

    private final long maxCount;

    private final long maxSize;

    //  3 枚举类的构造方法
    SpaceLevelEnum(String text, int value, long maxCount, long maxSize) {
        this.text = text;
        this.value = value;
        this.maxCount = maxCount;
        this.maxSize = maxSize;
    }

    /**
     * 根据value获取枚举
     *
     * @param value 枚举值得=的vakue
     * @return 枚举值
     */
    public static SpaceLevelEnum getSpaceLevelEnum(int value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
//        UserRoleEnum.values()是获取这个枚举类，所有已经定义的枚举
//        这个枚举类里定义好的所有值集合
//        4.遍历枚举
        for (SpaceLevelEnum userRoleEnum : SpaceLevelEnum.values()) {
//            5.根据枚举值找枚举
            if (userRoleEnum.value == value) {
                return userRoleEnum;
            }
        }
        return null;
    }

}
