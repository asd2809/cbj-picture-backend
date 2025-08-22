package com.yupi.cbjpicturebackend.api.aliyunai;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;

public class test {

    private static final String API_KEY = "sk-078eb0d5b3334324906d8e2cc33012cb";
    private static final String CREATE_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";
    private static final String GET_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    public static void main(String[] args) throws Exception {
        // 1. 创建扩图任务
        JSONObject body = new JSONObject();
        body.put("model", "image-out-painting");

        JSONObject input = new JSONObject();
        input.put("image_url", "https://www.gstatic.com/webp/gallery/1.jpg");
        body.put("input", input);

        JSONObject parameters = new JSONObject();
        parameters.put("angle", 0);
        parameters.put("x_scale", 1.2);
        parameters.put("y_scale", 1.2);
        body.put("parameters", parameters);

        HttpResponse createResp = HttpRequest.post(CREATE_URL)
                .header(Header.AUTHORIZATION, "Bearer " + API_KEY)
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, "application/json")
                .body(body.toString())
                .execute();

        if (!createResp.isOk()) {
            System.err.println("创建任务失败: " + createResp.body());
            return;
        }

        JSONObject createJson = JSONUtil.parseObj(createResp.body());
        String taskId = createJson.getJSONObject("output").getStr("task_id");
        System.out.println("任务已创建，taskId=" + taskId);

        // 2. 查询任务状态
        String status = null;
        for (int i = 0; i < 15; i++) { // 最多轮询 15 次，每次间隔 2 秒
            HttpResponse getResp = HttpRequest.get(String.format(GET_TASK_URL, taskId))
                    .header(Header.AUTHORIZATION, "Bearer " + API_KEY)
                    .execute();

            if (!getResp.isOk()) {
                System.err.println("查询任务失败: " + getResp.body());
                return;
            }

            JSONObject getJson = JSONUtil.parseObj(getResp.body());
            JSONObject output = getJson.getJSONObject("output");
            status = output.getStr("task_status");
            System.out.println("任务状态: " + status);

            if ("SUCCEEDED".equals(status)) {
                String resultUrl = output.getStr("output_image_url");
                System.out.println("扩图成功，图片地址: " + resultUrl);
                break;
            } else if ("FAILED".equals(status)) {
                System.err.println("扩图任务失败");
                break;
            }

            Thread.sleep(2000); // 等 2 秒再查
        }

        if (!"SUCCEEDED".equals(status)) {
            System.err.println("任务未成功，最终状态=" + status);
        }
    }
}
