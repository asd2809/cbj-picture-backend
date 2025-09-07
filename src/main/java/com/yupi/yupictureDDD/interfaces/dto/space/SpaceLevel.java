package com.yupi.yupictureDDD.interfaces.dto.space;


import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 空间级别，要返回给前端，让用户明白空间级别有多少
 */
@Data
@AllArgsConstructor
public class SpaceLevel {

    /**
     * 值
     */
    private int value;

    /**
     * 中文
     *
     */
    private String text;

    /**
     * 最大数量
     */
    private Long maxCount ;

    /**
     * 最大的大小
     */
    private long maxSize ;
}
