package com.yupi.yupicture.domain.picture.valueobject;


import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum PictureReviewStatusEnum {
    //    1.定义枚举值
    REVIEWING("待审核", 0),
    PASS("通过", 1),
    REJECT("拒绝", 2);
    //  2.枚举属性
    private final String text;
    //    final该属性只能被赋值一次，
    private final Integer value;

    //  3 枚举类的构造方法
    PictureReviewStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举
     *
     * @param value 枚举值得=的vakue
     * @return 枚举值
     */
    public static PictureReviewStatusEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
//        UserRoleEnum.values()是获取这个枚举类，所有已经定义的枚举
//        这个枚举类里定义好的所有值集合
//        4.遍历枚举
        for (PictureReviewStatusEnum pictureReviewStatusEnum : PictureReviewStatusEnum.values()) {
//            5.根据枚举值找枚举
            if (pictureReviewStatusEnum.value == value) {
                return pictureReviewStatusEnum;
            }
        }
        return null;
    }
}
