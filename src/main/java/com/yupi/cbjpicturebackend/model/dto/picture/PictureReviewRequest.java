package com.yupi.cbjpicturebackend.model.dto.picture;

import lombok.Data;

@Data
public class PictureReviewRequest {
    /**
     * id
     */
    private Long id;

    /***
     * 审核状态
     */
    private Integer reviewStatus;
    /***
     * 审核信息
     */
    private String reviewMessage;
}
