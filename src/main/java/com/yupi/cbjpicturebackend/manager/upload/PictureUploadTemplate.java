package com.yupi.cbjpicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.job.ProcessResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.yupi.cbjpicturebackend.config.CosClientConfig;
import com.yupi.cbjpicturebackend.exception.BusinessException;
import com.yupi.cbjpicturebackend.exception.ErrorCode;
import com.yupi.cbjpicturebackend.exception.ThrowUtils;
import com.yupi.cbjpicturebackend.manager.CosManager;
import com.yupi.cbjpicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    //第一个参数是图片，第二个参数是文件夹名
    //    主方法，用于把图片上传到腾讯云cos
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
//        1.校验图片
        validPicture(inputSource);
//        2.图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputSource);
//        自己拼接文件上传路径，而不是使用原始文件名称(可以自己定义  )
        String uploadFilename = String.format("%s_%s.%s",
                DateUtil.formatDate(new Date()),
                uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s",
                uploadPathPrefix,
                uploadFilename);
        File file = null;
        try {
//            上传文件
//            3.在本地上传了临时文件获取文件到服务器
            file = File.createTempFile(uploadPath, null);
//            把file传递给本地临时文件
            processFile(inputSource, file);
//            由于这个方法是接收file而不是multipartFile,所以要转换成file文件
//            4.上传到对象存储里面(腾讯云的cos中)
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
//            5.获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //获取图片处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if(CollUtil.isNotEmpty(objectList)) {
                CIObject compressedCiObject =objectList.get(0);
                //封装压缩图返回结果
                return buildResult(originalFilename,compressedCiObject);
            }
            //封装返回结果
            return buildResult(uploadPath, originalFilename, file, imageInfo);
        } catch (Exception e) {
            log.error("图片上传到存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        } finally {
//          6.临时文件清理
            deleteTempFile(file);
        }
    }
    /**
     * 根据url 校验文件
     */
    /**
     * 封装图片压缩返回的结果
     *
     * @return
     */
    private UploadPictureResult buildResult(String originalFilename, CIObject compressedCiObject) {
        //            计算宽高比
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picWidth, 2).doubleValue();
        //            封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        uploadPictureResult.setName(FileUtil.getSuffix(originalFilename));
        uploadPictureResult.setPicSize(Long.valueOf(compressedCiObject.getSize()));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        //返回可访问的地址
        return uploadPictureResult;
    }
    /**
     * 根据url 校验文件
     */
    /**
     * 封装返回结果
     *
     * @param uploadPath
     * @param originalFilename
     * @param file
     * @return
     */
    private UploadPictureResult buildResult(String uploadPath, String originalFilename, File file, ImageInfo imageInfo) {
        //            计算宽高比
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picWidth, 2).doubleValue();

        //            封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setName(FileUtil.getSuffix(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
//            返回可访问的地址
        return uploadPictureResult;
    }

    /**
     * 校验输入源(本地文件或者url)
     *
     * @param inputSource
     */

    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的初始名称
     *
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFilename(Object inputSource);


    /**
     * 处理输入源并生成本地临时文件
     *
     * @param inputSource
     */
    protected abstract void processFile(Object inputSource, File file) throws IOException;

    /**
     * 校验文件
     *
     * @param multipartFile
     */

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
