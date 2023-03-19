package com.xuecheng.content.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanMediaService;
import com.xuecheng.content.service.TeachplanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(value = "课程计划编辑接口",tags = "课程计划编辑接口")
@RestController
public class TeachplanController {
    @Autowired
    TeachplanMediaService teachplanMediaService;
    @Autowired
    TeachplanService teachplanService;
    @ApiOperation("查询课程计划树形结构")
    @ApiImplicitParam(value = "courseId",name = "课程基础Id值",required =
            true,dataType = "Long",paramType = "path")
    @GetMapping("teachplan/{courseId}/tree-nodes")
    public List<TeachplanDto> getTreeNodes(@PathVariable Long courseId){
        return teachplanService.findTeachplanTree(courseId);
    }


    @ApiOperation("课程计划创建或修改")
    @PostMapping("/teachplan")
    public void saveTeachplan( @RequestBody SaveTeachplanDto teachplan){
        teachplanService.saveTeachplan(teachplan);
    }

    @ApiOperation("课程计划删除")
    @DeleteMapping("/teachplan/{id}")
    public void deleteTeachplan(@PathVariable Long id){
        teachplanService.deleteTeachplan(id);
    }

    @ApiOperation("课程计划下移")
    @DeleteMapping("/teachplan/movedown/{id}")
    public void moveDownTeachplan(@PathVariable Long id){
        teachplanService.moveDownTeachplan(id);
    }

    @ApiOperation("课程计划上移")
    @DeleteMapping("/teachplan/moveup/{id}")
    public void moveUpTeachplan(@PathVariable Long id){
        teachplanService.moveUpTeachplan(id);
    }

    @ApiOperation(value = "课程计划和媒资信息绑定")
    @PostMapping("/teachplan/association/media")
    public void associationMedia(@RequestBody BindTeachplanMediaDto bindTeachplanMediaDto){
        teachplanService.associationMedia(bindTeachplanMediaDto);
    }

    @ApiOperation(value = "课程计划和媒资信息解除绑定")
    @DeleteMapping("/teachplan/association/media/{teachPlanId}/{mediaId}")
    public void disAssociationMedia(@PathVariable Long teachPlanId, @PathVariable Long mediaId){
        LambdaQueryWrapper<TeachplanMedia> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeachplanMedia::getTeachplanId, teachPlanId);
        wrapper.eq(TeachplanMedia::getMediaId, mediaId);
        teachplanMediaService.remove(wrapper);
    }
}
