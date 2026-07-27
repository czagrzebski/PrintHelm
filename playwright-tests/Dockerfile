FROM mcr.microsoft.com/playwright:v1.59.1-noble
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN chmod +x run-tests.sh
CMD ["./run-tests.sh"]
