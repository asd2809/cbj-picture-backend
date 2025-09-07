package com.yupi.yupictureDDD.interfaces.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.yupi.yupictureDDD.infrastructure.annotation.AuthCheck;
import com.yupi.yupictureDDD.infrastructure.common.BaseResponse;
import com.yupi.yupictureDDD.infrastructure.common.ResultUtils;
import com.yupi.yupictureDDD.domain.user.constant.UserConstant;
import com.yupi.yupictureDDD.infrastructure.exception.BusinessException;
import com.yupi.yupictureDDD.infrastructure.exception.ErrorCode;
import com.yupi.yupictureDDD.infrastructure.api.CosManager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;
    /**
     *
     * 测试文件上传
     * @param multipartFile 接收文件
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
//    MultipartFile，springmvc封装的上传文件对象
    public BaseResponse<String> testUploadFile(MultipartFile multipartFile) {
//        文件目录
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);
        File file =  null;
        try {
//            上传文件
//            在本地上传了临时文件
            file = File.createTempFile(filepath,null);
//            把file传递给本地临时文件
            multipartFile.transferTo(file);
//            由于这个方法是接收file而不是multipartFile,所以要转换成file文件
            cosManager.putObject(filepath,file);
//            返回可访问的地址
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            log.error("file upload error,filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"图片上传失败");
        } finally {
//            删除临时文件
            if (file != null) {
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error,filepath = {}", filepath);
                }
            }
        }

    }


    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download")
//    通过HttpServletResponse把数据返回给浏览器
    public void testDownloadFile(String filepath, HttpServletResponse response) throws Exception {
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            byte[] byteArray = IOUtils.toByteArray(cosObjectInput);
            //        设置响应头
            response.setContentType("application/octet-stream;charset=utf-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
//            写入相应
            response.getOutputStream().write(byteArray);
//            强制把缓冲区的数据立即写入到客户端(浏览器)
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("download file error,filepath = " + filepath, e);
            throw new RuntimeException(e);
        } finally {
//      释放流
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }


}

