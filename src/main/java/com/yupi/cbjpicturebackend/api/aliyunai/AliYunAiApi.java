package com.yupi.cbjpicturebackend.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.yupi.cbjpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.yupi.cbjpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.cbjpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.yupi.cbjpicturebackend.exception.BusinessException;
import com.yupi.cbjpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Component
public class AliYunAiApi {
    // 读取配置文件
    @Value("${aliYunAi.apiKey}")
    private String apiKey;


    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建任务
     *
     * @param createOutPaintingTaskRequest
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        if (createOutPaintingTaskRequest == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "扩图参数为空");
        }
        //参数校验
        validateOutPaintingTaskRequest(createOutPaintingTaskRequest);
        String jsonStr = JSONUtil.toJsonStr(createOutPaintingTaskRequest);
        // 发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                // 必须开启异步处理，设置为enable。
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(jsonStr);
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                String errorMessage = response.getMessage();
                log.error("AI 扩图失败，errorCode:{}, errorMessage:{}", errorCode, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图接口响应异常");
            }
            return response;
        }
    }

    /**
     * 查询创建的任务
     *
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 id 不能为空");
        }
        try (HttpResponse httpResponse = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .execute()) {
            if (!httpResponse.isOk()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }

    /**
     * ⚠️ 参数校验
     */
    public void validateOutPaintingTaskRequest(CreateOutPaintingTaskRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "扩图参数为空");
        }

        // 校验输入图片
        if (request.getInput() == null || StrUtil.isBlank(request.getInput().getImageUrl())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "输入图片 URL 不能为空");
        }

        // 校验模型名称
        if (StrUtil.isBlank(request.getModel())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "模型名称不能为空");
        }

        // 校验参数
        CreateOutPaintingTaskRequest.Parameters p = request.getParameters();
        if (p != null) {
            // 角度
            if (p.getAngle() != null && (p.getAngle() < 0 || p.getAngle() > 359)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "angle 必须在 0-359 之间");
            }

            // 输出比例
            if (StrUtil.isNotBlank(p.getOutputRatio())) {
                String[] validRatios = {"", "1:1", "3:4", "4:3", "9:16", "16:9"};
                boolean match = false;
                for (String ratio : validRatios) {
                    if (ratio.equals(p.getOutputRatio())) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "outputRatio 值不合法");
                }
            }

            // xScale/yScale
            if (p.getXScale() != null && (p.getXScale() < 1.0f || p.getXScale() > 3.0f)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "xScale 必须在 1.0-3.0 之间");
            }
            if (p.getYScale() != null && (p.getYScale() < 1.0f || p.getYScale() > 3.0f)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "yScale 必须在 1.0-3.0 之间");
            }

            // top/bottom/left/right offset
            if (p.getTopOffset() != null && p.getTopOffset() < 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "topOffset 不能小于 0");
            }
            if (p.getBottomOffset() != null && p.getBottomOffset() < 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "bottomOffset 不能小于 0");
            }
            if (p.getLeftOffset() != null && p.getLeftOffset() < 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "leftOffset 不能小于 0");
            }
            if (p.getRightOffset() != null && p.getRightOffset() < 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "rightOffset 不能小于 0");
            }
        }
    }


}
