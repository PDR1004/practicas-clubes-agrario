FROM node:22-alpine AS build

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration=production --base-href=/dacs/

FROM nginx:1.27-alpine

COPY --from=build /app/dist/dacs-fe/browser/ /opt/dacs/
