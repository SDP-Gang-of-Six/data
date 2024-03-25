package cn.wxl475.controller;

import cn.wxl475.pojo.Result;
import cn.wxl475.service.ImagesService;
import cn.wxl475.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;

@Slf4j
@RestController
@RequestMapping("/data")
public class ImagesController {

    @Value("${jwt.signKey}")
    private String signKey;

    @Autowired
    private ImagesService imagesService;

    /**
     * 上传图片
     *
     * @param Authorization token
     * @param images       图片
     * @return Result      响应
     */
    @PostMapping("/uploadImages")
    public Result uploadImages(@RequestHeader String Authorization, @RequestBody ArrayList<MultipartFile> images) {
        Claims claims = JwtUtils.parseJWT(Authorization, signKey);
        if(images.isEmpty()){
            return Result.error("上传图片为空");
        }
        return Result.success(imagesService.uploadImages(images,Long.valueOf(claims.get("uid").toString())));
    }

    /**
     * 删除图片
     *
     * @param Authorization token
     * @param imageIds       图片id
     * @return  Result      响应
     */
    @PostMapping("/deleteImages")
    public Result deleteImages(@RequestHeader String Authorization, @RequestBody ArrayList<Long> imageIds) {
        if(imageIds.isEmpty()){
            return Result.error("无图片需要删除");
        }
        return Result.success(imagesService.deleteImages(imageIds));
    }

    /**
     * 关键词分页查询图片
     *
     * @param Authorization token
     * @param keyword       图片id
     * @param pageNum      页码
     * @param pageSize     页大小
     * @return  Result      响应
     */
    @PostMapping("/searchImagesByKeyword")
    public Result searchImagesByKeyword(@RequestHeader String Authorization,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam Integer pageNum,
                                        @RequestParam Integer pageSize,
                                        @RequestParam(required = false) String sortField,
                                        @RequestParam(required = false) Integer sortOrder){
        if(pageNum<=0||pageSize<=0){
            return Result.error("页码或页大小不合法");
        }
        return Result.success(imagesService.searchImagesWithKeyword(keyword,pageNum,pageSize,sortField,sortOrder));
    }

    /**
     * 按图片id查询
     *
     * @param Authorization token
     * @param imageId       图片id
     * @return  Result      响应
     */
    @PostMapping("/searchImageById")
    public Result searchImagesById(@RequestHeader String Authorization,@RequestParam Long imageId) {
        return Result.success(imagesService.searchImagesById(imageId));
    }
}
