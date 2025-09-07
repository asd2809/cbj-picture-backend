package com.yupi.yupictureDDD.interfaces.controller;


import com.yupi.yupictureDDD.infrastructure.common.BaseResponse;
import com.yupi.yupictureDDD.infrastructure.common.ResultUtils;
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
