package cn.wxl475.service.impl;

import cn.wxl475.mapper.ImagesMapper;
import cn.wxl475.pojo.Image;
import cn.wxl475.service.ImagesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImagesServiceImpl implements ImagesService {

    @Autowired
    private ImagesMapper imagesMapper;

    @Override
    public Image insertImage(Image image1) {
        imagesMapper.insert(image1);
        return image1;
    }
}
