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
     * @return 结果
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

    @PostMapping("/deleteImages")
    public Result deleteImages(@RequestHeader String Authorization, @RequestBody ArrayList<String> imageIds) {
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
}
