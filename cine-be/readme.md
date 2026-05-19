# CINEGO TICKETING SYSTEM

## 🚀 HƯỚNG DẪN CHI TIẾT CHẠY DỰ ÁN (CHO NGƯỜI MỚI)

---

## 📋 1. Giới thiệu

CineGo là hệ thống đặt vé xem phim trực tuyến với **xử lý thanh toán real-time** và **phát hiện gian lận** bằng Spark:

- **Backend**: Spring Boot (Java) + Kafka + HBase + Redis + Spark
- **Frontend User**: HTML / CSS / JavaScript thuần
- **Frontend Admin**: HTML / CSS / JavaScript, chạy trên Nginx (Docker)
- **Big Data**: Kafka + Spark + HBase cho fraud detection và analytics

---

## 🛠️ 2. Yêu cầu môi trường (BẮT BUỘC)

Trước khi chạy, máy tính CẦN cài đặt:

- **Java JDK 21** (KHÔNG phải 17)
- **Apache Maven 3.8+**
- **Docker Desktop** (Windows 10/11) hoặc Docker + Docker Compose (Linux/Mac)
- **IntelliJ IDEA** (recommended)
- **Git** để clone project
- **Chrome/Edge** trình duyệt

---

## 📁 3. Clone project

```bash
# Clone project từ GitHub
git clone <repository-url>

# Di chuyển vào thư mục project
cd cinego_ticket
cd cine-be
```

---

## 🏗️ 4. Cấu trúc project

```
cinego_ticket/
├── cine-be/                    # Backend Spring Boot + Kafka + Spark
│   ├── docker-compose.yml      # Infrastructure (Kafka, Redis, HBase, Spark)
│   ├── spark-jobs/             # Spark JAR file (mount vào container)
│   └── src/                    # Java source code
├── cine_fe/                    # Frontend User
├── admin/                      # Frontend Admin (Nginx Docker)
└── README.md                   # File hướng dẫn chính
```

---

## 🚀 5. HƯỚNG DẪN CHẠY CHI TIẾT (TỪNG BƯỚC)

### ⚠️  QUAN TRỌNG: Thứ tự chạy KHÔNG THỂ thay đổi

#### 📦 Bước 1: Khởi động Infrastructure (BẮT BUỘC ĐẦU TIÊN)

**Mở PowerShell trong thư mục `cine-be`:**

```powershell
cd cine-be
docker compose up -d
```

Chờ HBase sẵn sàng (~30 giây), sau đó tạo 3 tables thủ công:
```powershell
docker exec hbase bash -c "echo \"create 'payment_history', 'cf'\" | /opt/hbase/bin/hbase shell -n 2>/dev/null"
docker exec hbase bash -c "echo \"create 'fraud_logs', 'cf'\" | /opt/hbase/bin/hbase shell -n 2>/dev/null"
docker exec hbase bash -c "echo \"create 'analytics_daily', 'cf'\" | /opt/hbase/bin/hbase shell -n 2>/dev/null"
```

Pre-create Kafka topic `anomaly-events`:
```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --create --topic anomaly-events --partitions 3 --replication-factor 1 --if-not-exists
```

**Verify services:**
- **Spark UI**: http://localhost:8080
- **HBase UI**: http://localhost:16010
- **Docker containers**: `docker ps` (phải thấy 6 container: kafka, redis, zookeeper, hbase, spark-master, spark-worker)

---

#### 💾 Bước 2: Thiết lập Database MySQL

MySQL chạy ngoài Docker (không có trong docker-compose). Kết nối bằng MySQL Workbench/DBeaver:
- Host: `localhost`, Port: `3306`
- Username: `username`, Password: `password`
- Database: `linh_test_5000`

Spring Boot tự chạy `db/data.sql` để seed phim và user khi start lần đầu (nếu chưa có data).

---

#### ☕ Bước 3: Chạy Spring Boot Backend

**Trong IntelliJ IDEA:**

1. **Open project**: File → Open → chọn thư mục `cine-be`
2. **Wait Maven download dependencies** (lần đầu có thể mất 5-10 phút)
3. **Find main class**: `src/main/java/linh/vn/cinegoticket/CinegoTicketApplication.java`
4. **Right-click → Run 'CinegoTicketApplication()'**

**Backend chạy thành công khi thấy:**
```
Started CinegoTicketApplication in X.XXX seconds
```

**Endpoint:** http://localhost:9595

---

#### 🌐 Bước 4: Chạy Frontend User

**Mở VS Code:**

1. **Open folder**: `cine_fe`
2. **Install extension**: **Live Server** (Ritwick Dey)
3. **Right-click `index.html` → Open with Live Server**

**Frontend chạy tại:** http://127.0.0.1:5500

**Đăng nhập test:**
- Username: `user3`
- Password: `user3`

---

#### 👨‍💼 Bước 5: Chạy Frontend Admin

**Mở PowerShell trong thư mục `admin`:**

```powershell
cd admin
docker build -t cinego-admin .
docker run -d --name cinego-admin -p 80:80 cinego-admin
```

**Admin UI:** http://localhost (analytics.html, anomalies.html, ...)

**Đăng nhập admin:**
- Username: `admin`
- Password: `admin`

---

## 🔄 6. Luồng hoạt động Payment + Fraud Detection

Khi user thanh toán:

1. **Spring Boot** nhận VNPay callback → cập nhật Payment thành `PAID`
2. **Publish event** vào Kafka topic `payment-events` (3 partitions)
3. **Spring AnomalyConsumer** (real-time, <50ms) phát hiện fraud → ghi vào MySQL `anomaly_log` với `source=SPRING`
4. **Spark FraudDetectionJob** (deep analysis, 30s batch) → publish alert vào `anomaly-events` → HBase + Redis
5. **SparkFraudAlertConsumer** (Spring) consume topic `anomaly-events` → ghi vào MySQL với `source=SPARK`
6. **Spark AnalyticsJob** tổng hợp doanh thu, top movies → ghi vào Redis + HBase
7. **Admin dashboard** đọc Redis để hiển thị analytics realtime

**Monitor:**
- **Spark UI**: http://localhost:8080
- **HBase UI**: http://localhost:16010
- **Kafka topics**: `docker exec kafka /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server kafka:9092`

---

## ⚠️ 7. TROUBLESHOOTING (Thường gặp)

### 🔴 Docker không start được
```bash
# Kiểm tra Docker Desktop đang chạy
docker --version

# Kiểm tra Docker daemon
docker ps
```

### 🔴 Spring Boot không kết nối được Kafka
- **Nguyên nhân**: Chạy Spring Boot TRƯỚC khi start infrastructure
- **Fix**: Dừng Spring Boot, chạy `start-infrastructure.bat` trước

### 🔴 Maven download dependencies quá lâu
- **Lần đầu**: Có thể mất 10-15 phút, bình thường
- **Solution**: Đợi Maven download xong hoặc dùng Maven repository mirror

---

## 📋 7. QUẢN LÝ CONTAINER

### 🆕 **Lần đầu chạy — Chưa có container**
```powershell
cd cine-be
docker compose up -d
# Sau đó tạo HBase tables và anomaly-events topic (xem Bước 1)
```

### 🔄 **Restart lại — Đã có container từ trước**
```powershell
cd cine-be
docker compose up -d
# HBase tables và Redis data giữ nguyên trong ./hbase-data và ./redis-data
```

### 🛑 **Dừng tất cả containers**
```powershell
docker compose stop   # dừng, giữ data
docker compose down   # dừng + xóa container (data trong volumes vẫn còn)
```

### 🔴 Port 80 bị chiếm (Skype, IIS, XAMPP)
```powershell
# Tìm process chiếm port 80
netstat -ano | findstr :80
# Kill process
taskkill /PID <PID> /F
```

### 🔴 Spark jobs không chạy
```powershell
# Check Spark Master status
docker logs spark-master

# Submit lại Spark job (xem COMPLETE-TEST-GUIDE.md Bước 3-4)
docker exec spark-master /opt/spark/bin/spark-submit --deploy-mode client --master spark://spark-master:7077 --conf spark.cores.max=4 --class linh.vn.spark.job.FraudDetectionJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379
```

---

## 🛑 8. DỪNG DỰ ÁN

Khi làm xong, dừng tất cả services:

```powershell
# Trong thư mục cine-be
docker compose stop

# Dừng admin container
docker stop cinego-admin
docker rm cinego-admin
```

---

## 📝 9. TỔNG KẾT CÁC BƯỚC (QUICK REFERENCE)

Xem hướng dẫn đầy đủ:
- **Lần đầu:** `FIRST-TEST-GUIDE.md`
- **Restart lại:** `COMPLETE-TEST-GUIDE.md`

```powershell
# 1. Start infrastructure (từ thư mục cine-be)
docker compose up -d
# Tạo HBase tables + anomaly-events topic (xem FIRST-TEST-GUIDE.md Bước 1)

# 2. Start Spring Boot (từ root project)
./mvnw spring-boot:run -pl cine-be

# 3. Submit Spark jobs (terminal riêng)
docker exec spark-master /opt/spark/bin/spark-submit --deploy-mode client --master spark://spark-master:7077 --conf spark.cores.max=4 --class linh.vn.spark.job.FraudDetectionJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379
docker exec spark-master /opt/spark/bin/spark-submit --deploy-mode client --master spark://spark-master:7077 --conf spark.cores.max=4 --class linh.vn.spark.job.AnalyticsJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379

# 4. Start Frontend User (VS Code)
# - Open cine_fe/ → Right-click index.html → Open with Live Server

# 5. Start Frontend Admin
cd ../admin
docker build -t cinego-admin .
docker run -d --name cinego-admin -p 80:80 cinego-admin
```

---

## 🎯 10. TESTING PAYMENT FLOW

### 💳 Thẻ test VNPay
Khi thanh toán, dùng thông tin thẻ test:
- **Ngân hàng**: NCB
- **Số thẻ**: 9704198526191432198
- **Tên chủ thẻ**: NGUYEN VAN A
- **Ngày phát hành**: 07/15
- **Mật khẩu OTP**: 123456

### 🔄 Luồng test hoàn chỉnh
1. **Đăng nhập user**: http://127.0.0.1:5500 (user3/user3)
2. **Chọn phim + ghế** → Thanh toán VNPay
3. **Nhập thông tin thẻ test** → Xác nhận thanh toán
4. **Monitor real-time** (xem ngay sau khi thanh toán):

---

## 📊 11. MONITOR REAL-TIME (QUAN TRỌNG)

### 🔥 Spark UI: http://localhost:8080
**Cần thấy:**
- **Running Applications**: 2 jobs (FraudDetectionJob, AnalyticsJob)
- **Completed Jobs**: Tăng lên sau mỗi lần thanh toán
- **Job Status**: "RUNNING" hoặc "SUCCEEDED"

**Nếu không thấy jobs:**
```bash
# Restart Spark jobs
docker exec -it spark-master bash -c "spark-submit --master spark://spark-master:7077 --class linh.vn.spark.job.FraudDetectionJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379"
```

### 🗄️ HBase UI: http://localhost:16010
**Cần thấy:**
- **Tables**: `payment_history`, `fraud_logs`, `analytics_daily`
- **Table Rows**: Tăng lên sau mỗi lần thanh toán
- **Browse Table**: Click vào table name → "Browse" để xem dữ liệu

**Check tables:**
```bash
# Vào HBase shell
docker exec -it hbase bash -c "echo 'list' | hbase shell"
```

### 📈 Admin Dashboard: http://localhost
**Cần thấy:**
- **Payment Statistics**: Cập nhật real-time
- **Fraud Alerts**: Hiển thị nếu có giao dịch đáng ngờ
- **Revenue Analytics**: Dữ liệu doanh thu theo thời gian

### 📡 Kafka Topics Monitoring
```powershell
# List all topics (phải thấy: payment-events, anomaly-events)
docker exec kafka /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server kafka:9092

# Check messages trong payment-events
docker exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic payment-events --from-beginning --max-messages 5
```

---

## ✅ 12. KẾT QUẢ TEST ĐÚNG

### 🎯 **Thành công khi thấy:**
1. **Spark UI**: Jobs đang chạy và hoàn thành
2. **HBase UI**: Tables có dữ liệu mới
3. **Admin Dashboard**: Statistics cập nhật
4. **Kafka**: Messages được consume

### ❌ **Lỗi khi thấy:**
1. **Spark UI**: Không có jobs hoặc jobs failed
2. **HBase UI**: Tables rỗng sau thanh toán
3. **Admin Dashboard**: Không có dữ liệu mới
4. **Kafka**: Messages không được consume

---

## 👥 13. Tác giả

Sinh viên thực hiện: **Phạm Mỹ Linh**  
Đề tài: **Hệ thống đặt vé xem phim CineGo với Fraud Detection**  
Công nghệ: **Spring Boot + Kafka + Spark + HBase + Redis**

