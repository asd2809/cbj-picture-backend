package com.yupi.yupictureDDD.interfaces.Assembler;

import com.yupi.yupictureDDD.domain.space.entity.Space;
import com.yupi.yupictureDDD.interfaces.dto.space.SpaceAddRequest;
import com.yupi.yupictureDDD.interfaces.dto.space.SpaceEditRequest;
import com.yupi.yupictureDDD.interfaces.dto.space.SpaceUpdateRequest;
import org.springframework.beans.BeanUtils;

public class SpaceAssembler {

    public static Space toSpaceEntity(SpaceAddRequest request) {
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        return space;
    }

    public static Space toSpaceEntity(SpaceUpdateRequest request) {
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        return space;
    }

    public static Space toSpaceEntity(SpaceEditRequest request) {
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        return space;
    }
}
