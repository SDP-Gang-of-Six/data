package cn.wxl475.controller;

import cn.hutool.core.thread.ThreadUtil;
import cn.wxl475.minio.MinioUtils;
import cn.wxl475.pojo.Image;
import cn.wxl475.pojo.Result;
import cn.wxl475.pojo.userType;
import cn.wxl475.service.ImagesService;
import cn.wxl475.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Future;

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
        }else if(claims.get("userType")!=Integer.valueOf(userType.ADMIN.ordinal())){
            return Result.error("权限不足");
        }else if(images == null || images.isEmpty()){
            return Result.error("上传文件为空");
        }
        return Result.success(imagesService.uploadImages(images, (Long) claims.get("userId")));
    }
}
