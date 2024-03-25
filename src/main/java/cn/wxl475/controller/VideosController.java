package cn.wxl475.controller;

import cn.wxl475.pojo.Result;
import cn.wxl475.service.VideosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/data")
public class VideosController {
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
        return Result.success(videosService.uploadOneVideoSharding(videoSharding,videoMd5,shardingMd5,shardingInVideoIndex,shardingInVideoStart,shardingInVideoEnd,allShardingNums,videoSize));
    }
}
