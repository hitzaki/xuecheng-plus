package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CoursePreviewDto;

import java.io.File;

public interface CoursePublishService {
    public CoursePreviewDto getCoursePreviewInfo(Long courseId);
    /**
     * @description 课程发布接口
     * @param companyId 机构 id
     * @param courseId 课程 id
     * @return void
     * @author Mr.M
     * @date 2022/9/20 16:23
     */
    public void publish(Long companyId,Long courseId);

    /**
     * @description 课程静态化
     * @param courseId 课程 id
     * @return File 静态化文件
     * @author Mr.M
     * @date 2022/9/23 16:59
     */
    public File generateCourseHtml(Long courseId);
    /**
     * @description 上传课程静态化页面
     * @param file 静态化文件
     * @return void
     * @author Mr.M
     * @date 2022/9/23 16:59
     */
    public void uploadCourseHtml(Long courseId,File file);

    Boolean saveCourseIndex(Long courseId);
}
