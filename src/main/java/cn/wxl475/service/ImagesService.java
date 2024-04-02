package cn.wxl475.service;

import cn.wxl475.pojo.data.Image;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;

public interface ImagesService extends IService<Image> {
    /**
     * 上传图片
     * @param images
     * @param userId
     * @return
     */
    ArrayList<Image> uploadImagesWithNewTypes(ArrayList<MultipartFile> images,ArrayList<String> newImageTypes,
                                  ArrayList<Integer> newImageTypesIndex, Long userId);

    ArrayList<Image> uploadImages(ArrayList<MultipartFile> images, Long userId);

    void deleteImages(ArrayList<Long> imageIds) throws Exception;

    ArrayList<Image> searchImagesWithKeyword(String keyword,Integer pageNum,Integer pageSize,String sortField,Integer sortOrder);

    ArrayList<Image> searchImagesByIds(ArrayList<Long> imageIds);
}
