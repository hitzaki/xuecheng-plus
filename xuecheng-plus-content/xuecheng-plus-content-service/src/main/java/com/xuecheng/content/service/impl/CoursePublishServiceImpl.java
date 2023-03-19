package com.xuecheng.content.service.impl;

import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseIndex;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.service.CourseBaseService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.content.service.jobhandler.CoursePublishTask;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CoursePublishServiceImpl implements CoursePublishService {
        @Autowired
        CourseBaseService courseBaseService;
        @Autowired
        TeachplanService teachplanService;
        @Autowired
        CoursePublishPreMapper coursePublishPreMapper;
        @Autowired
        CoursePublishMapper coursePublishMapper;
        @Autowired
        CourseBaseMapper courseBaseMapper;
        @Autowired
        MqMessageService mqMessageService;
        @Autowired
        MediaServiceClient mediaServiceClient;
        @Autowired
        SearchServiceClient searchServiceClient;

        @Override
        public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
                //课程基本信息、营销信息
                CourseBaseInfoDto courseBaseInfo = courseBaseService.getCourseBaseInfo(courseId);
                //课程计划信息
                List<TeachplanDto> teachplanTree= teachplanService.findTeachplanTree(courseId);
                CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
                coursePreviewDto.setCourseBase(courseBaseInfo);
                coursePreviewDto.setTeachplans(teachplanTree);
                return coursePreviewDto;
        }

        @Transactional
        @Override
        public void publish(Long companyId, Long courseId) {
                //约束校验
                //查询课程预发布表
                CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
                if(coursePublishPre == null){
                        XueChengPlusException.cast("请先提交课程审核，审核通过才可以发布 ");
                }
                //本机构只允许提交本机构的课程
                if(!coursePublishPre.getCompanyId().equals(companyId)){
                        XueChengPlusException.cast("不允许提交其它机构的课程。");
                }
                //课程审核状态
                String auditStatus = coursePublishPre.getStatus();
                //审核通过方可发布
                if(!"202004".equals(auditStatus)){
                        XueChengPlusException.cast("操作失败，课程审核通过方可发布。");
                }
                //保存课程发布信息
                saveCoursePublish(courseId);
                //保存消息表
                saveCoursePublishMessage(courseId);
                //删除课程预发布表对应记录
                coursePublishPreMapper.deleteById(courseId);
        }


        /**
         * @description 保存课程发布信息
         * @param courseId 课程 id
         * @return void
         * @author Mr.M
         * @date 2022/9/20 16:32
         */
        private void saveCoursePublish(Long courseId){
                //整合课程发布信息
                //查询课程预发布表
                CoursePublishPre coursePublishPre =
                        coursePublishPreMapper.selectById(courseId);
                if(coursePublishPre == null){
                        XueChengPlusException.cast("课程预发布数据为空");
                }
                CoursePublish coursePublish = new CoursePublish();
                //拷贝到课程发布对象
                BeanUtils.copyProperties(coursePublishPre,coursePublish);
                coursePublish.setStatus("203002");
                CoursePublish coursePublishUpdate = coursePublishMapper.selectById(courseId);
                if(coursePublishUpdate == null){
                        coursePublishMapper.insert(coursePublish);
                }else{
                        coursePublishMapper.updateById(coursePublish);
                }
                //更新课程基本表的发布状态
                CourseBase courseBase = courseBaseMapper.selectById(courseId);
                courseBase.setStatus("203002");
                courseBaseMapper.updateById(courseBase);
        }

        /**
         * @description 保存消息表记录
         * @param courseId 课程 id
         * @return void
         */
        private void saveCoursePublishMessage(Long courseId){
                MqMessage mqMessage = mqMessageService.addMessage(CoursePublishTask.MESSAGE_TYPE, String.valueOf(courseId), null, null);
                if(mqMessage==null){
                        XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
                }
        }


        @Override
        public File generateCourseHtml(Long courseId) {
                //静态化文件
                File htmlFile = null;
                try {
                        //配置 freemarker
                        Configuration configuration = new Configuration(Configuration.getVersion());
                        //加载模板
                        //选指定模板路径,classpath 下 templates 下
                        //得到 classpath 路径
                        String classpath = this.getClass().getResource("/").getPath();
                        configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/"));
                        //设置字符编码
                        configuration.setDefaultEncoding("utf-8");
                        //指定模板文件名称
                        Template template = configuration.getTemplate("course_template.ftl");
                        //准备数据
                        CoursePreviewDto coursePreviewInfo = this.getCoursePreviewInfo(courseId);
                        Map<String, Object> map = new HashMap<>();
                        map.put("model", coursePreviewInfo);
                        //静态化
                        //参数 1：模板，参数 2：数据模型
                        String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
                        // System.out.println(content);
                        //将静态化内容输出到文件中
                        InputStream inputStream = IOUtils.toInputStream(content);
                        //创建静态化文件
                        htmlFile = File.createTempFile("course",".html");
                        log.debug("课程静态化，生成静态文件:{}",htmlFile.getAbsolutePath());
                        //输出流
                        FileOutputStream outputStream = new FileOutputStream(htmlFile);
                        IOUtils.copy(inputStream, outputStream);
                } catch (Exception e) {
                        e.printStackTrace();
                }
                return htmlFile;
        }
        @Override
        public void uploadCourseHtml(Long courseId, File file) {
                MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
                String course = mediaServiceClient.uploadFile(multipartFile,
                        "course", courseId+".html");
                if(course == null){
                        XueChengPlusException.cast("远程调用媒资服务上传文件失败 ");
                }
        }

        @Override
        public Boolean saveCourseIndex(Long courseId) {
                //取出课程发布信息
                CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
                //拷贝至课程索引对象
                CourseIndex courseIndex = new CourseIndex();
                BeanUtils.copyProperties(coursePublish,courseIndex);
                //远程调用搜索服务 api 添加课程信息到索引
                Boolean add = searchServiceClient.add(courseIndex);
                if(!add){
                        XueChengPlusException.cast("添加索引失败");
                }
                return add;
        }


}
