FROM node:16.13.2-alpine as build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm install

COPY . ./

RUN npm run build

FROM nginx:latest


COPY --from=build /app/build /var/www

COPY htreact.conf /etc/nginx/conf.d/htreact.conf

EXPOSE 80

ENTRYPOINT ["nginx","-g","daemon off;"]