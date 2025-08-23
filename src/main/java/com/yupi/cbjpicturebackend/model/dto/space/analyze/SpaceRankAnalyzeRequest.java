package com.yupi.cbjpicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    /**
     * 排名前N的空间
     */
    private Integer toN = 10;

    private static final long serialVersionUID  =1L ;
}
