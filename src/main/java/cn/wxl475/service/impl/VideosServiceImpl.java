package cn.wxl475.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.URLUtil;
import cn.wxl475.mapper.VideosMapper;
import cn.wxl475.pojo.data.Video;
import cn.wxl475.redis.CacheClient;
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
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

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
    public Boolean uploadOneVideoSharding(MultipartFile videoSharding, String videoMd5, String shardingMd5, String shardingInVideoIndex, String shardingInVideoStart, String shardingInVideoEnd, String allShardingNums, String videoSize) throws IOException {
        String shardingPath = videoShardingsPathInVM + shardingMd5 + "_" + shardingInVideoIndex;
        File shardingFile = new File(shardingPath);
        videoSharding.transferTo(shardingFile);
        Map<String, String> map = new HashMap<>();
        map.put("sharding_path_" + shardingInVideoIndex, shardingPath);//分片存储路径
        map.put("sharding_start_end_" + shardingInVideoIndex, shardingInVideoStart + "_" + shardingInVideoEnd);
        map.put("sharding_md5_" + shardingInVideoIndex, shardingMd5);
        map.put("video_size", videoSize);
        map.put("video_sharding_num", allShardingNums);
        cacheClient.setHash(videoMd5, map);
        return true;
    }

    @Override
    public Boolean checkOneVideoSharding(String videoMd5, String shardingInVideoIndex, String shardingMd5) {
        Object o = cacheClient.getHashValue(videoMd5, "sharding_md5_" + shardingInVideoIndex);
        if (shardingMd5.equals(o)) {
            return true;
        }
        return false;
    }

    @Override
    public Video mergeVideoShardings(String videoMd5, String videoOriginalName, Long uid)throws IOException{
        boolean flag = checkBeforeMerge(videoMd5);
        if(!flag){
            return null;
        }
        //是否已经上传过
        Object videoPath = cacheClient.getHashValue(videoMd5, "video_path");
        //上传过，直接复制已上传文件
        if(videoPath != null){
            String source = videoPath.toString();
            File file = new File(source);
            if (!file.getName().equals(videoOriginalName)) {
                Long snowflakeNextId = IdUtil.getSnowflakeNextId();
                String newVideoName=snowflakeNextId+"_"+videoOriginalName;
                File target = new File(videosPathInVM +newVideoName);
                Files.copy(file.toPath(), target.toPath());
                Video video = new Video(
                        snowflakeNextId,
                        uid,
                        URLUtil.normalize(
                            urlPrefix +
                            "videos/" +
                            newVideoName
                        ),
                        videoOriginalName,
                        videoOriginalName.substring(videoOriginalName.lastIndexOf(".")+1),
                        file.length(),
                        null,
                        null,
                        false
                );
                videosMapper.insert(video);
                videoEsRepo.save(video);
                return video;
            }
        }
        //合并分片,按照分片的索引顺序进行合并
        Integer videoShardingNum = (Integer) cacheClient.getHashValue(videoMd5, "video_sharding_num");
        Long snowflakeNextId = IdUtil.getSnowflakeNextId();
        String newVideoName=snowflakeNextId+"_"+videoOriginalName;
        File videoFile = new File(videosPathInVM + newVideoName);
        RandomAccessFile fileOutputStream;
        RandomAccessFile fileInputStream;
        fileOutputStream =new RandomAccessFile(videoFile,"rw");
        for(int i = 0; i < videoShardingNum; i++) {
            String shardingPath = (String) cacheClient.getHashValue(videoMd5, "sharding_path_" + i);
            File shardingFile = new File(shardingPath);
            fileInputStream = new RandomAccessFile(shardingFile, "r");
            int len=-1;
            byte[] bytes = new byte[1024*1024];
            while ((len = fileInputStream.read(bytes)) != -1) {
                fileOutputStream.write(bytes,0,len);
            }
            if(fileInputStream!=null)fileInputStream.close();
        }

    }
    private boolean checkBeforeMerge(String videoMd5) {
        Map map = cacheClient.getHashMap(videoMd5);
        Object videoShardingNum = map.get("video_sharding_num");
        int i = 0;
        for (Object hashKey : map.keySet()) {
            if (hashKey.toString().startsWith("sharding_md5_")) {
                ++i;
            }
        }
        if (Integer.valueOf(videoShardingNum.toString())==(i)) {
            return true;
        }
        return false;
    }
}
