package cn.wxl475.controller;

import cn.wxl475.pojo.Result;
import cn.wxl475.pojo.enums.userType;
import cn.wxl475.service.ImagesService;
import cn.wxl475.utils.JwtUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.*;

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
        if (claims == null) {
            return Result.error("token无效");
        }else if(!((Boolean) claims.get("userType"))){
            return Result.error("权限不足");
        }else if(images == null || images.isEmpty()){
            return Result.error("上传文件为空");
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
        Claims claims = JwtUtils.parseJWT(Authorization, signKey);
        if (claims == null) {
            return Result.error("token无效");
        }else if(!((Boolean) claims.get("userType"))){
            return Result.error("权限不足");
        }else if(imageIds == null || imageIds.isEmpty()){
            return Result.success("无文件删除");
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
                                        @RequestParam Integer pageSize) {
        Claims claims = JwtUtils.parseJWT(Authorization, signKey);
        if (claims == null) {
            return Result.error("token无效");
        }else if(!((Boolean) claims.get("userType"))){
            return Result.error("权限不足");
        }
        return Result.success(imagesService.searchImagesWithKeyword(keyword,pageNum,pageSize));
    }

    /**
     * 按图片id查询
     *
     * @param Authorization token
     * @param imageId       图片id
     * @return  Result      响应
     */
    @PostMapping("/searchImageById")
    public Result searchImagesById(@RequestHeader String Authorization, @RequestParam String imageId) {
        Claims claims = JwtUtils.parseJWT(Authorization, signKey);
        if (claims == null) {
            return Result.error("token无效");
        }else if(!((Boolean) claims.get("userType"))){
            return Result.error("权限不足");
        }else if(imageId == null || imageId.isEmpty()){
            return Result.error("Id为空");
        }
        return Result.success(imagesService.searchImagesById(imageId));
    }
}
