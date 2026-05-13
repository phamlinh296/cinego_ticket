# 🚀 HƯỚNG DẪN TEST HOÀN CHỈNH - LUỒNG ĐẦY ĐỦ

**🎯 Mục tiêu:** Test complete flow từ API → Kafka → Spark → HBase/Redis với output cụ thể

---

## 📋 YÊU CẦU TRƯỚC KHI TEST

**Kiểm tra containers đang chạy:**
```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```
**Expected output:**
```
NAMES                STATUS
zookeeper            Up
kafka                Up  
hbase                Up
redis                Up
spark-master         Up
spark-worker         Up
```

---

## 🔧 STEP 1 - CHUẨN BỊ JAR FILE

### 🔥 Cách 1: Copy trực tiếp vào container (Development - Nhanh)
```bash
cd spark-processor
docker cp target/spark-processor-0.0.1-SNAPSHOT-shaded.jar spark-master:/opt/spark-jobs/

# Verify file đã được copy
docker exec spark-master ls -la /opt/spark-jobs/
```
**Expected output:**
```
spark-processor-0.0.1-SNAPSHOT-shaded.jar
```

### 🏭 Cách 2: Copy vào thư mục local rồi rebuild containers (Production - Ổn định)
```bash
cd spark-processor
# Copy JAR vào thư mục spark-jobs của cine-be
cp target/spark-processor-0.0.1-SNAPSHOT-shaded.jar ../cine-be/spark-jobs/

# Rebuild containers để cả master và worker đều có JAR
cd ../cine-be
docker-compose down spark-master spark-worker
docker-compose up -d spark-master spark-worker
```

**🎯 Khuyến nghị:**
- **Development**: Dùng Cách 1 cho nhanh
- **Production**: Dùng Cách 2 cho stability

---

## 🔧 STEP 2 - FIX HBASE ZOOKEEPER (QUAN TRỌNG)

**Tại sao phải làm thủ công?**
- HBase cần znode `/hbase/hbaseid` để identify cluster
- Khi HBase start lần đầu, nó tự tạo znode này
- Nhưng nếu ZooKeeper restart sau HBase, znode có thể bị mất
- Spark job đọc HBase qua ZooKeeper → cần znode này để kết nối

**Tạo missing znode trong ZooKeeper:**
```bash
docker exec zookeeper ./bin/zkCli.sh create /hbase ""
docker exec zookeeper ./bin/zkCli.sh create /hbase/hbaseid "hbase-cluster"
```
**Expected output:**
```
Created /hbase
Created /hbase/hbaseid
```

---

## 🚀 STEP 3 - KHỞI ĐỘNG SPRING BOOT

**Mở IntelliJ IDEA và run:**
- `CinegoTicketApplication.java`
- Đợi log: `Started CinegoTicketApplication on port 9595`

**Verify application đang chạy:**
```bash
curl http://localhost:9595/api/payment/test-kafka
```
**Expected output:**
```
Test Kafka event published: test-xxxxxxxx
```

---

## 🔥 STEP 4 - GỬI EVENT TEST TRƯỚC

**Gửi vài events để có data trong Kafka:**
```powershell
# PowerShell - gửi 3 events
for ($i=1; $i -le 3; $i++) {
    curl http://localhost:9595/api/payment/test-kafka
    Start-Sleep 1
}
```

**Expected output trong Spring Boot log:**
```
Publishing event test-1778512345678
✅ Successfully published payment event test-1778512345678 to partition X offset Y
```

---

## ⚡ STEP 5 - CHẠY CẢ HAI SPARK JOBS

### 5.1 Fraud Detection Job
**Mở PowerShell terminal 1 và chạy:**
```powershell
docker exec spark-master /opt/spark/bin/spark-submit --deploy-mode client --master spark://spark-master:7077 --class linh.vn.spark.job.FraudDetectionJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379
```

**Expected logs:**
```
Starting FraudDetectionJob | kafka=kafka:9092 hbase=zookeeper:2181 redis=localhost:6379
INFO FraudDetectionJob: Processing batch #1
INFO FraudDetectionJob: User=1 events=3 alerts=0
```

### 5.2 Analytics Job
**Mở PowerShell terminal 2 và chạy:**
```powershell
docker exec spark-master /opt/spark/bin/spark-submit --deploy-mode client --master spark://spark-master:7077 --class linh.vn.spark.job.AnalyticsJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379
```

**Expected logs:**
```
Starting AnalyticsJob | kafka=kafka:9092 hbase=zookeeper:2181 redis=localhost:6379
INFO AnalyticsJob: Revenue updated: 150000.0
INFO AnalyticsJob: Top movies updated
INFO AnalyticsJob: User stats updated
```

### 5.3 Tại sao phải chạy cả hai?
- **FraudDetectionJob:** Phát hiện gian lận, lưu vào `fraud_logs`
- **AnalyticsJob:** Tính toán analytics, lưu vào Redis (`user_stats`, `top_movies`, `revenue`)
- **Cùng consumer group khác:** Cả hai cùng đọc `payment-events` nhưng không ảnh hưởng nhau
- **Kafka fan-out:** Một topic, nhiều consumers độc lập

### 5.4 Nơi chạy commands
**🔥 Chạy trong PowerShell terminal:**
- Mở 2 cửa sổ PowerShell riêng biệt
- Mỗi terminal chạy 1 job
- Hoặc dùng VS Code terminal tabs

**🔹 Terminal 1 (Fraud Detection):**
```powershell
# Copy và paste vào PowerShell
docker exec spark-master /opt/spark/bin/spark-submit --deploy-mode client --master spark://spark-master:7077 --class linh.vn.spark.job.FraudDetectionJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379
```

**🔹 Terminal 2 (Analytics):**
```powershell
# Copy và paste vào PowerShell  
docker exec spark-master /opt/spark/bin/spark-submit --deploy-mode client --master spark://spark-master:7077 --class linh.vn.spark.job.AnalyticsJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379
```

---

## 📊 STEP 6 - KIỂM TRA KẾT QUẢ REDIS

**🔥 Redis nhận data từ CẢ HAI JOBS:**

### 6.1 Data từ AnalyticsJob (luôn có)
```bash
docker exec redis redis-cli KEYS "*"
```
**Expected output (từ AnalyticsJob):**
```
spark:user_stats:1           # User statistics
spark:top_movies:2026-05-11  # Top movies ranking
spark:revenue:2026-05-11:15 # Revenue theo giờ
```

**Check chi tiết user statistics (từ AnalyticsJob):**
```bash
docker exec redis redis-cli HGETALL "spark:user_stats:1"
```
**Expected output:**
```
tx_count        # Số transactions
3
total_amount    # Tổng số tiền
450000
avg_amount      # Trung bình transaction
150000.0
```

**Check top movies (từ AnalyticsJob):**
```bash
docker exec redis redis-cli ZREVRANGE "spark:top_movies:2026-05-11" 0 -1 WITHSCORES
```
**Expected output:**
```
123           # Movie ID
450000        # Total revenue
```

### 6.2 Data từ FraudDetectionJob (chỉ khi có fraud)
```bash
docker exec redis redis-cli KEYS "*fraud*"
```
**Expected output (chỉ khi có fraud):**
```
fraud_count:2026-05-11    # Counter fraud alerts
```

**Check fraud counter (từ FraudDetectionJob):**
```bash
docker exec redis redis-cli GET "fraud_count:2026-05-11"
```
**Expected output:**
```
0    # Không có fraud, hoặc số lượng fraud detected
```

---

## 🗄️ STEP 7 - KIỂM TRA KẾT QUẢ HBASE (QUAN TRỌNG)

**🔥 HBase nhận data từ CẢ HAI JOBS:**

### 7.1 Check tất cả tables
```bash
docker exec hbase /bin/bash -c "echo 'list' | /opt/hbase/bin/hbase shell -n"
```
**Expected output:**
```
TABLE
analytics_daily    # Từ AnalyticsJob
fraud_logs        # Từ FraudDetectionJob  
payment_history   # Từ AnalyticsJob
3 row(s)
```

### 7.2 Records từ AnalyticsJob (luôn có)

**Check payment_history (từ AnalyticsJob):**
```bash
docker exec hbase /bin/bash -c "echo 'count \"payment_history\"' | /opt/hbase/bin/hbase shell -n"
```
**Expected output:**
```
3 row(s)    # Số payment records đã lưu
```

**Check analytics_daily (từ AnalyticsJob):**
```bash
docker exec hbase /bin/bash -c "echo 'count \"analytics_daily\"' | /opt/hbase/bin/hbase shell -n"
```
**Expected output:**
```
1 row(s)    # Số analytics records đã lưu
```

### 7.3 Records từ FraudDetectionJob (chỉ khi có fraud)

**Check fraud_logs (từ FraudDetectionJob):**
```bash
docker exec hbase /bin/bash -c "echo 'count \"fraud_logs\"' | /opt/hbase/bin/hbase shell -n"
```
**Expected output:**
```
0 row(s)    # Không có fraud detected, hoặc số fraud alerts
```

**Nếu có fraud, expected output:**
```
3 row(s)    # Số fraud alerts đã detected
```

---

## 🌐 STEP 8 - CHECK SPARK UI

**🔥 Kiểm tra CẢ HAI JOBS đang chạy:**

### 8.1 Spark UI (http://localhost:8080)
**Mở browser:** http://localhost:8080

**Expected cho FraudDetectionJob:**
- Completed Applications: 1+
- Streaming Queries: 1 active (`fraud-detection-query`)
- Jobs Succeeded: 1+

**Expected cho AnalyticsJob:**  
- Completed Applications: 1+
- Streaming Queries: 3 active (`revenue-query`, `top-movies-query`, `user-stats-query`)
- Jobs Succeeded: 1+

**Tổng expected:**
- **2 Completed Applications** (mỗi job 1)
- **4 Active Streaming Queries** (1 fraud + 3 analytics)

### 8.2 HBase UI (http://localhost:16010)
**Mở browser:** http://localhost:16010

**Expected Tables:**
- `payment_history` - Records từ AnalyticsJob
- `analytics_daily` - Records từ AnalyticsJob  
- `fraud_logs` - Records từ FraudDetectionJob

**Expected Regions:**
- 3 active regions (mỗi table 1 region)

---

## ✅ SUCCESS CRITERIA

**✅ Luồng thành công khi:**

### 🚀 Infrastructure
1. [ ] Spring Boot running port 9595
2. [ ] API test trả về success
3. [ ] FraudDetectionJob start không lỗi
4. [ ] AnalyticsJob start không lỗi

### 📊 Redis Results (từ CẢ HAI JOBS)
5. [ ] **Từ AnalyticsJob:** Có keys `spark:user_stats:*`, `spark:top_movies:*`, `spark:revenue:*`
6. [ ] **Từ FraudDetectionJob:** Có key `fraud_count:*` (chỉ khi có fraud)

### 🗄️ HBase Results (từ CẢ HAI JOBS)  
7. [ ] HBase có 3 tables: `analytics_daily`, `fraud_logs`, `payment_history`
8. [ ] **Từ AnalyticsJob:** `payment_history` có records, `analytics_daily` có records
9. [ ] **Từ FraudDetectionJob:** `fraud_logs` có records (chỉ khi có fraud)

### 🌐 UI Results
10. [ ] **Spark UI:** Show 2 completed applications + 4 active streaming queries
11. [ ] **HBase UI:** Show 3 tables + 3 active regions

---

## 🚨 TROUBLESHOOTING

### Error 1: Spark job exit immediately
**Fix:** Gửi events trước khi chạy job (Step 4)

### Error 2: HBase connection failed
**Fix:** Chạy Step 2 - tạo znode

### Error 3: No data in Redis/HBase
**Fix:** 
- Check Spring Boot logs có "Successfully published"
- Check Spark logs có "Processing batch"
- Gửi nhiều events hơn

---

## 🎯 QUICK TEST COMMANDS

**One-liner để test nhanh:**
```bash
# 1. Gửi event
curl http://localhost:9595/api/payment/test-kafka

# 2. Check Redis
docker exec redis redis-cli HGETALL "spark:user_stats:1"

# 3. Check HBase
docker exec hbase /bin/bash -c "echo 'count \"payment_history\"' | /opt/hbase/bin/hbase shell -n"
```

**Expected final output:**
```
# Redis:
tx_count
1
total_amount
150000
avg_amount
150000.0

# HBase:
1 row(s)
```

---

## 💡 KEY POINTS

1. **Gửi events TRƯỚC khi chạy Spark job** - job sẽ exit nếu topic trống
2. **HBase cần znode /hbase/hbaseid** - tạo thủ công nếu thiếu  
3. **Check records chứ không chỉ tables** - data mới quan trọng
4. **Monitor logs real-time** - thấy processing mới chắc chắn thành công

**Luồng hoàn chỉnh:** API → Kafka → Spark → {Redis Analytics + HBase Storage} ✅
