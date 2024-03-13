package cn.wxl475.minio;

import io.minio.*;
import io.minio.http.Method;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class MinioUtils {

    @Autowired
    private MinioClient client;
    @Autowired
    private MinioProp minioProp;

    /**
     * 创建存储桶
     *
     * @param bucketName 存储桶名称
     */
    @SneakyThrows
    public void createBucket(String bucketName) {
        BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(bucketName).build();
        if (!client.bucketExists(bucketExistsArgs)) {
            MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder().bucket(bucketName).build();
            client.makeBucket(makeBucketArgs);
        }
    }

    /**
     * 上传文件
     *
     * @param file       文件
     * @param path       文件路径
     * @param bucketName 存储桶
     */
    public String uploadFile(MultipartFile file, String path, String bucketName) {
        // 判断上传文件是否为空
        if (file == null || file.isEmpty()) {
            return "上传文件为空";
        }

        String originalFilename = file.getOriginalFilename();
        assert originalFilename != null;
        // 新的文件名 = 时间戳+文件名
        String fileName = System.currentTimeMillis() + "_" + originalFilename;
        path = path + fileName;

        // 开始上传
        ObjectWriteResponse objectWriteResponse;
        try {
            objectWriteResponse = client.putObject(PutObjectArgs.builder().
                    contentType(file.getContentType()).
                    bucket(bucketName).
                    stream(file.getInputStream(), file.getSize(), -1).
                    object(path).
                    build());
        }catch (Exception e){
            log.error("上传文件失败",e);
            return "上传文件失败";
        }
        log.info("文件上传成功" + minioProp.getEndpoint()+"/"+objectWriteResponse.bucket()+"/"+objectWriteResponse.object());
        return minioProp.getEndpoint()+"/"+objectWriteResponse.bucket()+"/"+objectWriteResponse.object();
    }

}
