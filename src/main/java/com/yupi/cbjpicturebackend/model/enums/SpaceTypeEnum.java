package com.yupi.cbjpicturebackend.model.enums;


import cn.hutool.core.util.ObjUtil;
import lombok.Getter;


/**
 * 空间类型枚举类
 */
@Getter
public enum SpaceTypeEnum {

    //    1.定义枚举值
    PRIVATE("私有空间", 0),
    TEAM("团队空间", 1);
    //  2.枚举属性
    private final String text;
    private final int value;

    //    final该属性只能被赋值一次，
    //  3 枚举类的构造方法
    SpaceTypeEnum(String text, int value) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据value获取枚举
     *
     * @param value 枚举值得=的vakue
     * @return 枚举值
     */
    public static SpaceTypeEnum getEnumByValue(int value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
//        UserRoleEnum.values()是获取这个枚举类，所有已经定义的枚举
//        这个枚举类里定义好的所有值集合
//        4.遍历枚举
        for (SpaceTypeEnum spaceTypeEnum : SpaceTypeEnum.values()) {
//            5.根据枚举值找枚举
            if (spaceTypeEnum.value == value) {
                return spaceTypeEnum;
            }
        }
        return null;
    }

}
