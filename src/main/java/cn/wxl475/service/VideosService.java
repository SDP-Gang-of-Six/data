package cn.wxl475.service;

import cn.wxl475.pojo.Page;
import cn.wxl475.pojo.data.Video;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;

public interface VideosService {
    void uploadOneVideoSharding(MultipartFile videoSharding, String videoMd5, String shardingMd5, String shardingInVideoIndex, String allShardingNums) throws Exception;

    Boolean checkOneVideoSharding(String videoMd5, String shardingInVideoIndex, String shardingMd5);

    Video mergeVideoSharding(String videoMd5, String videoOriginalName, Long uid) throws Exception;

    void deleteVideoSharding(String videoMd5) throws Exception;

    void deleteVideos(ArrayList<Long> videoIds)throws Exception;

    Page<Video> searchVideosByKeyword(String keyword, Integer pageNum, Integer pageSize, String sortField, Integer sortOrder);

    ArrayList<Video> searchVideosByIds(ArrayList<Long> videoIds);
}
