package cn.wxl475.service.impl;

import cn.wxl475.mapper.ImagesMapper;
import cn.wxl475.mapper.VideosMapper;
import cn.wxl475.pojo.data.Video;
import cn.wxl475.redis.CacheClient;
import cn.wxl475.repo.ImagesEsRepo;
import cn.wxl475.repo.VideoEsRepo;
import cn.wxl475.service.VideosService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Slf4j
@Service
public class VideosServiceImpl extends ServiceImpl<VideosMapper, Video> implements VideosService {
    @Autowired
    private VideosMapper videosMapper;
    @Autowired
    private VideoEsRepo videoEsRepo;
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Value("${fileServer.urlPrefix}")
    private String urlPrefix;

    private final String videosPathInVM = "/data/pet-hospital/videos/"; //linux
    private final String videoShardingsPathInVM = "/data/pet-hospital/videos/shardings/"; //linux
    private final String videosPathInWindows = "D:/"; //windows
    private final String videoShardingsPathInWindows = "D:/"; //windows
    @Override
    public Boolean uploadOneVideoSharding(MultipartFile videoSharding, String videoMd5, String shardingMd5, String shardingInVideoIndex, String shardingInVideoStart, String shardingInVideoEnd, String allShardingNums, String videoSize) {
        String shardingsPath = videoShardingsPathInVM + shardingMd5 + "_" + shardingInVideoIndex;
        File shardingFile = new File(shardingsPath);
        try {
            videoSharding.transferTo(shardingFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
