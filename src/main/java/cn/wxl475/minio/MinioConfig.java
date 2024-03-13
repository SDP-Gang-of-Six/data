package cn.wxl475.minio;


import io.minio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * minio 核心配置类
 */
@Configuration
@EnableConfigurationProperties(MinioProp.class)
public class MinioConfig {

    @Autowired
    private MinioProp minioProp;

    /**
     * 获取 MinioClient
     *
     * @return
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder().endpoint(minioProp.getEndpoint()).credentials(minioProp.getAccesskey(), minioProp.getSecretKey()).build();
    }
}
