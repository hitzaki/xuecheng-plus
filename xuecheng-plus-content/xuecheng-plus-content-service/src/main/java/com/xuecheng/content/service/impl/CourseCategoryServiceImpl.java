package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <p>
 * 课程分类 服务实现类
 * </p>
 *
 * @author lxc
 */
@Slf4j
@Service
public class CourseCategoryServiceImpl extends ServiceImpl<CourseCategoryMapper, CourseCategory> implements CourseCategoryService {

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        //查询数据库得到的课程分类
        List<CourseCategoryTreeDto> courseList = baseMapper.selectTreeNodes(id);
        //最终返回的列表
        List<CourseCategoryTreeDto> resultList = new ArrayList<>();
        HashMap<String, CourseCategoryTreeDto> map = new HashMap<>();
        // 迭代课程分类的记录
        for(CourseCategoryTreeDto item: courseList){
            map.put(item.getId(), item);
            // 只将根节点的下级节点放入list, 此时的id就是根节点,一般传入1
            if(id.equals(item.getParentid())){
                resultList.add(item);
            }
            CourseCategoryTreeDto courseCategoryTreeDto = map.get(item.getParentid());
            if(courseCategoryTreeDto!=null){
                // 孩子结点为null就new一个
                if(courseCategoryTreeDto.getChildrenTreeNodes() ==null){
                    courseCategoryTreeDto.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                }
            //向节点的下级节点list加入节点
            courseCategoryTreeDto.getChildrenTreeNodes().add(item);
            }
        }  // 迭代结束
        return resultList;
    }
}
