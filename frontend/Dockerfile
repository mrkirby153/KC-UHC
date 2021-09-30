FROM node:current-alpine as builder

WORKDIR /build

COPY package.json /build/package.json
COPY yarn.lock /build/yarn.lock

RUN yarn install

COPY . /build

RUN yarn build

FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

COPY --from=builder /build/dist /app
