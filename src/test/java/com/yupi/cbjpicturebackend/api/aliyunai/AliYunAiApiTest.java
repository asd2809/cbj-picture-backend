package com.yupi.cbjpicturebackend.api.aliyunai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yupi.cbjpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.yupi.cbjpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.cbjpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.Order;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class AliYunAiApiTest {

    private static String taskId;
    private static final String originalUrl = "https://www.gstatic.com/webp/gallery/1.jpg"; // 合规测试图片

    @Autowired
    private AliYunAiApi aliYunAiApi;

    /**
     * 创建扩图任务
     */
    @Test
    @Order(1)
    public void testCreateOutPaintingTask() throws JsonProcessingException {
        CreateOutPaintingTaskRequest request = new CreateOutPaintingTaskRequest();
        request.setModel("image-out-painting");

        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(originalUrl);
        request.setInput(input);

        CreateOutPaintingTaskRequest.Parameters parameters = new CreateOutPaintingTaskRequest.Parameters();
        parameters.setAngle(0);       // 不旋转
        parameters.setXScale(1.2f);   // 扩展 20%
        parameters.setYScale(1.2f);
        request.setParameters(parameters);

        CreateOutPaintingTaskResponse response = aliYunAiApi.createOutPaintingTask(request);
        log.info("创建扩图任务响应：{}", response);

        assert response != null;
        assert response.getOutput() != null;
        taskId = response.getOutput().getTaskId();
        assert taskId != null;

        log.info("任务ID: {}", taskId);
    }

    /**
     * 查询扩图任务并校验结果
     */
    @Test
    @Order(2)
    public void testGetOutPaintingTask() throws Exception {
        assert taskId != null : "taskId 为空，请先执行 testCreateOutPaintingTask";

        GetOutPaintingTaskResponse response = null;
        String status = null;

        // 轮询等待任务完成
        for (int i = 0; i < 15; i++) { // 最多等 15 次（30秒）
            response = aliYunAiApi.getOutPaintingTask(taskId);
            status = response.getOutput().getTaskStatus();
            log.info("查询扩图任务响应：{}", response);
            log.info("任务状态: {}", status);

            if ("SUCCEEDED".equals(status)) {
                break;
            } else if ("FAILED".equals(status)) {
                throw new AssertionError("扩图任务失败: " + response);
            }
            Thread.sleep(2000); // 等 2 秒再查
        }

        assert "SUCCEEDED".equals(status) : "任务未成功，状态=" + status;

        // 校验返回图片
        String resultUrl = response.getOutput().getOutputImageUrl();
        log.info("扩图生成图片地址: {}", resultUrl);

        BufferedImage originalImg = ImageIO.read(new URL(originalUrl));
        BufferedImage expandedImg = ImageIO.read(new URL(resultUrl));

        int originalW = originalImg.getWidth();
        int originalH = originalImg.getHeight();
        int expandedW = expandedImg.getWidth();
        int expandedH = expandedImg.getHeight();

        log.info("原图尺寸: {}x{}", originalW, originalH);
        log.info("扩图后尺寸: {}x{}", expandedW, expandedH);

        assert expandedW > originalW || expandedH > originalH
                : "扩图失败，尺寸未增加";
    }
}
