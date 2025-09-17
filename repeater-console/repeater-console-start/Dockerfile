# 运行时使用 JRE 8（与 JDK 8 构建产物兼容）
FROM eclipse-temurin:8-jre-jammy

# 设定工作目录为 /app —— Spring Boot 会自动查找 ./config 下的外部配置
WORKDIR /app

# 非 root 运行
RUN useradd -r -u 10001 -g root appuser

# 复制可运行的 fat-jar（可通过 --build-arg 传入真实文件名）
ARG JAR_FILE=target/repeater-console-start.jar
COPY ${JAR_FILE} /app/app.jar

# 暴露仓库里的默认端口（可被外部配置或参数覆盖）
EXPOSE 8001

USER 10001

# 关键点：不在镜像里硬编码任何 Spring/JVM 参数
# K8s 会用 container.args 追加到这里
ENTRYPOINT ["java","-jar","/app/app.jar"]

# 可选：给出空 CMD 以方便追加参数（K8s 会覆盖/追加）
CMD []

