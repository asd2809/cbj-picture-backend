package com.yupi.cbjpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.yupi.cbjpicturebackend.config.CosClientConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

//跟业务没有关系，通用的代码
//这些都是将文件上传到腾讯云cos的代码操作
@Component
public class CosManager {
    @Resource
    private CosClientConfig cosClientConfig;

//    腾讯云cos的客户端
    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     * 只有上传对象，没有使用数据万象，不能返回图片的信息，即只上传不返回
     * @param key 唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file){
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载图片
     *
     * @param key
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传对象(带有图片信息)
     *使用了数据万象，对图片进行了持久化处理
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
//        对图片进行处理(获取基本信息也被作为一种图片的处理)
        PicOperations picOperations = new PicOperations();
//      1.表示返回原图信息
        picOperations.setIsPicInfo(1);
//        构造处理参数
        //图片处理规则列表
        List<PicOperations.Rule> rule = new ArrayList<PicOperations.Rule>();
        //2. 图片压缩(转成webp格式)
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(webpKey);
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setBucket(cosClientConfig.getBucket());
        rule.add(compressRule);
        //3. 图片缩略图
        PicOperations.Rule thumbnailRule = new PicOperations.Rule();
        //FileUtil.mainName，获取文件主名称(去掉路径和文件后缀名)
        //FileUtil.getSuffix获取文件名后缀
        if (file.length() > 2* 1024){
            String thumbnailKey = FileUtil.mainName(key) + ".thumb" +  "." +FileUtil.getSuffix(key);
            thumbnailRule.setFileId(thumbnailKey);
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>",128,128));
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            rule.add(thumbnailRule);
        }


        picOperations.setRules(rule);
        putObjectRequest.setPicOperations(picOperations);

        return cosClient.putObject(putObjectRequest);
    }

    /**
     *删除图片
     * @param key
     */
    public void deleteObject(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }
}
