package cn.wxl475.service;

import cn.wxl475.pojo.Image;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;

public interface ImagesService {
    /**
     * 上传图片
     * @param images
     * @param userId
     * @return
     */
    ArrayList<Image> uploadImages(ArrayList<MultipartFile> images, Long userId);
}
