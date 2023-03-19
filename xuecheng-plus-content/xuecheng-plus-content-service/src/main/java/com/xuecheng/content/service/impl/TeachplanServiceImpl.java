package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanMediaService;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 课程计划 服务实现类
 * </p>
 *
 * @author lxc
 */
@Slf4j
@Service
public class TeachplanServiceImpl extends ServiceImpl<TeachplanMapper, Teachplan> implements TeachplanService {
    @Override
    public List<TeachplanDto> findTeachplanTree(long courseId) {
        return baseMapper.selectTreeNodes(courseId);
    }
   @Autowired
   TeachplanMediaMapper teachplanMediaMapper;

    @Transactional
    @Override
    public void saveTeachplan(SaveTeachplanDto teachplanDto) {
        //课程计划id
        Long id = teachplanDto.getId();
        //修改课程计划
        if(id!=null){
            Teachplan teachplan = baseMapper.selectById(id);
            BeanUtils.copyProperties(teachplanDto,teachplan);
            baseMapper.updateById(teachplan);
        }else{
            //取出同父同级别的课程计划数量
            int count = getTeachplanCount(teachplanDto.getCourseId(), teachplanDto.getParentid());
            Teachplan teachplanNew = new Teachplan();
            //设置排序号
            teachplanNew.setOrderby(count+1);
            BeanUtils.copyProperties(teachplanDto,teachplanNew);
            baseMapper.insert(teachplanNew);
        }
    }

    @Autowired
    private TeachplanMediaService teachplanMediaService;

    @Transactional
    @Override
    public void deleteTeachplan(Long id) {
        // 判断是否为父节点，若为父节点，其下不能有子节点，因此直接查询其下子节点的数量，若为0则不删除
        LambdaQueryWrapper<Teachplan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Teachplan::getParentid, id);
        if(baseMapper.selectCount(wrapper)>0) XueChengPlusException.cast("课程计划信息还有子级信息，无法操作");
        // 若为子节点，还要删除跟他关联的视频信息
        baseMapper.deleteById(id);
        LambdaQueryWrapper<TeachplanMedia> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(TeachplanMedia::getTeachplanId, id);
        teachplanMediaService.remove(wrapper2);
    }

    @Transactional
    @Override
    public void moveDownTeachplan(Long id) {
        Teachplan teachplan = baseMapper.selectById(id);
        if(teachplan==null) XueChengPlusException.cast("章节或小节不存在");
        LambdaQueryWrapper<Teachplan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Teachplan::getParentid, teachplan.getParentid());
        wrapper.eq(Teachplan::getOrderby, teachplan.getOrderby()+1);
        List<Teachplan> teachplans = baseMapper.selectList(wrapper);
        Teachplan teachplan1 = teachplans.get(0);
        if(teachplan1==null) XueChengPlusException.cast("已经是最下级结点");
        teachplan1.setOrderby(teachplan.getOrderby());
        teachplan.setOrderby(teachplan.getOrderby()+1);
        baseMapper.updateById(teachplan);
        baseMapper.updateById(teachplan1);
    }

    @Transactional
    @Override
    public void moveUpTeachplan(Long id) {
        Teachplan teachplan = baseMapper.selectById(id);
        if(teachplan==null) XueChengPlusException.cast("章节或小节不存在");
        if(1==teachplan.getOrderby()) XueChengPlusException.cast("已经是最上级结点");
        LambdaQueryWrapper<Teachplan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Teachplan::getParentid, teachplan.getParentid());
        wrapper.eq(Teachplan::getOrderby, teachplan.getOrderby()-1);
        List<Teachplan> teachplans = baseMapper.selectList(wrapper);
        Teachplan teachplan1 = teachplans.get(0);
        if(teachplan1==null) XueChengPlusException.cast("已经是最上级结点");
        teachplan1.setOrderby(teachplan.getOrderby());
        teachplan.setOrderby(teachplan.getOrderby()-1);
        baseMapper.updateById(teachplan);
        baseMapper.updateById(teachplan1);
    }

    // 获取最新的排序号
    private int getTeachplanCount(long courseId,long parentId){
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId,courseId);
        queryWrapper.eq(Teachplan::getParentid,parentId);
        Integer count = baseMapper.selectCount(queryWrapper);
        return count;
    }


    @Transactional
    @Override
    public TeachplanMedia associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {
//教学计划 id
        Long teachplanId = bindTeachplanMediaDto.getTeachplanId();
        Teachplan teachplan = baseMapper.selectById(teachplanId);
        if(teachplan==null){
            XueChengPlusException.cast("教学计划不存在");
        }
        Integer grade = teachplan.getGrade();
        if(grade!=2){
            XueChengPlusException.cast("只允许第二级教学计划绑定媒资文件");
        }
//课程 id
        Long courseId = teachplan.getCourseId();
//先删除原来该教学计划绑定的媒资
        teachplanMediaMapper.delete(new LambdaQueryWrapper<TeachplanMedia>().eq(TeachplanMedia::getTeachplanId,teachplanId));
//再添加教学计划与媒资的绑定关系
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        teachplanMedia.setCourseId(courseId);
        teachplanMedia.setTeachplanId(teachplanId);
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
        teachplanMedia.setMediaId(bindTeachplanMediaDto.getMediaId());
        teachplanMedia.setCreateDate(LocalDateTime.now());
        teachplanMediaMapper.insert(teachplanMedia);
        return teachplanMedia;
    }

}
