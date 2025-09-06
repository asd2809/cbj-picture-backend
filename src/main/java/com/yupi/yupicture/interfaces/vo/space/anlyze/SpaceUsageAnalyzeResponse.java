package com.yupi.yupicture.interfaces.vo.space.anlyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


/**
 * 空间资源使用分析响应类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceUsageAnalyzeResponse implements Serializable {

    /**
     * 已使用大小
     */
    private Long usedSize;
    /**
     * 总大小
     */
    private Long maxSize;

    /**
     * 空间使用比例
     */
    private Double sizeUsageRatio;
    /**
     * 当前图片数据
     */
    private Long userCount;
    /**
     * 最大图片数量
     */
    private Long maxCount;
    /**
     * 图片数量占比
     */
    private Double userUsageRatio;

    private static final long serialVersionUID = 1L;
}

