package cn.wxl475.controller;

import cn.wxl475.exception.FileIOException;
import cn.wxl475.pojo.Result;
import cn.wxl475.pojo.data.Video;
import cn.wxl475.redis.CacheClient;
import cn.wxl475.service.VideosService;
import cn.wxl475.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/data")
public class VideosController {

    @Value("${jwt.signKey}")
    private String signKey;

    @Autowired
    private VideosService videosService;
    @PostMapping("/uploadOneVideoSharding")
    public Result uploadOneVideoSharding(@RequestHeader String Authorization,
                                         @RequestBody MultipartFile videoSharding,
                                         @RequestParam String videoMd5,
                                         @RequestParam String shardingMd5,
                                         @RequestParam String shardingInVideoIndex,
                                         @RequestParam String shardingInVideoStart,
                                         @RequestParam String shardingInVideoEnd,
                                         @RequestParam String allShardingNums,
                                         @RequestParam String videoSize){
        try {
            return Result.success(videosService.uploadOneVideoSharding(videoSharding,videoMd5,shardingMd5,shardingInVideoIndex,shardingInVideoStart,shardingInVideoEnd,allShardingNums,videoSize));
        } catch (IOException e) {
            throw new FileIOException(e.getMessage());
        }
    }
    @PostMapping("/checkOneVideoSharding")
    public Result checkOneVideoSharding(@RequestHeader String Authorization,
                                   @RequestParam String videoMd5,
                                   @RequestParam String shardingInVideoIndex,
                                   @RequestParam String shardingMd5){
        return Result.success(videosService.checkOneVideoSharding(videoMd5,shardingInVideoIndex,shardingMd5));
    }

    @PostMapping("/mergeVideoShardings")
    public Result mergeVideoShardings(@RequestHeader String Authorization,
                                      @RequestParam String videoMd5,
                                      @RequestParam String videoOriginalName){
        Claims claims = JwtUtils.parseJWT(Authorization,signKey);
        Video video = null;
        try {
            video = videosService.mergeVideoShardings(
                    videoMd5,
                    videoOriginalName,
                    Long.valueOf(claims.get("uid").toString()));
        } catch (IOException e) {
            throw new FileIOException(e.getMessage());
        }
        if(video == null){
            return Result.error("视频分片尚未全部上传完毕");
        }
        return Result.success(video);
    }
}
