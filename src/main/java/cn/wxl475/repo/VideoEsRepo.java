package cn.wxl475.repo;

import cn.wxl475.pojo.data.Video;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface VideoEsRepo extends ElasticsearchRepository<Video, Long> {
}
