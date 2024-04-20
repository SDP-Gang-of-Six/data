package cn.wxl475;

import cn.wxl475.pojo.data.Video;
import cn.wxl475.repo.VideoEsRepo;
import cn.wxl475.service.VideosService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@Slf4j
@SpringBootTest(args = "--spring.profiles.active=dev")
@RunWith(SpringRunner.class)
public class VideosEsTest {
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private VideoEsRepo videoEsRepo;

    @Autowired
    private VideosService videosService;

    @Test
    public void creatVideosIndex(){
        Class<Video> aClass = Video.class;
        boolean created = elasticsearchRestTemplate.indexOps(aClass).createWithMapping();
        if(created){
            log.info("创建索引：{}",aClass.getName()+",成功");
        }else {
            log.info("创建索引：{}",aClass.getName()+",失败");
        }
    }

    @Test
    public void deleteVideosIndex(){
        Class<Video> aClass = Video.class;
        boolean deleted = elasticsearchRestTemplate.indexOps(aClass).delete();
        if(deleted){
            log.info("删除索引：{}",aClass.getName()+",成功");
        }else {
            log.info("删除索引：{}",aClass.getName()+",失败");
        }
    }

    @Test
    public void rebuildVideosIndex(){
        Class<Video> aClass = Video.class;
        boolean deleted = elasticsearchRestTemplate.indexOps(aClass).delete();
        boolean created = elasticsearchRestTemplate.indexOps(aClass).createWithMapping();
        List<Video> list = videosService.list();
        videoEsRepo.saveAll(list);
        if(deleted&&created){
            log.info("重建索引：{}",aClass.getName()+",成功");
        }else {
            log.info("重建索引：{}",aClass.getName()+",失败");
        }
    }

    @Test
    public void findAllVideos(){
        videoEsRepo.findAll().forEach(video -> { log.info(String.valueOf(video));});
    }

    @Test
    public void deleteAllVideos(){
        videoEsRepo.deleteAll();
    }
}
