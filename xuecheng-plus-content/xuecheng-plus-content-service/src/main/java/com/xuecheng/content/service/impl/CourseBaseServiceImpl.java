package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.*;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseService;
import com.xuecheng.content.service.CourseMarketService;
import com.xuecheng.content.service.CourseTeacherService;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 课程基本信息 服务实现类
 * </p>
 *
 * @author lxc
 */
@Slf4j
@Service
public class CourseBaseServiceImpl extends ServiceImpl<CourseBaseMapper, CourseBase> implements CourseBaseService {

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {
        //构建查询条件对象
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //构建查询条件，根据课程名称查询
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()),CourseBase::getName,queryCourseParamsDto.getCourseName());
        //构建查询条件，根据课程审核状态查询
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()),CourseBase::getAuditStatus,queryCourseParamsDto.getAuditStatus());
        //构建查询条件，根据课程发布状态查询
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()),
                CourseBase::getStatus,queryCourseParamsDto.getPublishStatus());
        //分页对象
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(),
                pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<CourseBase> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        return new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
    }

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId,AddCourseDto dto) {
        //合法性校验
        //新增对象
        CourseBase courseBaseNew = new CourseBase();
            //将填写的课程信息赋值给新增对象
        BeanUtils.copyProperties(dto,courseBaseNew);
        //设置审核状态
        courseBaseNew.setAuditStatus("202002");
        //设置发布状态
        courseBaseNew.setStatus("203001");
        //机构id
        courseBaseNew.setCompanyId(companyId);
        //添加时间
        courseBaseNew.setCreateDate(LocalDateTime.now());
        //插入课程基本信息表
        int insert = courseBaseMapper.insert(courseBaseNew);
        Long courseId = courseBaseNew.getId();
        //课程营销信息
        CourseMarket courseMarketNew = new CourseMarket();
        BeanUtils.copyProperties(dto,courseMarketNew);
        courseMarketNew.setId(courseId);
        //收费规则
        String charge = dto.getCharge();
        //收费课程必须写价格且价格大于0
        if(charge.equals("201001")){
            Float price = dto.getPrice();
            if(price == null || price.floatValue()<=0){
                throw new RuntimeException("课程设置了收费价格不能为空且必须大于0");
            }
        }
        //插入课程营销信息
        int insert1 = courseMarketMapper.insert(courseMarketNew);
        if(insert<=0 || insert1<=0){
            throw new RuntimeException("新增课程基本信息失败");
        }
        //添加成功
        //返回添加的课程信息
        return getCourseBaseInfo(courseId);
    }

    @Override
    //根据课程id查询课程基本信息，包括基本信息和营销信息
    public CourseBaseInfoDto getCourseBaseInfo(long courseId){
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        if(courseBase == null){
            return null;
        }
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        if(courseMarket != null){
            BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);
        }
        //查询分类名称:大分类和小分类的名
        CourseCategory courseCategoryBySt = courseCategoryMapper.selectById(courseBase.getSt());
        courseBaseInfoDto.setStName(courseCategoryBySt.getName());
        CourseCategory courseCategoryByMt = courseCategoryMapper.selectById(courseBase.getMt());
        courseBaseInfoDto.setMtName(courseCategoryByMt.getName());
        return courseBaseInfoDto;
    }

    @Autowired
    CourseMarketService courseMarketService;

    @Transactional
    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto dto) {
        //业务规则校验， 1、本机构只允许修改本机构的课程
        // 2、课程id对应的数据也必须之前就存在
        Long courseId = dto.getId();
        CourseBase courseBase_u = courseBaseMapper.selectById(courseId);
        if(courseBase_u==null){
            XueChengPlusException.cast("课程信息不存在");
        }
        if(!companyId.equals(courseBase_u.getCompanyId())){
            XueChengPlusException.cast("本机构只允许修改本机构的课程");
        }
        //封装数据
        //将请求参数拷贝到待修改对象中
        BeanUtils.copyProperties(dto,courseBase_u);
        courseBase_u.setChangeDate(LocalDateTime.now());
        //更新到数据库
        int i = courseBaseMapper.updateById(courseBase_u);
        //查询课程营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        if(courseMarket==null){
            courseMarket = new CourseMarket();
        }
        //判断是否收费
        String charge = dto.getCharge();
        if(charge.equals("201001")){
            Float price = dto.getPrice();
            if(price == null || price.floatValue()<=0){
                XueChengPlusException.cast("课程设置了收费价格不能为空且必须大于0");
            }
        }
        //将dto中的课程营销信息拷贝至courseMarket对象中
        BeanUtils.copyProperties(dto,courseMarket);
        boolean save = courseMarketService.saveOrUpdate(courseMarket);
        return getCourseBaseInfo(courseId);
    }

    @Autowired
    private CourseTeacherMapper courseTeacherMapper;
    @Autowired
    private TeachplanMapper teachplanMapper;
    @Autowired
    private TeachplanMediaMapper teachplanMediaMapper;

    @Transactional
    @Override
    public void deleteAll(Long id) {
        // 删除课程相关的基本信息、营销信息、课程计划及对应媒资信息、课程教师信息。
        baseMapper.deleteById(id);
        courseMarketMapper.deleteById(id);
        // 删除章节信息
        LambdaQueryWrapper<Teachplan> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(Teachplan::getCourseId, id);
        teachplanMapper.delete(wrapper1);
        // 删除教师信息
        LambdaQueryWrapper<CourseTeacher> wrapperTeacher = new LambdaQueryWrapper<>();
        wrapperTeacher.eq(CourseTeacher::getCourseId, id);
        courseTeacherMapper.delete(wrapperTeacher);
        // 删除媒资信息
        LambdaQueryWrapper<TeachplanMedia> wrapperMedia = new LambdaQueryWrapper<>();
        wrapperMedia.eq(TeachplanMedia::getCourseId, id);
        teachplanMediaMapper.delete(wrapperMedia);

    }


    //根据课程id查询课程基本信息，包括基本信息和营销信息  备用方法备用方法备用方法
    public CourseBaseInfoDto getCourseBaseInfo2(long courseId){
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        LambdaQueryWrapper<CourseMarket> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(CourseMarket::getId,courseId);
        CourseMarket courseMarket = courseMarketMapper.selectOne(queryWrapper);
        if(courseBase == null || courseMarket == null){
            return null;
        }
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        courseBaseInfoDto.setPrice(courseMarket.getPrice());
        courseBaseInfoDto.setCharge(courseMarket.getCharge());
        return courseBaseInfoDto;
    }


}
