# Frontend Dockerfile (frontend/Dockerfile)
FROM eclipse-temurin:21-jdk-alpine AS java-base

FROM node:22-alpine AS build
WORKDIR /app

COPY --from=java-base /opt/java/openjdk /opt/java/openjdk
ENV PATH="/opt/java/openjdk/bin:${PATH}"

COPY printhelm-vue/ ./

RUN npm install

# list files in the current directory
RUN ls -la

COPY printhelm-web-contract/src/main/resources/printhelm-web-openapi.yaml ./

RUN npm run generateApiDocker
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY printhelm-vue/nginx.conf /etc/nginx/templates/default.conf.template
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]