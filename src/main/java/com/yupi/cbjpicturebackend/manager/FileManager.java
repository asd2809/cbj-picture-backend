package com.yupi.cbjpicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.yupi.cbjpicturebackend.config.CosClientConfig;
import com.yupi.cbjpicturebackend.exception.BusinessException;
import com.yupi.cbjpicturebackend.exception.ErrorCode;
import com.yupi.cbjpicturebackend.exception.ThrowUtils;
import com.yupi.cbjpicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 文件服务
 * @deprecated 已废弃，改为使用upload包的模板方法优化
 */
@Service
@Slf4j
@Deprecated
public class FileManager {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

//    主方法，用于把图片上传到腾讯云cos
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
//        1.校验图片
        validPicture(multipartFile);
//        2.图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
//        自己拼接文件上传路径，而不是使用原始文件名称(可以自己定义  )
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFilename);
        //3.        解析结果并返回
        File file = null;
        try {
//            上传文件
//            在本地上传了临时文件
            file = File.createTempFile(uploadPath, null);
//            把file传递给本地临时文件
            multipartFile.transferTo(file);
//            由于这个方法是接收file而不是multipartFile,所以要转换成file文件
//            上传到对象存储里面(腾讯云的cos中)
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
//            获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            String format = imageInfo.getFormat();

//            计算宽高比
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 /picWidth,2).doubleValue();
//            封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setName(FileUtil.getSuffix(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(format);
//            返回可访问的地址
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        } finally {
//       4. 临时文件清理
            deleteTempFile(file);
        }
    }

    /**
     * 校验文件
     *
     * @param multipartFile
     */
    private void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIF(multipartFile == null, ErrorCode.PARAMS_ERROR, "图片校验失败");
//        校验文本大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIF(fileSize > ONE_M, ErrorCode.PARAMS_ERROR, "图片过大,图片大小不能超过：2MB");
//        2.校验文件后端
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpg", "jpeg", "png", "gif");
        ThrowUtils.throwIF(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "不符合图片的后缀名");
    }
    /***
     * 清理临时文件
     * @param file
     */
    private void deleteTempFile(File file) {
        //            删除临时文件
        if (file == null) {
            return;
        }
//        删除临时文件
        boolean delete = file.delete();
        if (!delete) {
            log.error("file delete error,filepath = {}", file.getAbsolutePath());

        }
    }



}
