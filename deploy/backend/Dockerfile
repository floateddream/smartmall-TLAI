FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY backend/pom.xml ./pom.xml
COPY backend/smartmall-common/pom.xml smartmall-common/pom.xml
COPY backend/smartmall-ai/pom.xml smartmall-ai/pom.xml
COPY backend/smartmall-knowledge/pom.xml smartmall-knowledge/pom.xml
COPY backend/smartmall-rag/pom.xml smartmall-rag/pom.xml
COPY backend/smartmall-business/pom.xml smartmall-business/pom.xml
COPY backend/smartmall-analytics/pom.xml smartmall-analytics/pom.xml
COPY backend/smartmall-agent/pom.xml smartmall-agent/pom.xml
COPY backend/smartmall-infrastructure/pom.xml smartmall-infrastructure/pom.xml
COPY backend/smartmall-security/pom.xml smartmall-security/pom.xml
COPY backend/smartmall-web/pom.xml smartmall-web/pom.xml
COPY backend/smartmall-job/pom.xml smartmall-job/pom.xml
COPY backend/smartmall-application/pom.xml smartmall-application/pom.xml
RUN mvn -B -ntp -DskipTests dependency:go-offline

COPY backend/ ./
RUN mvn -B -ntp -DskipTests package \
    && cp smartmall-application/target/smartmall-application-*.jar /workspace/application.jar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S smartmall && adduser -S -G smartmall -h /opt/smartmall smartmall
WORKDIR /opt/smartmall
COPY --from=build --chown=smartmall:smartmall /workspace/application.jar ./application.jar
USER smartmall
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8"
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
  CMD wget -q -O - http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar application.jar"]
