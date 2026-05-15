# COMPLETE TEST GUIDE — Luồng Restart (Đã có container chạy lần trước)

Dùng guide này khi: đã từng chạy dự án, container đã tồn tại, muốn khởi động lại để test/demo.
Nếu lần đầu tải về chưa có gì: xem `FIRST-TEST-GUIDE.md`.

---

## BƯỚC 1 — Khởi động Infrastructure

```powershell
cd cine-be
docker compose up -d
```

Kiểm tra containers:
```powershell
docker ps --format "table {{.Names}}\t{{.Status}}"
```

Expected — phải thấy đủ 6 container này:
```
NAMES          STATUS
kafka          Up
redis          Up
zookeeper      Up
hbase          Up
spark-master   Up
spark-worker   Up
```

`hbase-init` sẽ có trạng thái `Exited` hoặc không xuất hiện — bình thường, tables đã tạo từ lần trước.

Fix hostname HBase (cần làm mỗi lần restart container hbase để lệnh count hoạt động):
```powershell
docker exec hbase bash -c "echo ""127.0.0.1 $(docker exec hbase hostname)"" >> /etc/hosts"
```

---

## BƯỚC 2 — Chạy Backend Spring Boot

Mở terminal riêng, đứng ở thư mục **root** của project:
```powershell
./mvnw spring-boot:run -pl cine-be
```

Chờ log: `Started CinegoTicketApplication on port 9595`

Verify nhanh:
```powershell
curl http://localhost:9595/actuator/health
```
Expected: `{"status":"UP"}`

---

## BƯỚC 3 — Chạy FraudDetectionJob

Mở terminal riêng:
```powershell
docker exec spark-master /opt/spark/bin/spark-submit --deploy-mode client --master spark://spark-master:7077 --class linh.vn.spark.job.FraudDetectionJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379
```

Log bình thường khi start (chưa có event):
```
INFO FraudDetectionJob: Starting FraudDetectionJob | kafka=kafka:9092 hbase=hbase:2181 redis=redis:6379
INFO MicroBatchExecution: Streaming query made progress ...
INFO MicroBatchExecution: Streaming query has been idle and waiting for new data more than 10000 ms.
```

Log "idle" là bình thường — job đang poll Kafka mỗi 10 giây, chưa có message mới.
FraudDetectionJob dùng `startingOffsets: earliest` → khi start lần đầu sẽ đọc lại toàn bộ Kafka topic từ đầu (kể cả message cũ còn trong retention 24h).

---

## BƯỚC 4 — Chạy AnalyticsJob

Mở terminal riêng:
```powershell
docker exec spark-master /opt/spark/bin/spark-submit --deploy-mode client --master spark://spark-master:7077 --class linh.vn.spark.job.AnalyticsJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379
```

Log bình thường khi start:
```
INFO AnalyticsJob: Starting AnalyticsJob | kafka=kafka:9092 ...
INFO AnalyticsJob: [AnalyticsJob] 3 queries still running...
INFO MicroBatchExecution: Streaming query has been idle and waiting for new data more than 10000 ms.
```

`3 queries still running` = AnalyticsJob có 3 streaming query song song (revenue, top_movies, user_stats) — đây là trạng thái bình thường, không phải lỗi.
AnalyticsJob dùng `startingOffsets: latest` → chỉ đọc event MỚI từ lúc job start, không đọc lại cũ.

---

## BƯỚC 5 — Gửi Test Event

```powershell
curl http://localhost:9595/api/payment/test-kafka
```

Expected response:
```
Test Kafka event published: test-1747282000000
```

Gửi nhiều event để thấy kết quả rõ hơn:
```powershell
# PowerShell — gửi 5 event cách nhau 1 giây
for ($i=1; $i -le 5; $i++) { curl http://localhost:9595/api/payment/test-kafka; Start-Sleep 1 }
```

Log trong terminal Spring Boot sau khi gửi:
```
Publishing event test-1747282000000
Successfully published payment event test-1747282000000 to partition 0 offset 10
```

Log trong terminal FraudDetectionJob sau khi nhận event (xuất hiện sau ~10 giây theo micro-batch):
```
INFO MicroBatchExecution: Streaming query made progress: {"numInputRows": 1, "processedRowsPerSecond": ...}
```

Log trong terminal AnalyticsJob tương tự — theo batch, không log từng event riêng lẻ.

---

## BƯỚC 6 — Kiểm tra kết quả Redis

Redis lưu kết quả của **cả 2 jobs** nhưng với mục đích khác nhau:

| Key pattern | Từ job | Ý nghĩa |
|---|---|---|
| `spark:user_stats:{userId}` | AnalyticsJob | Thống kê giao dịch theo user |
| `spark:top_movies:{date}` | AnalyticsJob | Bảng xếp hạng phim theo doanh thu |
| `spark:revenue:{date}:{hour}` | AnalyticsJob | Doanh thu theo giờ |
| `spark:fraud_count:{date}` | FraudDetectionJob | Số fraud phát hiện trong ngày (chỉ có khi có fraud) |

### 6.1 Xem tất cả keys
```powershell
docker exec redis redis-cli KEYS "spark:*"
```

Expected sau khi gửi events:
```
spark:user_stats:1
spark:top_movies:2026-05-15
spark:revenue:2026-05-15:03
```

### 6.2 User stats (AnalyticsJob)
```powershell
docker exec redis redis-cli HGETALL "spark:user_stats:1"
```

Expected sau khi gửi 5 events (userId=1, amount=150000 mỗi event):
```
tx_count
5
total_amount
750000
avg_amount
150000.0
```

Nếu đã gửi trước đó thì các giá trị sẽ cộng dồn (không reset).

### 6.3 Top movies (AnalyticsJob)
```powershell
docker exec redis redis-cli ZREVRANGE "spark:top_movies:2026-05-15" 0 -1 WITHSCORES
```

Expected (thay 2026-05-15 bằng ngày hiện tại):
```
123
750000
```

Nếu rỗng: AnalyticsJob chưa xử lý xong batch, chờ thêm 10-15 giây rồi check lại.

### 6.4 Revenue theo giờ (AnalyticsJob)
```powershell
docker exec redis redis-cli KEYS "spark:revenue:*"
docker exec redis redis-cli GET "spark:revenue:2026-05-15:03"
```

Expected: một số dương (tổng doanh thu giờ đó tính bằng VND).

### 6.5 Fraud count (FraudDetectionJob — chỉ có khi có fraud)
```powershell
docker exec redis redis-cli KEYS "spark:fraud_count:*"
docker exec redis redis-cli GET "spark:fraud_count:2026-05-15"
```

Expected khi gửi test event bình thường (amount=150000, 1 lần): key không tồn tại hoặc giá trị thấp.
Fraud bị trigger khi: cùng userId gửi nhiều transaction nhanh (smurfing) hoặc amount bất thường (Z-Score).

---

## BƯỚC 7 — Kiểm tra kết quả HBase

HBase lưu data của **cả 2 jobs**:

| Table | Từ job | Nội dung |
|---|---|---|
| `payment_history` | FraudDetectionJob | Lịch sử payment dùng để tính Z-Score |
| `analytics_daily` | AnalyticsJob | Tổng hợp doanh thu/movie theo ngày |
| `fraud_logs` | FraudDetectionJob | Chi tiết các fraud alert đã phát hiện |

### 7.1 Kiểm tra tables tồn tại
```powershell
docker exec hbase bash -c "echo 'list' | /opt/hbase/bin/hbase shell -n 2>/dev/null"
```

Expected:
```
TABLE
analytics_daily
fraud_logs
payment_history
3 row(s)
```

### 7.2 Đếm records — payment_history (FraudDetectionJob)
```powershell
docker exec hbase bash -c "echo 'count ""payment_history""' | /opt/hbase/bin/hbase shell -n 2>/dev/null"
```

Expected sau khi gửi 5 events:
```
Current count: 5, row: ...
5 row(s)
Took X seconds
```

Nếu báo lỗi `UnknownHostException: can not resolve <hostname>`: chạy lại lệnh fix hostname ở Bước 1 rồi thử lại.

### 7.3 Đếm records — analytics_daily (AnalyticsJob)
```powershell
docker exec hbase bash -c "echo 'count ""analytics_daily""' | /opt/hbase/bin/hbase shell -n 2>/dev/null"
```

Expected: số row bằng số cặp `(date, movieId)` độc nhất đã xử lý.

### 7.4 Đếm records — fraud_logs (FraudDetectionJob)
```powershell
docker exec hbase bash -c "echo 'count ""fraud_logs""' | /opt/hbase/bin/hbase shell -n 2>/dev/null"
```

Expected khi test bình thường: `0 row(s)`.
Sẽ có records khi: gửi nhiều transaction liên tiếp từ cùng user trong thời gian ngắn.

---

## BƯỚC 8 — Kiểm tra Spark UI

Mở browser: `http://localhost:8080`

Expected khi cả 2 jobs đang chạy:
- **Running Applications**: 2 (FraudDetectionJob + AnalyticsJob)
- Mỗi application có trạng thái `RUNNING`

Click vào từng application để xem:
- **Streaming Queries**: FraudDetectionJob có 1 query, AnalyticsJob có 3 queries
- **Input Rate**: tăng lên khi gửi event
- **Batch Duration**: thời gian xử lý mỗi micro-batch (~10 giây)

HBase UI: `http://localhost:16010` — xem tables, regions đang hoạt động.

---

## CHECKLIST XÁC NHẬN LUỒNG CHẠY ĐÚNG

- [ ] 6 container Up (Bước 1)
- [ ] Spring Boot log `Started CinegoTicketApplication on port 9595` (Bước 2)
- [ ] FraudDetectionJob log `Streaming query started` + log `idle` (Bước 3)
- [ ] AnalyticsJob log `3 queries still running` (Bước 4)
- [ ] Gửi event nhận response `Test Kafka event published` (Bước 5)
- [ ] Spring Boot log `Successfully published payment event` (Bước 5)
- [ ] Redis `spark:user_stats:1` có `tx_count` tăng lên (Bước 6)
- [ ] Redis `spark:top_movies:{ngày hôm nay}` có movieId 123 (Bước 6)
- [ ] HBase `payment_history` count tăng lên (Bước 7)
- [ ] Spark UI http://localhost:8080 hiện 2 Running Applications (Bước 8)

---

## LƯU Ý QUAN TRỌNG

**Kafka giữ message 24h (retention):**
FraudDetectionJob dùng `startingOffsets: earliest` nên khi start sẽ tự đọc lại event cũ còn trong Kafka. Đây là lý do Redis có thể đã có data trước khi bạn gửi event mới. Không cần xóa Kafka, chỉ cần gửi event mới và check kết quả tăng lên.

**Log Spark không hiện từng event:**
Spark Structured Streaming xử lý theo batch (~10 giây/batch). Log `Batch N completed — X rows` xuất hiện sau mỗi batch, không log từng event riêng lẻ.

**`spark:top_movies:{date}` rỗng dù đã gửi event:**
Key này dùng ngày hiện tại. Nếu event cũ trong Kafka có timestamp ngày khác thì kết quả ghi vào key ngày đó, không phải ngày hôm nay. Gửi event mới (timestamp = now) thì key ngày hôm nay mới có data.
