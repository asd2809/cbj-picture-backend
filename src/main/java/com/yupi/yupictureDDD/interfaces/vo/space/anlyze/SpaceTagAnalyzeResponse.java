package com.yupi.yupictureDDD.interfaces.vo.space.anlyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceTagAnalyzeResponse implements Serializable {

    /**
     * 标签
     */
    private String tag;

    /**
     * 标签出现的次数
     */
    private Long count;



    private static final long serialVersionUID = 1L;
}
