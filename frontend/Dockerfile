FROM node:24.12.0-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.29-alpine3.23
USER root
RUN apk upgrade --no-cache \
    c-ares \
    curl \
    libcurl \
    libcrypto3 \
    libssl3 \
    libexpat \
    libxml2
USER 101
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html
EXPOSE 8080
