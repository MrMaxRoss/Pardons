FROM node:20-slim

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm install --production

COPY tsconfig.json ./
COPY src/ ./src/

RUN npx tsc

EXPOSE 8080
CMD ["node", "dist/index.js"]
