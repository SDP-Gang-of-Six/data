package cn.wxl475.service;

import cn.wxl475.pojo.data.Video;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface VideosService {
    Boolean uploadOneVideoSharding(MultipartFile videoSharding, String videoMd5, String shardingMd5, String shardingInVideoIndex, String allShardingNums) throws IOException;

    Boolean checkOneVideoSharding(String videoMd5, String shardingInVideoIndex, String shardingMd5);

    Video mergeVideoSharding(String videoMd5, String videoOriginalName, Long uid) throws IOException;
}
