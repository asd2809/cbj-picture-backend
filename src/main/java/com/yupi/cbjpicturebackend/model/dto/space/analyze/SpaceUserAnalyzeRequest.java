package com.yupi.cbjpicturebackend.model.dto.space.analyze;


import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 空间用户上传行为分析请求
 */
@EqualsAndHashCode(callSuper=false)
@Data
public class SpaceUserAnalyzeRequest extends SpaceAnalyzeRequest {

    /**
     * 用户id
     */
     private Long userId ;
    /**
     * 时间维度：day/week/math
     */
    private String timeDimension;
}
