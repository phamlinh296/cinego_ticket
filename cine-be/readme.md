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
├── cine_be/                    # Backend Spring Boot + Kafka + Spark
│   ├── docker-compose.yml      # All services (MySQL, Kafka, Redis, HBase, Spark)
│   ├── start-infrastructure.bat # Windows auto-start script
│   ├── stop-infrastructure.bat   # Windows stop script
│   ├── spark-jobs/             # Spark JAR file
│   └── src/                    # Java source code
├── cine_fe/                    # Frontend User
├── admin/                      # Frontend Admin (Nginx Docker)
└── README.md                   # File hướng dẫn này
```

---

## 🚀 5. HƯỚNG DẪN CHẠY CHI TIẾT (TỪNG BƯỚC)

### ⚠️  QUAN TRỌNG: Thứ tự chạy KHÔNG THỂ thay đổi

#### 📦 Bước 1: Khởi động Infrastructure (BẮT BUỘC ĐẦU TIÊN)

**Mở Command Prompt/PowerShell trong thư mục `cine-be`:**

```bash
# Navigate đến thư mục cine-be
cd D:\NewVolume\Spring\1.hoidanit\cinego_ticket\cine-be

# Chạy script khởi động (Windows)
.\start-infrastructure.bat
```

**⚠️ QUAN TRỌNG:**
- **Command Prompt**: `start-infrastructure.bat`
- **PowerShell**: `.\start-infrastructure.bat` (phải có `.\`)
- **Nếu lỗi persists**: Chuột phải file → "Run as Administrator"

**Script này sẽ tự động làm tất cả:**
- ✅ Start Docker containers: MySQL, Redis, Kafka, HBase, Spark Master/Worker
- ✅ Tạo Kafka topics: `payment-events`, `fraud-alerts`, `analytics-results`
- ✅ Khởi động Spark jobs: FraudDetectionJob, AnalyticsJob
- ✅ Tạo HBase tables: `payment_history`, `fraud_logs`, `analytics_daily`

**Đợi script chạy xong (khoảng 2-3 phút)**

**Verify services:**
- **Spark UI**: http://localhost:8080 (phải thấy Spark applications đang chạy)
- **HBase UI**: http://localhost:16010 (phải thấy tables đã tạo)
- **Docker containers**: `docker ps` (phải thấy 7+ containers đang chạy)

---

#### 💾 Bước 2: Thiết lập Database MySQL

1. **Mở MySQL Workbench/DBeaver/DataGrip**
2. **Kết nối tới MySQL:**
   - Host: `localhost`
   - Port: `3306`
   - Username: `username`
   - Password: `password`
   - Database: `linh_test_5000`

3. **Tạo database và insert data:**
   ```bash
   # Mở file data.sql và thực thi các câu lệnh
   cine_be/src/main/resources/db/data.sql
   ```

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

**Mở Command Prompt trong thư mục `admin`:**

```bash
# Di chuyển đến thư mục admin
cd D:\NewVolume\Spring\1.hoidanit\cinego_ticket\admin

# Build Docker image
docker build -t admin-nginx .

# Run container
docker run -d --name admin-nginx -p 80:80 -v ./ad-src:/var/www/html admin-nginx
```

**Admin UI:** http://localhost

**Đăng nhập admin:**
- Username: `admin`
- Password: `admin`

---

## 🔄 6. Luồng hoạt động Payment + Fraud Detection

Khi user thanh toán:

1. **Spring Boot** nhận VNPay callback
2. **Publish event** vào Kafka topic `payment-events`
3. **Spark FraudDetectionJob** consume event → Phát hiện gian lận
4. **Spark AnalyticsJob** consume event → Phân tích dữ liệu
5. **Results** được lưu vào HBase và Redis
6. **Admin dashboard** hiển thị analytics real-time

**Monitor:**
- **Spark Jobs**: http://localhost:8080
- **HBase Data**: http://localhost:16010
- **Kafka Topics**: `docker exec -it kafka bash -c "/opt/kafka/bin/kafka-topics.sh --list --bootstrap-server kafka:9092"`

---

## ⚠️ 7. TROUBLESHOOTING (Thường gặp)

### 🔴 Docker không start được
```bash
# Kiểm tra Docker Desktop đang chạy
docker --version

# Kiểm tra Docker daemon
docker ps
```

### 🔴 Script start-infrastructure.bat lỗi
- **Nguyên nhân**: Docker Desktop chưa start, port bị chiếm, hoặc PowerShell security policy
- **Fix**: 
  1. Mở Docker Desktop, đợi 30 giây
  2. Dùng `.\start-infrastructure.bat` cho PowerShell
  3. Chuột phải file → "Run as Administrator"

### 🔴 Spring Boot không kết nối được Kafka
- **Nguyên nhân**: Chạy Spring Boot TRƯỚC khi start infrastructure
- **Fix**: Dừng Spring Boot, chạy `start-infrastructure.bat` trước

### 🔴 Maven download dependencies quá lâu
- **Lần đầu**: Có thể mất 10-15 phút, bình thường
- **Solution**: Đợi Maven download xong hoặc dùng Maven repository mirror

---

## 📋 7. QUẢN LÝ CONTAINER (QUAN TRỌNG)

### 🆕 **Lần đầu chạy - Chưa có container nào**
```bash
# Script sẽ tự động tạo tất cả containers
.\start-infrastructure.bat
```
**Kết quả:** Tạo mới 7 containers (MySQL, Redis, Kafka, Zookeeper, HBase, Spark Master, Spark Worker)

### 🔄 **Đã có container rồi - Chỉ start lên**
```bash
# Script sẽ tự động nhận biết và chỉ start containers có sẵn
.\start-infrastructure.bat
```
**Kết quả:** Start lại 7 containers đã có, không tạo lại

### 🛑 **Dừng tất cả containers**
```bash
# Dừng containers nhưng giữ lại để dùng lần sau
docker-compose stop

# Hoặc dừng và xóa hoàn toàn
docker-compose down -v
```

### 🔧 **Xử lý lỗi container conflict**
Nếu bị lỗi "container name already in use":
```bash
# Cách 1: Dừng và xóa containers
docker-compose down -v

# Cách 2: Xóa container cụ thể
docker rm -f linh_db redis kafka zookeeper hbase spark-master spark-worker

# Sau đó chạy lại:
.\start-infrastructure.bat
```

### 🔴 Port 80 bị chiếm (Skype, IIS, XAMPP)
```bash
# Tìm process chiếm port 80
netstat -ano | findstr :80

# Kill process
taskkill /PID <PID> /F
```

### 🔴 Spark jobs không chạy
```bash
# Check Spark Master status
docker logs spark-master

# Restart Spark jobs
docker exec -it spark-master bash -c "spark-submit --master spark://spark-master:7077 --class linh.vn.spark.job.FraudDetectionJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379"
```

---

## 🛑 8. DỪNG DỰ ÁN

Khi làm xong, dừng tất cả services:

```bash
# Trong thư mục cine-be
stop-infrastructure.bat

# Dừng admin container
docker stop admin-nginx
docker rm admin-nginx
```

---

## 📝 9. TỔNG KẾT CÁC BƯỚC (QUICK REFERENCE)

```bash
# 1. Clone project
git clone <repository-url>
cd cinego_ticket/cine-be

# 2. Start infrastructure (QUAN TRỌNG NHẤT)
.\start-infrastructure.bat  # PowerShell
# hoặc
start-infrastructure.bat     # Command Prompt

# 3. Setup MySQL database
# - Connect: localhost:3306, username/password
# - Run: src/main/resources/db/data.sql

# 4. Start Spring Boot (IntelliJ IDEA)
# - Open project cine-be
# - Run: CinegoTicketApplication.java

# 5. Start Frontend User (VS Code)
# - Open cine_fe folder
# - Right-click index.html → Open with Live Server

# 6. Start Frontend Admin
cd ../admin
docker build -t admin-nginx .
docker run -d --name admin-nginx -p 80:80 -v ./ad-src:/var/www/html admin-nginx
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
```bash
# Check Kafka messages
docker exec -it kafka bash -c "/opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic payment-events --from-beginning"

# List all topics
docker exec -it kafka bash -c "/opt/kafka/bin/kafka-topics.sh --list --bootstrap-server kafka:9092"
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

