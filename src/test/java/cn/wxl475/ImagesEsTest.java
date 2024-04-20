package cn.wxl475;

import cn.wxl475.pojo.data.Image;
import cn.wxl475.repo.ImagesEsRepo;
import cn.wxl475.service.ImagesService;
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
public class ImagesEsTest {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private ImagesEsRepo imagesEsRepo;

    @Autowired
    private ImagesService imagesService;

    @Test
    public void creatImagesIndex(){
        Class<Image> aClass = Image.class;
        boolean created = elasticsearchRestTemplate.indexOps(aClass).createWithMapping();
        if(created){
            log.info("创建索引：{}",aClass.getName()+",成功");
        }else {
            log.info("创建索引：{}",aClass.getName()+",失败");
        }
    }

    @Test
    public void deleteImagesIndex(){
        Class<Image> aClass = Image.class;
        boolean deleted = elasticsearchRestTemplate.indexOps(aClass).delete();
        if(deleted){
            log.info("删除索引：{}",aClass.getName()+",成功");
        }else {
            log.info("删除索引：{}",aClass.getName()+",失败");
        }
    }

    @Test
    public void rebuildImagesIndex(){
        Class<Image> aClass = Image.class;
        boolean deleted = elasticsearchRestTemplate.indexOps(aClass).delete();
        boolean created = elasticsearchRestTemplate.indexOps(aClass).createWithMapping();
        List<Image> list = imagesService.list();
        imagesEsRepo.saveAll(list);
        if(deleted&&created){
            log.info("重建索引：{}",aClass.getName()+",成功");
        }else {
            log.info("重建索引：{}",aClass.getName()+",失败");
        }
    }

    @Test
    public void findAllImages(){
        imagesEsRepo.findAll().forEach(image -> { log.info(String.valueOf(image));});
    }

    @Test
    public void deleteAllImages(){
        imagesEsRepo.deleteAll();
    }
}
