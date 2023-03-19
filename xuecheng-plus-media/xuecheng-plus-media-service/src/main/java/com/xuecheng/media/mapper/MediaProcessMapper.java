package com.xuecheng.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.media.model.po.MediaProcess;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface MediaProcessMapper extends BaseMapper<MediaProcess> {
    /**
     * @description 根据分片参数获取待处理任务
     * @param shardTotal 分片总数
     * @param shardindex 分片序号
     * @param count 任务数
     * @return
    java.util.List<com.xuecheng.media.model.po.MediaProcess>
     * @author Mr.M
     * @date 2022/9/14 8:54
     */
    @Select("SELECT t.* FROM media_process t WHERE t.id % #{shardTotal} = #{shardindex} and t.status='1' limit #{count}")
    List<MediaProcess> selectListByShardIndex(@Param("shardTotal") int shardTotal,
                                              @Param("shardindex") int shardindex, @Param("count") int count);
}
