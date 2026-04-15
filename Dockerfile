FROM fedora:41
WORKDIR /app
COPY target/oda-vk-service /app

CMD ["./oda-vk-service"]
