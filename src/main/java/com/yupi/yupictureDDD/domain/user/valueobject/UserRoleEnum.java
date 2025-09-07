package com.yupi.yupictureDDD.domain.user.valueobject;


import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum UserRoleEnum {
    //    1.定义枚举值
    USER("用户", "user"),
    ADMIN("管理员", "admin");

    //  2.枚举属性
    private final String text;
    //    final该属性只能被赋值一次，
    private final String value;

    //  3 枚举类的构造方法
    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举
     *
     * @param value 枚举值得=的vakue
     * @return 枚举值
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
//        UserRoleEnum.values()是获取这个枚举类，所有已经定义的枚举
//        这个枚举类里定义好的所有值集合
//        4.遍历枚举
        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
//            5.根据枚举值找枚举
            if (userRoleEnum.value.equals(value)) {
                return userRoleEnum;
            }
        }
        return null;
    }
}
