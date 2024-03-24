package cn.wxl475.service.impl;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.URLUtil;
import cn.wxl475.mapper.ImagesMapper;
import cn.wxl475.pojo.Image;
import cn.wxl475.repo.ImagesEsRepo;
import cn.wxl475.service.ImagesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;

@Slf4j
@Service
public class ImagesServiceImpl extends ServiceImpl<ImagesMapper,Image> implements ImagesService {

    @Autowired
    private ImagesMapper imagesMapper;
    @Autowired
    private ImagesEsRepo imagesEsRepo;

    @Value("${fileServer.urlPrefix}")
    private String urlPrefix;

    private final String imagesPathInVM = "/data/pet-hospital/images/"; //linux
    private final String imagesPathInWindows = "D:/"; //windows


    /**
     * 上传图片
     *
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
}
