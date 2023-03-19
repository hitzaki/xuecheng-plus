package com.xuecheng.content.model.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel(value="AddCourseTeacherDto", description="新增课程教师信息")
public class AddCourseTeacherDto {
    /**
     * 课程标识
     */
    @NotNull
    private Long courseId;

    /**
     * 教师标识
     */
    @NotNull
    private String teacherName;

    /**
     * 教师职位
     */
    @NotNull
    private String position;

    /**
     * 教师简介
     */
    @NotNull
    private String introduction;
}
