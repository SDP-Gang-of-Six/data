package cn.wxl475.service;

import cn.wxl475.pojo.Image;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;

public interface ImagesService {
    ArrayList<Image> uploadImages(ArrayList<MultipartFile> images, Long userId);
}
