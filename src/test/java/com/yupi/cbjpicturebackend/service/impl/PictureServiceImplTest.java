package com.yupi.cbjpicturebackend.service.impl;

import com.yupi.cbjpicturebackend.model.entity.Picture;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PictureServiceImplTest {

    @Test
    void testFillPictureWithNameRule_NormalCase() {
        // 准备数据
        List<Picture> pictureList = new ArrayList<>();
        pictureList.add(new Picture());
        pictureList.add(new Picture());
        pictureList.add(new Picture());

        String nameRule = "图片{序号}";

        // 执行方法
        PictureServiceImpl service = new PictureServiceImpl();
        service.fillPictureWithNameRule(pictureList, nameRule);

        // 验证结果
        assertEquals("图片1", pictureList.get(0).getName());
        assertEquals("图片2", pictureList.get(1).getName());
        assertEquals("图片3", pictureList.get(2).getName());
    }

    @Test
    void testFillPictureWithNameRule_EmptyRule() {
        List<Picture> pictureList = new ArrayList<>();
        pictureList.add(new Picture());

        String nameRule = ""; // 空规则

        PictureServiceImpl service = new PictureServiceImpl();
        service.fillPictureWithNameRule(pictureList, nameRule);

        // 名称不应该被设置
        assertNull(pictureList.get(0).getName());
    }

    @Test
    void testFillPictureWithNameRule_CustomRule() {
        List<Picture> pictureList = new ArrayList<>();
        pictureList.add(new Picture());
        pictureList.add(new Picture());

        String nameRule = "第{序号}张照片";

        PictureServiceImpl service = new PictureServiceImpl();
        service.fillPictureWithNameRule(pictureList, nameRule);

        assertEquals("第1张照片", pictureList.get(0).getName());
        assertEquals("第2张照片", pictureList.get(1).getName());
    }
}
