package cn.wxl475.repo;

import cn.wxl475.pojo.Image;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ImagesEsRepo extends ElasticsearchRepository<Image, Long> {
}
