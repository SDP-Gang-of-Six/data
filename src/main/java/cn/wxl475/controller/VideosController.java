package cn.wxl475.controller;

import cn.wxl475.exception.FileIOException;
import cn.wxl475.pojo.Result;
import cn.wxl475.pojo.data.Video;
import cn.wxl475.service.VideosService;
import cn.wxl475.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;

@RestController
@RequestMapping("/data")
public class VideosController {

    @Value("${jwt.signKey}")
    private String signKey;

    private final VideosService videosService;

    @Autowired
    public VideosController(VideosService videosService) {
        this.videosService = videosService;
    }

    /**
     * 视频-上传视频分片
     * @param Authorization
     * @param videoSharding
     * @param videoMd5
     * @param shardingMd5
     * @param shardingInVideoIndex
     * @param allShardingNums
     * @return
     */
    @PostMapping("/uploadOneVideoSharding")
    public Result uploadOneVideoSharding(@RequestHeader String Authorization,
                                         @RequestBody MultipartFile videoSharding,
                                         @RequestParam String videoMd5,
                                         @RequestParam String shardingMd5,
                                         @RequestParam String shardingInVideoIndex,
                                         @RequestParam String allShardingNums){
        try {
            return Result.success(videosService.uploadOneVideoSharding(videoSharding,videoMd5,shardingMd5,shardingInVideoIndex,allShardingNums));
        } catch (IOException e) {
            throw new FileIOException(e.getMessage());
        }
    }
    /**
     * 视频-检查视频分片是否上传过
     * @param Authorization
     * @param videoMd5
     * @param shardingInVideoIndex
     * @param shardingMd5
     * @return
     */
    @PostMapping("/checkOneVideoSharding")
    public Result checkOneVideoSharding(@RequestHeader String Authorization,
                                   @RequestParam String videoMd5,
                                   @RequestParam String shardingInVideoIndex,
                                   @RequestParam String shardingMd5){
        return Result.success(videosService.checkOneVideoSharding(videoMd5,shardingInVideoIndex,shardingMd5));
    }
    /**
     * 视频-合并视频分片
     * @param Authorization
     * @param videoMd5
     * @param videoOriginalName
     * @return
     */
    @PostMapping("/mergeVideoSharding")
    public Result mergeVideoSharding(@RequestHeader String Authorization,
                                     @RequestParam String videoMd5,
                                     @RequestParam String videoOriginalName){
        Claims claims = JwtUtils.parseJWT(Authorization,signKey);
        Video video;
        try {
            video = videosService.mergeVideoSharding(
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

    /**
     * 视频-删除视频分片
     * @param Authorization
     * @param videoMd5
     * @return
     */
    @PostMapping("/deleteVideoSharding")
    public Result deleteVideoSharding(@RequestHeader String Authorization,
                                      @RequestParam String videoMd5){
        return Result.success(videosService.deleteVideoSharding(videoMd5));
    }

    /**
     * 视频-批量/单个-删除视频
     * @param Authorization
     * @param videoIds
     * @return
     */
    @PostMapping("/deleteVideos")
    public Result deleteVideos(@RequestHeader String Authorization,
                               @RequestParam ArrayList<Long> videoIds){
        return Result.success(videosService.deleteVideos(videoIds));
    }

    /**
     * 视频-关键词分页查询视频
     * @param Authorization
     * @param keyword
     * @param pageNum
     * @param pageSize
     * @param sortField
     * @param sortOrder
     * @return
     */
    @PostMapping("/searchVideosByKeyword")
    public Result searchVideosByKeyword(@RequestHeader String Authorization,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam Integer pageNum,
                                        @RequestParam Integer pageSize,
                                        @RequestParam(required = false) String sortField,
                                        @RequestParam(required = false) Integer sortOrder){
        if(pageNum<=0||pageSize<=0){
            return Result.error("页码或页大小不合法");
        }
        return Result.success(videosService.searchVideosByKeyword(keyword,pageNum,pageSize,sortField,sortOrder));
    }

    /**
     * 视频-批量/单个-按视频id查询
     * @param Authorization
     * @param videoIds
     * @return
     */
    @PostMapping("/searchVideosByIds")
    public Result searchVideosByIds(@RequestHeader String Authorization,@RequestBody ArrayList<Long>  videoIds) {
        return Result.success(videosService.searchVideosByIds(videoIds));
    }
}
