# =========================
# Stage 1: Build ứng dụng
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Thiết lập múi giờ cho container
ENV TZ=Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Copy chỉ file cần thiết để cache dependencies
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Cấp quyền thực thi cho mvnw
RUN chmod +x mvnw

# Chạy lệnh cache dependencies
RUN ./mvnw verify --fail-never -DskipTests

# Copy source code
COPY src src

# Build ứng dụng thành file JAR
RUN ./mvnw clean package -DskipTests

# =========================
# Stage 2: Run ứng dụng
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV DATABASE_URL=jdbc:mysql://linh_db:3306/laptop2
ENV DATABASE_USERNAME=username
ENV DATABASE_PASSWORD=password
ENV PORT=9595

ENV REDIS_HOST=redis-container

# Copy file JAR từ stage build vào container
COPY --from=builder /app/target/*.jar cine-app.jar

# Cấp quyền thực thi cho file JAR (có thể không cần)
RUN chmod +x cine-app.jar

# Định nghĩa JAVA_OPTS để tối ưu hiệu suất JVM
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication"

# Mở cổng ứng dụng (phải trùng với server.port trong application.properties)
EXPOSE 9595

# Chạy ứng dụng
#ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar cine-app.jar"]
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar cine-app.jar --server.port=${PORT}"]

