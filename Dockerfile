# Use lightweight JDK runtime
FROM eclipse-temurin:21-jdk-alpine

COPY printhelm-web-service/build/libs/*jar app.jar
RUN apk add --no-cache curl jq
ENV JAVA_OPTIONS=${JAVA_OPTS}

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTIONS -jar /app.jar"]