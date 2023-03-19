package com.xuecheng.content.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.content.model.dto.AddCourseTeacherDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(value = "讲师信息编辑接口", tags = "讲师信息编辑接口")
@RestController
public class CourseTeacherController {
    @Autowired
    CourseTeacherService teacherService;

    @ApiOperation("根据课程id查询相关教师")
    @GetMapping("/courseTeacher/list/{id}")
    public List<CourseTeacher> getListByCourseId(@PathVariable Long id){
        LambdaQueryWrapper<CourseTeacher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseTeacher::getCourseId, id);
        return teacherService.list(wrapper);
    }

    @ApiOperation("新增或修改教师信息")
    @PostMapping ("/courseTeacher")
    public CourseTeacher insertCourseTeacher(@RequestBody @Validated CourseTeacher teacher){
        teacherService.saveOrUpdate(teacher);
        return teacher;
    }

    @ApiOperation("删除教师")
    @DeleteMapping ("/courseTeacher/course/{courseId}/{id}")
    public void deleteCourseTeacher(@PathVariable Long courseId, @PathVariable Long id){
        LambdaQueryWrapper<CourseTeacher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseTeacher::getCourseId, courseId);
        wrapper.eq(CourseTeacher::getId, id);
        teacherService.remove(wrapper);
    }
}
