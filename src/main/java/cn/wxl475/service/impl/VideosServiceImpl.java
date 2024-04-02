package cn.wxl475.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.URLUtil;
import cn.wxl475.mapper.VideosMapper;
import cn.wxl475.pojo.data.Video;
import cn.wxl475.redis.CacheClient;
import cn.wxl475.repo.VideoEsRepo;
import cn.wxl475.service.VideosService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static cn.wxl475.redis.RedisConstants.*;

@Slf4j
@Service
public class VideosServiceImpl extends ServiceImpl<VideosMapper, Video> implements VideosService {

    private final VideosMapper videosMapper;
    private final VideoEsRepo videoEsRepo;
    private final CacheClient cacheClient;
    private final ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Value("${fileServer.urlPrefix}")
    private String urlPrefix;
    @Value("${fileServer.videosPathInVM}")
    private String videosPathInVM; //linux
    @Value("${fileServer.videoShardingPathInVM}")
    private String videoShardingPathInVM; //linux

    @Autowired
    public VideosServiceImpl(VideosMapper videosMapper, VideoEsRepo videoEsRepo, CacheClient cacheClient, ElasticsearchRestTemplate elasticsearchRestTemplate) {
        this.videosMapper = videosMapper;
        this.videoEsRepo = videoEsRepo;
        this.cacheClient = cacheClient;
        this.elasticsearchRestTemplate = elasticsearchRestTemplate;
    }

    /**
     * 视频-上传视频分片
     * @param videoSharding
     * @param videoMd5
     * @param shardingMd5
     * @param shardingInVideoIndex
     * @param allShardingNums
     * @return
     * @throws IOException
     */
    @Override
    public void uploadOneVideoSharding(MultipartFile videoSharding, String videoMd5, String shardingMd5, String shardingInVideoIndex, String allShardingNums)throws Exception{
        try {
            String shardingPath = videoShardingPathInVM + shardingMd5 + "_" + shardingInVideoIndex;
            File shardingFile = new File(shardingPath);
            videoSharding.transferTo(shardingFile);
            Map<String, String> map = new HashMap<>();
            map.put("sharding_path_" + shardingInVideoIndex, shardingPath);//分片存储路径
            map.put("sharding_md5_" + shardingInVideoIndex, shardingMd5);
            map.put("video_sharding_num", allShardingNums);
            cacheClient.setHashMap(videoMd5, map);
        } catch (Exception e) {
            log.info("上传视频分片失败：" + Arrays.toString(e.getStackTrace()));
            throw new Exception(e);
        }
    }

    /**
     * 视频-检查视频分片是否上传过
     * @param videoMd5
     * @param shardingInVideoIndex
     * @param shardingMd5
     * @return
     */
    @Override
    public Boolean checkOneVideoSharding(String videoMd5, String shardingInVideoIndex, String shardingMd5) {
        String o = cacheClient.getHashValue(videoMd5, "sharding_md5_" + shardingInVideoIndex);
        return shardingMd5.equals(o);
    }

    /**
     * 视频-合并视频分片
     * @param videoMd5
     * @param videoOriginalName
     * @param uid
     * @return
     * @throws IOException
     */
    @Override
    @Transactional
    public Video mergeVideoSharding(String videoMd5, String videoOriginalName, Long uid) throws Exception {
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

    /**
     * 视频-删除视频分片
     * @param videoMd5
     * @return
     */
    @Override
    public void deleteVideoSharding(String videoMd5)throws Exception {
        try {
            delTmpFile(videoMd5);
        }catch (Exception e) {
            throw new Exception(e);
        }
    }

    /**
     * 视频-批量/单个-删除视频
     * @param videoIds
     * @return
     */
    @Override
    public void deleteVideos(ArrayList<Long> videoIds)throws Exception {
        try {
            videosMapper.deleteBatchIds(videoIds);
            videoEsRepo.deleteAllById(videoIds);
            videoIds.forEach(videoId-> cacheClient.delete(CACHE_VIDEODETAIL_KEY+videoId));
        }catch (Exception e) {
            throw new Exception(e);
        }
    }

    /**
     * 视频-关键词分页查询视频
     * @param keyword
     * @param pageNum
     * @param pageSize
     * @param sortField
     * @param sortOrder
     * @return
     */
    @Override
    public ArrayList<Video> searchVideosByKeyword(String keyword, Integer pageNum, Integer pageSize, String sortField, Integer sortOrder) {
        ArrayList<Video> videos = new ArrayList<>();
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder().withPageable(PageRequest.of(pageNum-1, pageSize));
        if(keyword!=null && !keyword.isEmpty()){
            queryBuilder.withQuery(QueryBuilders.multiMatchQuery(keyword,"videoName","videoUrl","videoType","createTime","updateTime"));
        }
        if(sortField==null || sortField.isEmpty()){
            sortField = "videoId";
        }
        if(sortOrder==null || !(sortOrder==1 || sortOrder==-1)){
            sortOrder=-1;
        }
        queryBuilder.withSorts(SortBuilders.fieldSort(sortField).order(sortOrder==-1? SortOrder.DESC:SortOrder.ASC));
        SearchHits<Video> hits = elasticsearchRestTemplate.search(queryBuilder.build(), Video.class);
        hits.forEach(video -> videos.add(video.getContent()));
        return videos;
    }

    /**
     * 视频-批量/单个-按视频id查询
     * @param videoIds
     * @return
     */
    @Override
    @DS("slave")
    public ArrayList<Video> searchVideosByIds(ArrayList<Long> videoIds) {
        ArrayList<Video> videos = new ArrayList<>();
        videoIds.forEach(videoId->{
            Video video = cacheClient.queryWithPassThrough(CACHE_VIDEODETAIL_KEY,LOCK_VIDEODETAIL_KEY,videoId,Video.class,videosMapper::selectById,CACHE_VIDEODETAIL_TTL, TimeUnit.MINUTES);
            if(video!=null){
                videos.add(video);
            }
        });
        return videos;
    }

    /**
     * 检查视频分片是否全部上传完毕
     * @param videoMd5
     * @return
     */
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

    /**
     * 删除临时文件
     * @param md5Value
     */
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
