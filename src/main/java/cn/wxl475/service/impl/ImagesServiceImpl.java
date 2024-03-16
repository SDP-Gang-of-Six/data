package cn.wxl475.service.impl;

import cn.hutool.core.thread.ThreadUtil;
import cn.wxl475.mapper.ImagesMapper;
import cn.wxl475.minio.MinioUtils;
import cn.wxl475.pojo.Image;
import cn.wxl475.redis.CacheClient;
import cn.wxl475.service.ImagesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;

@Slf4j
@Service
public class ImagesServiceImpl implements ImagesService {

    @Autowired
    private ImagesMapper imagesMapper;

    @Autowired
    private MinioUtils minioUtils;

    /**
     * 上传图片
     * @param images
     * @param userId
     * @return
     */
    @Override
    @Transactional
    public ArrayList<Image> uploadImages(ArrayList<MultipartFile> images, Long userId) {
        // 上传文件
        CompletionService<Image> completionService = ThreadUtil.newCompletionService();
        ArrayList<Future<Image>> futures = new ArrayList<>();
        for(MultipartFile image : images){
            futures.add(completionService.submit(() -> {
                String url = minioUtils.uploadFile(image, "images/", "pet-hospital");
                return new Image(
                        null,
                        userId,
                        url,
                        image.getOriginalFilename(),
                        Objects.requireNonNull(image.getOriginalFilename()).substring(image.getOriginalFilename().lastIndexOf(".") + 1),
                        image.getSize(),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        false);
            }));
        }
        ArrayList<Image> imageList = new ArrayList<>();
        for(Future<Image> future : futures){
            Image image = new Image();
            try {
                image = future.get();
            } catch (Exception e) {
                log.error("线程结果获取失败", e);
            }
            String url = image.getImageUrl();
            if(!("上传文件为空".equals(url) || "上传文件失败".equals(url))){
                imagesMapper.insert(image);
            }
            imageList.add(image);
        }
        return imageList;
    }

    @Override
    public Object deleteImages(ArrayList<String> imageIds) {
        return null;
    }
}
