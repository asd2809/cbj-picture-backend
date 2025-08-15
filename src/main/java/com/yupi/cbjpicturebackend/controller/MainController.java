package com.yupi.cbjpicturebackend.controller;


import com.yupi.cbjpicturebackend.common.BaseResponse;
import com.yupi.cbjpicturebackend.common.ResultUtils;
import lombok.Getter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {
    /***
     *
     * 健康检查
     */
    @GetMapping("/heath")
    public BaseResponse<String> heath() {
        return ResultUtils.success("ok");
    }
}
