# Stage 1: Build ứng dụng bằng Maven và JDK 21
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
# Sao chép pom.xml và tải các dependency trước để tận dụng cache của Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B
# Sao chép mã nguồn và đóng gói file JAR (bỏ qua chạy test khi build)
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Chạy ứng dụng bằng JRE 21 siêu nhẹ
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Cấu hình cổng chạy mặc định
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
