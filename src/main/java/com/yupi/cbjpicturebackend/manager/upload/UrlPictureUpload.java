package com.yupi.cbjpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.yupi.yupicture.infrastructure.exception.BusinessException;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * url图片上传
 */
@Service
public class UrlPictureUpload extends PictureUploadTemplate {
    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        //1.校验参数
        ThrowUtils.throwIF(fileUrl == null, ErrorCode.PARAMS_ERROR, "文件地址为空");
        //2.校验URL格式
        try {
            new URL(fileUrl);
        } catch (Exception e) {
            ThrowUtils.throwIF( true,ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }
        //3校验URL的协议
        ThrowUtils.throwIF(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"),
                ErrorCode.PARAMS_ERROR, "仅支持http 或 https协议");
        //4.发送HEAD请求验证文件
        HttpResponse httpResponse = null;
        try{
            httpResponse =  HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
//          未正常返回
            if (httpResponse == null) {
                return;
            }
            //5.文件存在、文件类型校验
            String contentType = httpResponse.header("Content-Type");
            //不为空才校验是否合法,这样校验规则相对宽松
            if (StrUtil.isNotBlank(contentType)) {
                //允许图片的类型
                final List<String> ALLOW_FORMAT_LIST = Arrays.asList("image/jpg", "image/jpeg", "image/png",  "image/gif");
                ThrowUtils.throwIF(!ALLOW_FORMAT_LIST.contains(contentType),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");

            }
            //6.文件存在、文件大小校验
            String contentLengthStr = httpResponse.header("Content-Length");
            if(StrUtil.isNotBlank(contentLengthStr)){
                try{
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long ONE_M = 1024 * 1024;
                    ThrowUtils.throwIF(contentLength > ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过2mb");

                }catch (Exception e){
                    throw  new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式异常");
                }
            }
        } finally {
            //记得是释放资源
            if(httpResponse != null){
                httpResponse.close();
            }
        }


    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
//        从url中提取文件名
        return FileUtil.getName(fileUrl);
    }

    @Override
    protected void processFile(Object inputSource, File file) throws IOException {
        String fileUrl = (String) inputSource;
//        下载文件到临时目录
        HttpUtil.downloadFile(fileUrl, file);
    }
}
