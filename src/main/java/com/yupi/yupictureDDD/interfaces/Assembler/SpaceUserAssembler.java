package com.yupi.yupictureDDD.interfaces.Assembler;

import com.yupi.yupictureDDD.domain.space.entity.SpaceUser;
import com.yupi.yupictureDDD.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.yupi.yupictureDDD.interfaces.dto.spaceuser.SpaceUserEditRequest;
import org.springframework.beans.BeanUtils;

public class SpaceUserAssembler {

    public static SpaceUser toSpaceUserEntity(SpaceUserAddRequest request) {
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(request, spaceUser);
        return spaceUser;
    }

    public static SpaceUser toSpaceUserEntity(SpaceUserEditRequest request) {
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(request, spaceUser);
        return spaceUser;
    }
}
