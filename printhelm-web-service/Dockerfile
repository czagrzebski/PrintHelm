# Use lightweight JDK runtime
FROM eclipse-temurin:21-jdk-alpine

COPY printhelm-web-service/build/libs/*jar app.jar
RUN apk add --no-cache curl jq ffmpeg
ENV JAVA_OPTIONS=${JAVA_OPTS}

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTIONS --add-opens java.base/sun.security.ssl=ALL-UNNAMED -jar /app.jar"]