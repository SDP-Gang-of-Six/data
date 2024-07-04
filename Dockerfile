# 指定基础镜像
FROM amazoncorretto:21.0.3

# 拷贝jdk和java项目的包
COPY ./target/data-1.0-SNAPSHOT.jar /data/data.jar

# 挂载目录
VOLUME /data/pet-hospital

# 暴露端口
EXPOSE 8080
# 入口，java项目的启动命令
ENTRYPOINT java -jar -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=128m -Xms1024m -Xmx1024m -Xmn256m -Xss256k -XX:SurvivorRatio=8 /data/data.jar --spring.profiles.active=pro
