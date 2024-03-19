package cn.wxl475.service.impl;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.URLUtil;
import cn.wxl475.mapper.ImagesMapper;
import cn.wxl475.pojo.Image;
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

    @Value("${fileServer.urlPrefix}")
    private String urlPrefix;

    private final String imagesPathInVM = "/data/pet-hospital/images/";


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
                Image image1 = new Image(
                        null,
                        userId,
                        null,
                        image.getOriginalFilename(),
                        image.getContentType(),
                        image.getSize(),
                        null,
                        null,
                        false
                );
                String newFileName = image1.getImageId() + "___" + image.getOriginalFilename();
                image.transferTo(new File(imagesPathInVM+newFileName)); //linux
                image1.setImageUrl(URLUtil.normalize(
                            urlPrefix +
                                "/images/" +
                                newFileName
                ));
                return image1;
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
            imageList.add(image); //要在数据库操作后再加入列表，获取插入后返回的id和时间
        }
        return imageList;
    }
}
