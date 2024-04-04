package cn.wxl475.controller;

import cn.wxl475.pojo.Result;
import cn.wxl475.service.ImagesService;
import cn.wxl475.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;

@Slf4j
@RestController
@RequestMapping("/data")
public class ImagesController {

    @Value("${jwt.signKey}")
    private String signKey;

    private final ImagesService imagesService;

    @Autowired
    public ImagesController(ImagesService imagesService) {
        this.imagesService = imagesService;
    }

    /**
     * 图片-批量/单个-上传图片
     *
     * @param Authorization token
     * @param images        图片
     * @return Result      响应
     */
    @PostMapping("/uploadImages")
    public Result uploadImages(@RequestHeader String Authorization,
                               @RequestBody ArrayList<MultipartFile> images,
                               @RequestParam ArrayList<String> newImageTypes,
                               @RequestParam ArrayList<Integer> newImageTypesIndex) {
        Claims claims = JwtUtils.parseJWT(Authorization, signKey);
        if (images == null || images.isEmpty()) {
            return Result.error("上传图片为空");
        }
        if (newImageTypes.isEmpty()) {
            return Result.success(imagesService.uploadImages(images, Long.valueOf(claims.get("uid").toString())));
        }
        return Result.success(imagesService.uploadImagesWithNewTypes(images, newImageTypes, newImageTypesIndex, Long.valueOf(claims.get("uid").toString())));
    }

    /**
     * 图片-批量/单个-删除图片
     *
     * @param imageIds 图片id
     * @return Result      响应
     */
    @PostMapping("/deleteImages")
    public Result deleteImages(@RequestBody ArrayList<Long> imageIds) {
        if (imageIds == null ||imageIds.isEmpty()) {
            return Result.error("无图片需要删除");
        }
        try {
            imagesService.deleteImages(imageIds);
        } catch (Exception e) {
            log.info(Arrays.toString(e.getStackTrace()));
            return Result.error(e.getMessage());
        }
        return Result.success();
    }

    /**
     * 关键词分页查询图片
     *
     * @param keyword  图片id
     * @param pageNum  页码
     * @param pageSize 页大小
     * @return Result      响应
     */
    @PostMapping("/searchImagesByKeyword")
    public Result searchImagesByKeyword(@RequestParam(required = false) String keyword,
                                        @RequestParam Integer pageNum,
                                        @RequestParam Integer pageSize,
                                        @RequestParam(required = false) String sortField,
                                        @RequestParam(required = false) Integer sortOrder) {
        if (pageNum <= 0 || pageSize <= 0) {
            return Result.error("页码或页大小不合法");
        }
        return Result.success(imagesService.searchImagesWithKeyword(keyword, pageNum, pageSize, sortField, sortOrder));
    }

    /**
     * 图片-批量/单个-按图片id查询
     *
     * @param imageIds 图片ids
     * @return Result      响应
     */
    @PostMapping("/searchImagesByIds")
    public Result searchImagesByIds(@RequestBody ArrayList<Long> imageIds) {
        return Result.success(imagesService.searchImagesByIds(imageIds));
    }
}
