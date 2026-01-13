FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
ARG VITE_API_URL=http://localhost:8080
ENV VITE_API_URL=${VITE_API_URL}
RUN npm run build

FROM node:18-alpine
WORKDIR /app
COPY --from=build /app/dist ./dist
RUN npm install serve
EXPOSE 4173
CMD ["npx", "serve", "-s", "dist", "-l", "4173"]
