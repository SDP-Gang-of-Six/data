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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    private final String videosPathInVM; //linux
    private final String videoShardingPathInVM; //linux
//    private final String InWindows; //windows

    public VideosServiceImpl() {
        videosPathInVM = "/data/pet-hospital/videos/";
        videoShardingPathInVM = "/data/pet-hospital/videos/sharding/";
//        InWindows = "D:/";
    }

    @Override
    public Boolean uploadOneVideoSharding(MultipartFile videoSharding, String videoMd5, String shardingMd5, String shardingInVideoIndex, String allShardingNums) throws IOException {
        String shardingPath = videoShardingPathInVM + shardingMd5 + "_" + shardingInVideoIndex;
        File shardingFile = new File(shardingPath);
        videoSharding.transferTo(shardingFile);
        Map<String, String> map = new HashMap<>();
        map.put("sharding_path_" + shardingInVideoIndex, shardingPath);//分片存储路径
        map.put("sharding_md5_" + shardingInVideoIndex, shardingMd5);
        map.put("video_sharding_num", allShardingNums);
        cacheClient.setHashMap(videoMd5, map);
        return true;
    }

    @Override
    public Boolean checkOneVideoSharding(String videoMd5, String shardingInVideoIndex, String shardingMd5) {
        String o = cacheClient.getHashValue(videoMd5, "sharding_md5_" + shardingInVideoIndex);
        return shardingMd5.equals(o);
    }

    @Override
    @Transactional
    public Video mergeVideoSharding(String videoMd5, String videoOriginalName, Long uid) throws IOException {
        boolean flag = checkBeforeMerge(videoMd5);
        if (!flag) {
            return null;
        }
        //是否已经上传过
        List<Video> map = videosMapper.selectByMap(new HashMap<>() {{
            put("video_md5", videoMd5);
        }});
        String videoPath = null;
        if (!map.isEmpty()) {
            videoPath = videosPathInVM + map.get(0).getVideoId() + "_" + map.get(0).getVideoName();
        }
        //上传过，直接复制已上传文件
        if (videoPath != null) {
            File file = new File(videoPath);
            if (!file.getName().equals(videoOriginalName)) {
                Long snowflakeNextId = IdUtil.getSnowflakeNextId();
                String newVideoName = snowflakeNextId + "_" + videoOriginalName;
                File target = new File(videosPathInVM + newVideoName);
                Files.copy(file.toPath(), target.toPath());
                Video video = new Video(
                        snowflakeNextId,
                        uid,
                        videoMd5,
                        URLUtil.normalize(
                        urlPrefix +
                            "videos/" +
                            newVideoName
                        ),
                        videoOriginalName,
                        videoOriginalName.substring(videoOriginalName.lastIndexOf(".") + 1),
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
        int videoShardingNum = Integer.parseInt(cacheClient.getHashValue(videoMd5, "video_sharding_num")) ;
        Long snowflakeNextId = IdUtil.getSnowflakeNextId();
        String newVideoName = snowflakeNextId + "_" + videoOriginalName;
        File videoFile = new File(videosPathInVM + newVideoName);
        RandomAccessFile fileOutputStream;
        RandomAccessFile fileInputStream;
        fileOutputStream = new RandomAccessFile(videoFile, "rw");
        for (int i = 0; i < videoShardingNum; i++) {
            String shardingPath = cacheClient.getHashValue(videoMd5, "sharding_path_" + i);
            File shardingFile = new File(shardingPath);
            fileInputStream = new RandomAccessFile(shardingFile, "r");
            int len;
            byte[] bytes = new byte[1024 * 1024];
            while ((len=fileInputStream.read(bytes)) != -1) {
                fileOutputStream.write(bytes, 0, len);
            }
            fileInputStream.close();
        }
        fileOutputStream.close();
        Video video = new Video(
                snowflakeNextId,
                uid,
                videoMd5,
                URLUtil.normalize(
                urlPrefix +
                    "videos/" +
                    newVideoName
                ),
                videoOriginalName,
                videoOriginalName.substring(videoOriginalName.lastIndexOf(".") + 1),
                videoFile.length(),
                null,
                null,
                false
        );
        videosMapper.insert(video);
        videoEsRepo.save(video);
        delTmpFile(videoMd5);
        return video;
    }

    @Override
    public Boolean deleteVideoSharding(String videoMd5) {
        delTmpFile(videoMd5);
        return true;
    }

    private boolean checkBeforeMerge(String videoMd5) {
        Map<String, String> map = cacheClient.getHashMap(videoMd5);
        String videoShardingNum = map.get("video_sharding_num");
        int i = 0;
        for (String hashKey : map.keySet()) {
            if (hashKey.startsWith("sharding_md5_")) {
                ++i;
            }
        }
        return Integer.parseInt(videoShardingNum) == (i);
    }

    private void delTmpFile(String md5Value){
        Map<String, String> map = cacheClient.getHashMap(md5Value);
        ArrayList<String> list = new ArrayList<>();
        for (String hashKey : map.keySet()) {
            if (hashKey.startsWith("sharding_path_")) {
                String filePath = map.get(hashKey);
                File file = new File(filePath);
                boolean flag = file.delete();
                log.info("删除临时文件{}{}", filePath, flag ? "成功" : "失败");
            }
            list.add(hashKey);
        }
        if(!list.isEmpty()) {
            cacheClient.deleteHashKeys(md5Value,list);
        }
    }
}
