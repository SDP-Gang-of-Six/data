package cn.wxl475.service.impl;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.URLUtil;
import cn.wxl475.mapper.ImagesMapper;
import cn.wxl475.pojo.Image;
import cn.wxl475.redis.CacheClient;
import cn.wxl475.repo.ImagesEsRepo;
import cn.wxl475.service.ImagesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static cn.wxl475.redis.RedisConstants.*;

@Slf4j
@Service
public class ImagesServiceImpl extends ServiceImpl<ImagesMapper,Image> implements ImagesService {

    @Autowired
    private ImagesMapper imagesMapper;
    @Autowired
    private ImagesEsRepo imagesEsRepo;
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Value("${fileServer.urlPrefix}")
    private String urlPrefix;

    private final String imagesPathInVM = "/data/pet-hospital/images/"; //linux
    private final String imagesPathInWindows = "D:/"; //windows


    /**
     * 上传图片
     *
     * @param images            图片
     * @param userId            用户id
     * @return ArrayList<Image> 图片列表
     */
    @Override
    @Transactional
    public ArrayList<Image> uploadImages(ArrayList<MultipartFile> images, Long userId) {
        // 上传文件
        CompletionService<Image> completionService = ThreadUtil.newCompletionService();
        ArrayList<Future<Image>> futures = new ArrayList<>();
        for (MultipartFile image : images) {
            futures.add(completionService.submit(() -> {
                Long snowflakeNextId = IdUtil.getSnowflakeNextId();
                String newFileName = snowflakeNextId + "_" + image.getOriginalFilename();
                image.transferTo(new File(imagesPathInVM+newFileName));
                return new Image(
                        snowflakeNextId,
                        userId,
                        URLUtil.normalize(
                        urlPrefix +
                            "images/" +
                            newFileName
                        ),
                        image.getOriginalFilename(),
                        image.getContentType(),
                        image.getSize(),
                        null,
                        null,
                        false
                );
            }));
        }
        ArrayList<Image> imageList = new ArrayList<>();
        for (Future<Image> future : futures) {
            Image image = new Image();
            try {
                image = future.get();
            } catch (Exception e) {
                imageList.add(image);
                log.error("写文件线程结果获取失败，该线程索引："+futures.indexOf(future), e);
                continue;
            }
            imagesMapper.insert(image);
            imagesEsRepo.save(image);
            imageList.add(image); //要在数据库操作后再加入列表，获取插入后返回的id和时间
        }
        return imageList;
    }

    /**
     * 删除图片
     *
     * @param imageIds 图片id
     * @return Boolean 是否成功
     */
    @Override
    @Transactional
    public Boolean deleteImages(ArrayList<Long> imageIds) {
        imagesMapper.deleteBatchIds(imageIds);
        imagesEsRepo.deleteAllById(imageIds);
        imageIds.forEach(imageId->{
            cacheClient.delete(CACHE_IMAGEDETAIL_KEY+imageId);
        });
        return true;
    }

    /**
     * 按关键词查询图片
     *
     * @param keyword   关键词
     * @param pageNum   页码
     * @param pageSize  页大小
     * @return ArrayList<Image> 图片列表
     */
    @Override
    @Transactional
    public ArrayList<Image> searchImagesWithKeyword(String keyword, Integer pageNum, Integer pageSize) {
        ArrayList<Image> images = new ArrayList<>();
        NativeSearchQuery query = new NativeSearchQueryBuilder().
                withQuery(QueryBuilders.matchQuery("imageName", keyword)).
                withPageable(PageRequest.of(pageNum, pageSize)).
                build();
        SearchHits<Image> hits = elasticsearchRestTemplate.search(query, Image.class);
        hits.forEach(image -> images.add(image.getContent()));
        return images;
    }

    /**
     * 按图片id查询
     *
     * @param imageId   图片id
     * @return Image    图片
     */
    @Override
    @Transactional
    public Image searchImagesById(String imageId) {
        return cacheClient.queryWithPassThrough(CACHE_IMAGEDETAIL_KEY,LOCK_IMAGEDETAIL_KEY,imageId,Image.class,imagesMapper::selectById,CACHE_IMAGEDETAIL_TTL, TimeUnit.MINUTES);
    }
}
