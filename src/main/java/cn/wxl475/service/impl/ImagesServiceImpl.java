package cn.wxl475.service.impl;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.URLUtil;
import cn.wxl475.mapper.ImagesMapper;
import cn.wxl475.pojo.Page;
import cn.wxl475.pojo.data.Image;
import cn.wxl475.redis.CacheClient;
import cn.wxl475.repo.ImagesEsRepo;
import cn.wxl475.service.ImagesService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static cn.wxl475.redis.RedisConstants.*;

@Slf4j
@Service
public class ImagesServiceImpl extends ServiceImpl<ImagesMapper,Image> implements ImagesService {


    private final ImagesMapper imagesMapper;
    private final ImagesEsRepo imagesEsRepo;
    private final CacheClient cacheClient;
    private final ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Value("${fileServer.urlPrefix}")
    private String urlPrefix;
    @Value("${fileServer.imagesPathInVM}")
    private String imagesPathInVM; //linux

    @Autowired
    public ImagesServiceImpl(ImagesMapper imagesMapper, ImagesEsRepo imagesEsRepo, CacheClient cacheClient, ElasticsearchRestTemplate elasticsearchRestTemplate) {
        this.imagesMapper = imagesMapper;
        this.imagesEsRepo = imagesEsRepo;
        this.cacheClient = cacheClient;
        this.elasticsearchRestTemplate = elasticsearchRestTemplate;
    }


    /**
     * 上传图片
     *
     * @param images            图片
     * @param userId            用户id
     * @return ArrayList<Image> 图片列表
     */
    @Override
    @Transactional
    public ArrayList<Image> uploadImagesWithNewTypes(ArrayList<MultipartFile> images,ArrayList<String> newImageTypes,
                                         ArrayList<Integer> newImageTypesIndex, Long userId) {
        // 上传文件
        CompletionService<Image> completionService = ThreadUtil.newCompletionService();
        ArrayList<Future<Image>> futures = new ArrayList<>();
        for (int i=0;i<images.size();i++){
            MultipartFile image = images.get(i);
            if(newImageTypesIndex.contains(i)&& !Objects.requireNonNull(image.getContentType()).contains(newImageTypes.get(newImageTypesIndex.indexOf(i)))){
                AtomicReference<String> newImageType = new AtomicReference<>(newImageTypes.get(newImageTypesIndex.indexOf(i)));
                futures.add(completionService.submit(() -> {
                    Long snowflakeNextId = IdUtil.getSnowflakeNextId();
                    String originalFilename = image.getOriginalFilename();
                    String newImageName = snowflakeNextId + "_" + originalFilename;
                    File file = new File(imagesPathInVM + newImageName);
                    image.transferTo(file);
                    String newImageTypeString = newImageType.get();
                    newImageName = snowflakeNextId + "_" + Objects.requireNonNull(originalFilename).substring(0, originalFilename.lastIndexOf(".")+1)+newImageTypeString;
                    File file1 = FileUtil.file(imagesPathInVM + newImageName);
                    ImgUtil.convert(file,file1);
                    boolean deleted = file.delete();
                    log.info("删除原图片："+deleted);
                    return new Image(
                            snowflakeNextId,
                            userId,
                            URLUtil.normalize(
                                    urlPrefix +
                                            "images/" +
                                            newImageName
                            ),
                            newImageName.substring(newImageName.indexOf("_")+1),
                            "image/"+newImageTypeString,
                            image.getSize(),
                            null,
                            null
                    );
                }));
            }else {
                uploadWithoutNewType(userId, completionService, futures, image, imagesPathInVM, urlPrefix);
            }
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

    private static void uploadWithoutNewType(Long userId, CompletionService<Image> completionService, ArrayList<Future<Image>> futures, MultipartFile image, String imagesPathInVM, String urlPrefix) {
        futures.add(completionService.submit(() -> {
            Long snowflakeNextId = IdUtil.getSnowflakeNextId();
            String originalFilename = image.getOriginalFilename();
            String newImageName = snowflakeNextId + "_" + originalFilename;
            File file = new File(imagesPathInVM + newImageName);
            image.transferTo(file);
            return new Image(
                    snowflakeNextId,
                    userId,
                    URLUtil.normalize(
                            urlPrefix +
                                    "images/" +
                                    newImageName
                    ),
                    originalFilename,
                    image.getContentType(),
                    image.getSize(),
                    null,
                    null
            );
        }));
    }

    @Override
    public ArrayList<Image> uploadImages(ArrayList<MultipartFile> images, Long userId) {
        // 上传文件
        CompletionService<Image> completionService = ThreadUtil.newCompletionService();
        ArrayList<Future<Image>> futures = new ArrayList<>();
        for (MultipartFile image : images) {
            uploadWithoutNewType(userId, completionService, futures, image, imagesPathInVM, urlPrefix);
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
    public void deleteImages(ArrayList<Long> imageIds)throws Exception {
        try {
            imagesMapper.deleteBatchIds(imageIds);
            imagesEsRepo.deleteAllById(imageIds);
            imageIds.forEach(imageId-> cacheClient.delete(CACHE_IMAGE_DETAIL_KEY +imageId));
        }catch (Exception e){
            log.info(Arrays.toString(e.getStackTrace()));
            throw new Exception(e);
        }
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
    public Page<Image> searchImagesWithKeyword(String keyword, Integer pageNum, Integer pageSize, String sortField, Integer sortOrder) {
        Page<Image> images = new Page<>(0L,new ArrayList<>());
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder().withPageable(PageRequest.of(pageNum-1, pageSize));
        if(keyword!=null && !keyword.isEmpty()){
            queryBuilder.withQuery(QueryBuilders.multiMatchQuery(keyword,"imageName","imageUrl","imageType"));
        }
        if(sortField==null || sortField.isEmpty()){
            sortField = "imageId";
        }
        if(sortOrder==null || !(sortOrder==1 || sortOrder==-1)){
            sortOrder=-1;
        }
        queryBuilder.withSorts(SortBuilders.fieldSort(sortField).order(sortOrder==-1?SortOrder.DESC:SortOrder.ASC));
        SearchHits<Image> hits = elasticsearchRestTemplate.search(queryBuilder.build(), Image.class);
        hits.forEach(image -> images.getData().add(image.getContent()));
        images.setTotalNumber(hits.getTotalHits());
        return images;
    }

    /**
     * 按图片id查询
     *
     * @param imageIds   图片id
     * @return Image    图片
     */
    @Override
    @DS("slave")
    public ArrayList<Image> searchImagesByIds(ArrayList<Long> imageIds) {
        ArrayList<Image> images = new ArrayList<>();
        imageIds.forEach(imageId->{
            Image image = cacheClient.queryWithPassThrough(CACHE_IMAGE_DETAIL_KEY, LOCK_IMAGE_DETAIL_KEY,imageId,Image.class,imagesMapper::selectById, CACHE_IMAGE_DETAIL_TTL, TimeUnit.MINUTES);
            if(image!=null){
                images.add(image);
            }
        });
        return images;
    }
}
