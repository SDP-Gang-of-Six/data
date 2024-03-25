package cn.wxl475.service;

import org.springframework.web.multipart.MultipartFile;

public interface VideosService {
    Boolean uploadOneVideoSharding(MultipartFile videoSharding, String videoMd5, String shardingMd5, String shardingInVideoIndex, String shardingInVideoStart, String shardingInVideoEnd, String allShardingNums, String videoSize);
}
