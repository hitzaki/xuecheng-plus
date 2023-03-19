package com.xuecheng.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuecheng.content.model.po.CoursePublishPre;

/**
 * <p>
 * 课程发布 服务类
 * </p>
 *
 * @author lxc
 * @since 2023-03-05
 */
public interface CoursePublishPreService extends IService<CoursePublishPre> {
    /**
     * @description 提交审核
     * @param courseId 课程 id
     * @return void
     * @author Mr.M
     * @date 2022/9/18 10:31
     */
    public void commitAudit(Long companyId,Long courseId);


}
