package com.yupi.cbjpicturebackend.config;


import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;

import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
//把配置文件的cos.client.*前缀的属性,自动绑定到一个Java类的字段上
@ConfigurationProperties(prefix = "cos.client")
@Data
public class CosClientConfig {
    /**
     * 域名
     */
    private String host;
    /**
     * secretId
     */
    private String secretId;
    /**
     * 密钥(注意不要泄露)
     */
    private String secretKey;
    /**
     * 区域
     */
    private String region;
    /**
     * 域名
     */
    private String bucket;

//    返回腾讯云cos客户端
    @Bean
    public COSClient CosClient() {
//        初始化用户信息
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
//        设置bucket的区域
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        return new COSClient(cred,clientConfig);
    }
}
