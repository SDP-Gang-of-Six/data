package cn.wxl475.controller;

import cn.wxl475.minio.MinioUtils;
import cn.wxl475.pojo.Images;
import cn.wxl475.pojo.Result;
import cn.wxl475.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Objects;

@RestController
@RequestMapping("/data")
public class DataController {

    @Value("${jwt.signKey}")
    private String signKey;

    @Autowired
    private MinioUtils minioUtils;

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
        }else if(!"admin".equals(claims.get("userType"))){
            return Result.error("权限不足");
        }else if(images == null || images.isEmpty()){
            return Result.error("上传文件为空");
        }
        // 上传文件
        ArrayList<Images> imagesArrayList = new ArrayList<>();
        for (MultipartFile image : images) {
            String url = minioUtils.uploadFile(image, "images/", "pet-hospital");
            imagesArrayList.add(new Images(
                    null,
                    claims.get("userId").toString(),
                    url,
                    image.getOriginalFilename(),
                    Objects.requireNonNull(image.getOriginalFilename()).substring(image.getOriginalFilename().lastIndexOf(".") + 1),
                    String.valueOf(image.getSize()),
                    null,
                    null,
                    0,
                    null));
            if(!("上传文件为空".equals(url) || "上传文件失败".equals(url) || "获取文件URL失败".equals(url))){

            }
        }
        return Result.success();
    }
}
