package cn.wxl475.service.impl;

import cn.hutool.core.thread.ThreadUtil;
import cn.wxl475.mapper.ImagesMapper;
import cn.wxl475.minio.MinioUtils;
import cn.wxl475.pojo.Image;
import cn.wxl475.service.ImagesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;

@Service
public class ImagesServiceImpl implements ImagesService {

    @Autowired
    private ImagesMapper imagesMapper;

    @Autowired
    private MinioUtils minioUtils;

    @Override
    public ArrayList<Image> uploadImages(ArrayList<MultipartFile> images, Long userId) {
        // 上传文件
        CompletionService<Image> completionService = ThreadUtil.newCompletionService();
        ArrayList<Future<Image>> futures = new ArrayList<>();
        for(MultipartFile image : images){
            futures.add(completionService.submit(() -> {
                String url = minioUtils.uploadFile(image, "images/", "pet-hospital");
                Image image1 = new Image(
                        null,
                        userId,
                        url,
                        image.getOriginalFilename(),
                        Objects.requireNonNull(image.getOriginalFilename()).substring(image.getOriginalFilename().lastIndexOf(".") + 1),
                        image.getSize(),
                        null,
                        null,
                        0);
                if(!("上传文件为空".equals(url) || "上传文件失败".equals(url))){
                    imagesMapper.insert(image1);
                }
                return image1;
            }));
        }
        ArrayList<Image> imageList = new ArrayList<>();
        for(Future<Image> future : futures){
            try {
                imageList.add(future.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return imageList;
    }
}
