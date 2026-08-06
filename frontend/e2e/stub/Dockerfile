FROM node:24.12.0-alpine@sha256:c921b97d4b74f51744057454b306b418cf693865e73b8100559189605f6955b8
WORKDIR /stub
COPY server.mjs ./server.mjs
USER node
EXPOSE 8081
CMD ["node", "server.mjs"]
