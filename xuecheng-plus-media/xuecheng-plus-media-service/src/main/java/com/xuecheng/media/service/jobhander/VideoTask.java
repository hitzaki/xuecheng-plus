package com.xuecheng.media.service.jobhander;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;


@Slf4j
@Component
public class VideoTask {
    @Autowired
    MediaFileService mediaFileService;
    @Autowired
    MediaFileProcessService mediaFileProcessService;
    @Value("${videoprocess.ffmpegpath}")
    String ffmpegpath;

    @XxlJob("videoJobHander")
    public void videoJobHander() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        List<MediaProcess> mediaProcessList = null;
        int size = 0;
        try {
            //取出 2 条记录，一次处理视频数量不要超过 cpu 核心数
            mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, 2);
            size = mediaProcessList.size();
            log.debug("取出待处理视频任务{}条", size);
            if (size <= 0) {return;}
        } catch (Exception e) {
            e.printStackTrace();
            return ;
        }
        //启动 size 个线程的线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(size);
        //计数器
        CountDownLatch countDownLatch = new CountDownLatch(size);
        //将处理任务加入线程池
        mediaProcessList.forEach(mediaProcess -> {
            threadPool.execute(() -> {
                //下边是处理逻辑
                //桶
                String bucket = mediaProcess.getBucket();
                //存储路径
                String filePath = mediaProcess.getFilePath();
                //原始视频的 md5 值
                String fileId = mediaProcess.getFileId();
                //原始文件名称
                String filename = mediaProcess.getFilename();

                if("2".equals(mediaProcess.getStatus())){
                    log.debug("视频已经处理成功，不再处理,文件:{},路 径:{}",filename,filePath);
                    return ;
                }
                //将要处理的文件下载到服务器上
                File originalFile = null;
                //处理结束的视频文件
                File mp4File = null;
                try {
                    originalFile = File.createTempFile("original", null);
                    mp4File = File.createTempFile("mp4", ".mp4");
                } catch (IOException e) {
                    log.error("处理视频前创建临时文件失败");
                    countDownLatch.countDown();
                    return;
                }
                try {
                    //下载文件
                    mediaFileService.downloadFileFromMinIO(originalFile, mediaProcess.getBucket(), mediaProcess.getFilePath());
                } catch (Exception e) {
                    log.error("处理视频前下载原始文件:{},出错:{}", mediaProcess.getFilePath(), e.getMessage());
                    countDownLatch.countDown();
                    return;
                }
                //视频处理结果
                String result = null;
                try {
                    //开始处理视频
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath, originalFile.getAbsolutePath(), mp4File.getName(), mp4File.getAbsolutePath());
                    //开始视频转换，成功将返回 success
                    result = videoUtil.generateMp4();
                } catch (Exception e) {
                    log.error("处理视频文件:{},出错:{}", mediaProcess.getFilePath(), e.getMessage());
                    countDownLatch.countDown();
                    return;
                }
                if(!result.equals("success")){
                    //记录错误信息
                    log.error("处理视频失败,视频地址:{},错误信 息:{}",bucket+filePath,result);

                    mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(),"3",fileId,null,result);
                    countDownLatch.countDown();
                    return ;
                }
                //将 mp4 上传至 minio
                //文件路径
                String objectName = null;
                try {
                    objectName = getFilePath(fileId, ".mp4");
                    mediaFileService.addMediaFilesToMinIO(mp4File.getAbsolutePath(),bucket,objectName);
                } catch (Exception e) {
                    log.error("上传视频失败,视频地址:{},错误信 息:{}",bucket+objectName,e.getMessage());
                    countDownLatch.countDown();
                    return ;
                }
                try {
                    //访问 url
                    String url = "/"+bucket+"/"+objectName;
                    //将 url 存储至数据，并更新状态为成功，并将待处理视频记录删除存入历史
                    mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(),"2",fileId,url,null);
                } catch (Exception e) {
                    log.error("视频信息入库失败,视频地址:{},错误信 息:{}",bucket+objectName,e.getMessage());
                }
                countDownLatch.countDown();
            });
        });
        //等待,给一个充裕的超时时间,防止无限等待，到达超时时间还没有处理完成则结束任务
        countDownLatch.await(30, TimeUnit.MINUTES);
    }


    private String getFilePath(String fileMd5,String fileExt){
        return fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" +fileMd5 +fileExt;
    }
}

