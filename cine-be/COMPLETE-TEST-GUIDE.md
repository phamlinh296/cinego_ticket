# COMPLETE TEST GUIDE — Luồng Restart (Đã có container chạy lần trước)

Dùng guide này khi: muốn khởi động và demo hệ thống (lần đầu hoặc restart lại).

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

Expected — phải thấy đủ 6 container:

```
NAMES          STATUS
kafka          Up
redis          Up
zookeeper      Up
hbase          Up
spark-master   Up
spark-worker   Up
```

Chờ HBase sẵn sàng — chạy lại cho đến khi thấy `1 servers, 0 dead`:

```powershell
docker exec hbase bash -c "echo 'status' | /opt/hbase/bin/hbase shell -n 2>/dev/null"
```

Expected: `1 active master, 0 backup masters, 1 servers, 0 dead, 2.0000 average load`

**Nếu HBase bị stuck** (log báo "Master startup cannot progress"): ZooKeeper đang giữ stale data từ lần trước. Fix:

```powershell
docker compose restart zookeeper
docker compose restart hbase
```

Chờ ~30s rồi chạy lại lệnh `status` ở trên.

Kiểm tra và tạo HBase tables:

```powershell
docker exec hbase bash -c "echo 'list' | /opt/hbase/bin/hbase shell -n 2>/dev/null"
```

- Thấy **3 tables** (`analytics_daily`, `fraud_logs`, `payment_history`) → bỏ qua, chuyển bước tiếp theo.
- Thấy **0 tables** → tạo thủ công:

```powershell
docker exec hbase bash -c "echo \"create 'payment_history', 'cf'\" | /opt/hbase/bin/hbase shell -n 2>/dev/null"
docker exec hbase bash -c "echo \"create 'fraud_logs', 'cf'\" | /opt/hbase/bin/hbase shell -n 2>/dev/null"
docker exec hbase bash -c "echo \"create 'analytics_daily', 'cf'\" | /opt/hbase/bin/hbase shell -n 2>/dev/null"
```

Expected mỗi lệnh: `Created table X`

Pre-create Kafka topic `anomaly-events` (FraudDetectionJob publish fraud alerts vào đây — **bắt buộc** phải làm trước
khi submit job, thiếu topic sẽ khiến batch treo vô hạn):

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --create --topic anomaly-events --partitions 3 --replication-factor 1 --if-not-exists
```

Expected: `Created topic anomaly-events.` (hoặc `Topic 'anomaly-events' already exists.` nếu đã có — đều ổn)

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
docker exec spark-master /opt/spark/bin/spark-submit --deploy-mode client --master spark://spark-master:7077 --conf spark.cores.max=4 --class linh.vn.spark.job.FraudDetectionJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379
```

Log bình thường khi start (chưa có event):

```
INFO FraudDetectionJob: Starting FraudDetectionJob | kafka=kafka:9092 hbase=hbase:2181 redis=redis:6379
INFO MicroBatchExecution: Streaming query made progress ...
INFO MicroBatchExecution: Streaming query has been idle and waiting for new data more than 10000 ms.
```

Log "idle" là bình thường — job đang poll Kafka mỗi 10 giây, chưa có message mới.
FraudDetectionJob dùng `startingOffsets: earliest` → khi start lần đầu sẽ đọc lại toàn bộ Kafka topic từ đầu (kể cả
message cũ còn trong retention 24h).

---

## BƯỚC 4 — Chạy AnalyticsJob

Mở terminal riêng:

```powershell
docker exec spark-master /opt/spark/bin/spark-submit --deploy-mode client --master spark://spark-master:7077 --conf spark.cores.max=4 --class linh.vn.spark.job.AnalyticsJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379
```

Log bình thường khi start:

```
INFO AnalyticsJob: Starting AnalyticsJob | kafka=kafka:9092 ...
INFO AnalyticsJob: [AnalyticsJob] 3 queries still running...
INFO MicroBatchExecution: Streaming query has been idle and waiting for new data more than 10000 ms.
```

`3 queries still running` = AnalyticsJob có 3 streaming query song song (revenue, top_movies, user_stats) — đây là trạng
thái bình thường, không phải lỗi.
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

**Không cần nhìn log terminal ở bước này** — Spark xử lý theo batch 30s, log xuất hiện chậm và bị chôn vùi trong INFO
spam của Spark. Chờ 30 giây rồi check Redis ở BƯỚC 6 để xác nhận. Xem BƯỚC 9 để biết đúng log nào cần tìm khi debug.

---

## BƯỚC 5.5 — Trigger Fraud Detection (HIGH_FREQUENCY_WINDOW)

Fraud chỉ trigger khi cùng 1 userId gửi **≥ 10 transaction trong 1 giờ** (HIGH_FREQUENCY_WINDOW) hoặc amount bất thường
so với lịch sử (ZSCORE_DEEP).

> **Lưu ý:** FraudDetectionJob dùng `startingOffsets: earliest` → khi không có checkpoint, nó đọc lại toàn bộ Kafka cũ (
> retention 24h). Nếu đã gửi nhiều event từ trước, fraud_count có thể đã tăng ngay khi job start mà không cần BƯỚC này.
> Kiểm tra `GET spark:fraud_count:{ngày}` trước — nếu đã có giá trị thì fraud đã trigger rồi, bỏ qua bước gửi thêm.

Gửi nhanh 10+ event để trigger HIGH_FREQUENCY_WINDOW:

```powershell
# PowerShell — gửi 10 event nhanh nhất có thể (không sleep)
for ($i=1; $i -le 10; $i++) { curl http://localhost:9595/api/payment/test-kafka }
```

Chờ **30-60 giây** (1-2 micro-batch) rồi check:

```powershell
# Fraud count trong ngày
docker exec redis redis-cli GET "spark:fraud_count:2026-05-15"
```

Expected khi fraud bị trigger (thay ngày hiện tại):

```
1
```

(Giá trị nhỏ là đúng — xem giải thích dedup ở BƯỚC 6.5)

(Con số tích lũy theo sliding window — mỗi window 1h/5min slide có thể cho nhiều alert)

```powershell
# Fraud logs trong HBase
docker exec -e "HCMD=scan 'fraud_logs', {LIMIT => 3}" hbase bash -c 'echo "$HCMD" | /opt/hbase/bin/hbase shell -n 2>/dev/null'
```

Expected:

```
ROW                                                COLUMN+CELL
 1_HIGH_FREQUENCY_WINDOW_9223370258003049761       column=cf:alertType, value=HIGH_FREQUENCY_WINDOW
 1_HIGH_FREQUENCY_WINDOW_9223370258003049761       column=cf:description, value=30 transactions in 1h window (threshold: 10)
 1_HIGH_FREQUENCY_WINDOW_9223370258003049761       column=cf:userId, value=1
3 row(s)
```

Row key format: `{userId}_{alertType}_{reverseTimestamp}` — sort newest first, scan theo user nhanh.

**Sau khi chạy 10 events nhanh, các Redis key thay đổi như sau:**

| Key                           | Thay đổi                                        | Ghi chú                                                                    |
|-------------------------------|-------------------------------------------------|----------------------------------------------------------------------------|
| `spark:user_stats:1`          | **Cộng dồn** tx_count, total_amount             | tx_count tăng đúng bằng số PAID event nhận được                             |
| `spark:top_movies:{date}`     | Score tăng thêm 10 × 150,000                    | Cộng dồn qua ZINCRBY                                                       |
| `spark:revenue:{date}:{hour}` | Tăng thêm 1,500,000                             | Cộng dồn vào giờ hiện tại                                                  |
| `spark:fraud_count:{date}`    | Key mới xuất hiện (nếu chưa có), hoặc tăng thêm | Chỉ tạo khi có fraud — giá trị = số batch phát hiện fraud (dedup per userId:alertType) |

Log trong terminal FraudDetectionJob khi fraud được phát hiện:

```
INFO FraudDetectionJob: [FraudDetectionJob] Processing batch #N
INFO FraudDetectionJob: [FraudDetectionJob] User=1 events=10 alerts=34
INFO HBaseSink: [HBaseSink] Wrote 34 fraud alerts to HBase
INFO KafkaSink: [KafkaSink] Published 34 fraud alerts to topic anomaly-events (async, no flush)
```

`alerts=34` nhiều hơn 10 event gửi là bình thường — sliding window 1h/5min tạo ra nhiều overlapping windows, mỗi window
đều trigger HIGH_FREQUENCY nếu count ≥ 10.

---

## BƯỚC 6 — Kiểm tra kết quả Redis

Redis lưu kết quả của **cả 2 jobs** nhưng với mục đích khác nhau:

| Key pattern                   | Từ job            | Ý nghĩa                                             |
|-------------------------------|-------------------|-----------------------------------------------------|
| `spark:user_stats:{userId}`   | AnalyticsJob      | Thống kê giao dịch theo user                        |
| `spark:top_movies:{date}`     | AnalyticsJob      | Bảng xếp hạng phim theo doanh thu                   |
| `spark:revenue:{date}:{hour}` | AnalyticsJob      | Doanh thu theo giờ                                  |
| `spark:fraud_count:{date}`    | FraudDetectionJob | Số fraud phát hiện trong ngày (chỉ có khi có fraud) |

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

Nếu đã gửi trước đó thì các giá trị sẽ **cộng dồn** (không reset) — đây là behavior đúng (HINCRBY/HINCRBYFLOAT).

**Lưu ý về tx_count:** `tx_count` đếm số **PAID event** thực sự nhận được. AnalyticsJob Query 3 xử lý từng event riêng lẻ (không dùng window), gọi `updateUserStats` 1 lần per event → gửi 5 event PAID → tx_count tăng đúng 5. Chỉ sự kiện có `status=PAID` mới được đếm.

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

Giải thích output: `ZREVRANGE ... WITHSCORES` trả về cặp xen kẽ [member, score] theo thứ tự score giảm dần. `123` =
movieId (member của Sorted Set); `750000` = tổng doanh thu VND từ phim đó trong tumbling window 1h (tích lũy bằng
ZINCRBY). Nếu nhiều phim: phim doanh thu cao nhất hiện trước.

### 6.4 Revenue theo giờ (AnalyticsJob)

```powershell
docker exec redis redis-cli KEYS "spark:revenue:*"
docker exec redis redis-cli GET "spark:revenue:2026-05-15:03"
```

Expected: một số dương (tổng doanh thu giờ đó tính bằng VND).

Giải thích key: `{hour}` là 2 chữ số UTC 00–23. Value là float (INCRBYFLOAT), tích lũy theo sliding window 1h/5min —
tăng dần mỗi batch khi có event mới trong giờ đó. Nếu `KEYS` trả về nhiều key → đã có event ở nhiều giờ khác nhau (TTL
48h nên key từ hôm qua vẫn còn).

### 6.5 Fraud count (FraudDetectionJob — chỉ có khi có fraud)

```powershell
docker exec redis redis-cli KEYS "spark:fraud_count:*"
docker exec redis redis-cli GET "spark:fraud_count:2026-05-15"
```

Expected khi gửi **1 event bình thường**: key không tồn tại (chưa có fraud).
Expected khi gửi **10+ event nhanh** (BƯỚC 5.5): giá trị dương nhỏ, thường `1` đến `5`.

Giải thích con số: fraud_count = **số batch phát hiện fraud**, không phải số alert. FraudDetectionJob dedup alerts theo `userId:alertType` trước khi increment → dù sliding window sinh ra 12 alert `HIGH_FREQUENCY_WINDOW` trong 1 batch, sau dedup chỉ có 1 unique `"1:HIGH_FREQUENCY_WINDOW"` → `incrementFraudCount` gọi 1 lần. Mỗi batch 30s phát hiện fraud → fraud_count tăng 1. Giá trị nhỏ là đúng, không phải thiếu data.

---

## BƯỚC 7 — Kiểm tra kết quả HBase

HBase lưu data của **cả 2 jobs**:

| Table             | Từ job            | Nội dung                                          | Dùng để demo |
|-------------------|-------------------|---------------------------------------------------|--------------|
| `analytics_daily` | AnalyticsJob      | Tổng hợp doanh thu/movie theo ngày                | ✅            |
| `fraud_logs`      | FraudDetectionJob | Chi tiết các fraud alert đã phát hiện             | ✅            |
| `payment_history` | FraudDetectionJob | Lịch sử amount per user — đọc bởi ZScoreWindowCalculator để tính deep Z-Score | ✅            |

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

### 7.2 Đếm records — payment_history

`FraudDetectionJob` ghi mỗi `PaymentEvent` vào table này trước khi chạy pattern detection. `ZScoreWindowCalculator` đọc lại để tính deep Z-Score với 500 records lịch sử.

```powershell
'count "payment_history"' | docker exec -i hbase /opt/hbase/bin/hbase shell
```

Expected sau khi gửi events: số row > 0 và tăng dần theo số event gửi.

Nếu báo lỗi `UnknownHostException`: hbase-data có data cũ từ hostname khác. Fix: restart ZooKeeper + HBase (xem Bước 1),
sau đó tạo lại tables.

### 7.3 Đếm records — analytics_daily (AnalyticsJob)

```powershell
'count "analytics_daily"' | docker exec -i hbase /opt/hbase/bin/hbase shell
```

Expected: số row bằng số cặp `(date, movieId)` độc nhất đã xử lý.

### 7.4 Đếm records — fraud_logs (FraudDetectionJob)

```powershell
'count "fraud_logs"' | docker exec -i hbase /opt/hbase/bin/hbase shell
```

Expected khi test 1 event bình thường: `0 row(s)`.
Expected sau BƯỚC 5.5 (10+ event nhanh): số dương và tăng dần theo sliding window (ví dụ 258 row(s)).

### 7.5 Xem data thực trong từng table

```powershell
# analytics_daily — row key đọc được: {date}_{movieId}
docker exec -e "HCMD=scan 'analytics_daily'" hbase bash -c 'echo "$HCMD" | /opt/hbase/bin/hbase shell -n 2>/dev/null'
```

Output mẫu sau khi gửi 5 events:

```
ROW                         COLUMN+CELL
 2026-05-15___ALL__         column=cf:revenue, timestamp=..., value=\x00\x00\x00\x00\x00\x12N \
 2026-05-15___ALL__         column=cf:tx_count, timestamp=..., value=\x00\x00\x00\x00\x00\x00\x00\x05
 2026-05-15_123             column=cf:revenue, timestamp=..., value=\x00\x00\x00\x00\x00\x12N \
 2026-05-15_123             column=cf:tx_count, timestamp=..., value=\x00\x00\x00\x00\x00\x00\x00\x05
2 row(s)
```

Row key pattern: `{date}___ALL__` = aggregate toàn bộ revenue (từ Query 1 — Revenue); `{date}_{movieId}` = per-movie (từ Query 2 — Top movies). Giá trị numeric hiển thị dạng binary bytes — bình thường, HBase lưu kiểu binary.

```powershell
# payment_history — luôn rỗng (missing feature, xem BƯỚC 7.2)
docker exec -e "HCMD=scan 'payment_history', {LIMIT => 5}" hbase bash -c 'echo "$HCMD" | /opt/hbase/bin/hbase shell -n 2>/dev/null'
```

```powershell
# fraud_logs — chỉ có khi fraud bị trigger
docker exec -e "HCMD=scan 'fraud_logs', {LIMIT => 3}" hbase bash -c 'echo "$HCMD" | /opt/hbase/bin/hbase shell -n 2>/dev/null'
```

Output fraud_logs khi có fraud (alertType, description, userId là String → đọc được; amount, riskScore, detectedAt là
binary bytes → hiện dạng `\xXX`):

```
ROW                                                COLUMN+CELL
 1_HIGH_FREQUENCY_WINDOW_9223370258003049761       column=cf:alertType, value=HIGH_FREQUENCY_WINDOW
 1_HIGH_FREQUENCY_WINDOW_9223370258003049761       column=cf:amount, value=AQ*\x88\x00\x00\x00\x00
 1_HIGH_FREQUENCY_WINDOW_9223370258003049761       column=cf:description, value=30 transactions in 1h window (threshold: 10)
 1_HIGH_FREQUENCY_WINDOW_9223370258003049761       column=cf:detectedAt, value=\x00\x00\x01\x9E+\xD3B\xDE
 1_HIGH_FREQUENCY_WINDOW_9223370258003049761       column=cf:paymentId, value=
 1_HIGH_FREQUENCY_WINDOW_9223370258003049761       column=cf:riskScore, value=?\xE8\x00\x00\x00\x00\x00\x00
 1_HIGH_FREQUENCY_WINDOW_9223370258003049761       column=cf:userId, value=1
 1_HIGH_FREQUENCY_WINDOW_9223370258003049796       column=cf:description, value=35 transactions in 1h window (threshold: 10)
 1_HIGH_FREQUENCY_WINDOW_9223370258003049796       column=cf:userId, value=1
3 row(s)
```

**Cách đọc output:** Mỗi **row key** = 1 fraud alert. HBase hiển thị từng column trên 1 dòng riêng → 1 alert với 7
columns = 7 dòng. Tất cả dòng có cùng row key thuộc về cùng 1 alert. `{LIMIT => 3}` = giới hạn **3 row keys** (3 alert),
không phải 3 dòng text.

Field đọc được rõ: `alertType=HIGH_FREQUENCY_WINDOW`, `userId=1`,
`description=N transactions in 1h window (threshold: 10)`. Field binary (`amount`, `riskScore`, `detectedAt`) hiện
`\xXX` — HBase lưu số dạng bytes, đây là behavior đúng, không phải lỗi.

### 7.6 Check payment_history có dữ liệu thực (Z-Score deep)

Sau khi gửi event, chờ 1 batch (30s) rồi check:

```powershell
'count "payment_history"' | docker exec -i hbase /opt/hbase/bin/hbase shell
```

Nếu count > 0 → ZScoreWindowCalculator có data để tính deep Z-Score → ZSCORE_DEEP alert có thể trigger khi amount bất thường.

---

## BƯỚC 7.7 — Kiểm tra UI Admin

Admin chạy qua Docker container Nginx riêng (Dockerfile trong `admin/`). Build và chạy lần đầu:

```powershell
cd ..\admin
docker build -t cinego-admin .
docker run -d --name cinego-admin -p 80:80 cinego-admin
cd ..\cine-be
```

Truy cập qua `http://localhost`.

**Analytics Dashboard** — xem top phim + doanh thu theo giờ từ Redis:
```
http://localhost/analytics.html
```
Expected: hiện bảng "Top Phim Theo Doanh Thu Hôm Nay" (tên phim) và "Doanh Thu Theo Giờ Hôm Nay".
Nếu rỗng: chưa có Redis data cho ngày hôm nay → gửi thêm event (Bước 5) rồi chờ 30s.

**Anomaly Logs** — xem fraud alerts từ Spring AnomalyConsumer:
```
http://localhost/anomalies.html
```
Expected: danh sách các giao dịch bất thường đã được phát hiện và lưu vào MySQL.
Nếu rỗng: Spring-side anomaly detection chưa trigger (cần nhiều event hơn hoặc amount đủ bất thường so với lịch sử Redis).

---

## BƯỚC 8 — Kiểm tra Spark UI và HBase UI

### 8.1 Spark UI — http://localhost:8080

**Trạng thái ban đầu (chưa submit job):**

```
Workers: 1 — ALIVE, 16 cores, 0 Used
Running Applications: 0
```

**Sau khi submit 2 jobs thành công:**

```
Workers: 1 — ALIVE, 16 (16 Used)     ← worker đang cấp cores
Running Applications: 2
  CineGoTicket-FraudDetection   RUNNING   16 cores
  CineGoTicket-Analytics        RUNNING   16 cores
```

Click vào tên app → tab **Structured Streaming**:

- Active Streaming Queries: 1 (FraudDetection) hoặc 3 (Analytics)
- **Input Rate**: tăng lên khi Kafka có event mới
- **Batch Duration**: thời gian xử lý mỗi micro-batch (~30s)
- Batch list: mỗi 30s thêm 1 dòng, cột **Input Rows > 0** khi có event

**Sau khi gửi event và xử lý xong:**

- Input Rate tăng trong ~30s rồi về 0
- Batch list: dòng mới nhất có `Input Rows = số event gửi`, status Completed

**Dấu hiệu có vấn đề:**

- App ở trạng thái `WAITING, 0 cores` + log `Initial job has not accepted any resources` → worker hết tài nguyên do có
  job cũ còn chạy. Fix: click **(kill)** các app cũ trên UI, hoặc restart spark containers.
- Active Streaming Queries giảm từ 3 xuống 1-2 → query chết, xem Troubleshooting Error 4.

**Khi có nhiều app Running (đã submit nhiều lần):** kill các app cũ để giải phóng cores cho app mới.

### 8.2 HBase — dùng terminal (không dùng UI)

`dajobe/hbase` (HBase 2.1.2) có bug: **User Tables section trên UI tại localhost:16010 thường rỗng** dù tables tồn tại —
đây là lỗi render của image, không phải lỗi hệ thống.

**Không cần show UI.** Dùng terminal commands ở BƯỚC 7 để chứng minh data — chuyên nghiệp hơn và đáng tin hơn UI.

Nếu tò mò muốn thử: `http://localhost:16010/tablesDetailed.jsp` (đôi khi hiện, đôi khi không tùy version).

---

## BƯỚC 9 — Đọc log terminal trong lúc demo

Log terminal **không cần nhìn liên tục** — chỉ cần xác nhận trạng thái ban đầu rồi để đó.

### Log bình thường khi job đang chờ (chưa gửi event):

```
# FraudDetectionJob terminal — xuất hiện mỗi 60 giây:
INFO FraudDetectionJob: [FraudDetectionJob] Fraud detection query still running...

# AnalyticsJob terminal — xuất hiện mỗi 60 giây:
INFO AnalyticsJob: [AnalyticsJob] 3 queries still running...

# Cả 2 terminal — xuất hiện mỗi 10 giây khi không có event:
INFO MicroBatchExecution: Streaming query has been idle and waiting for new data more than 10000 ms.
```

Đây là trạng thái **bình thường** — jobs đang sống và chờ event.

### Log xuất hiện sau khi gửi event (trong vòng 30 giây):

```
# FraudDetectionJob — xác nhận đã xử lý batch:
INFO FraudDetectionJob: [FraudDetectionJob] Processing batch #1
INFO FraudDetectionJob: [FraudDetectionJob] User=1 events=1 alerts=0

# AnalyticsJob — xác nhận revenue được tính:
INFO AnalyticsJob: [AnalyticsJob] Revenue batch #1: 1 windows
INFO AnalyticsJob: [AnalyticsJob] Revenue: date=2026-05-15 hour=7 revenue=150000.0 txCount=1

# Cả 2 terminal — Spark báo có data được xử lý:
INFO MicroBatchExecution: Streaming query made progress: {"numInputRows": 1, ...}
```

`alerts=0` khi gửi test event bình thường là đúng — fraud chỉ trigger khi gửi nhiều event liên tiếp (≥10 trong 1 giờ)
hoặc amount bất thường.

### Khi nào batch tăng lên:

- FraudDetectionJob: trigger mỗi **30 giây** → batch tăng mỗi 30s
- AnalyticsJob revenue/top-movies: trigger mỗi **30 giây - 1 phút**
- AnalyticsJob user-stats: trigger mỗi **30 giây**
- Gửi 5 event trong 10 giây → tất cả vào cùng 1 batch (gom lại theo trigger interval)

---

## KHI NÀO XÓA SPARK CHECKPOINT

Checkpoint lưu tại `/tmp/spark-checkpoint/` bên trong container `spark-master`.

### Phải xóa checkpoint khi:

| Tình huống                                 | Dấu hiệu                                                                    | Lý do                                      |
|--------------------------------------------|-----------------------------------------------------------------------------|--------------------------------------------|
| **Thay đổi schema PaymentEvent**           | Spark báo `StreamingQueryException: Schema changed`                         | Checkpoint lưu schema cũ, không compatible |
| **Thay đổi window size** (1h → 2h)         | Kết quả sai, aggregate không đúng                                           | State store dùng window key cũ             |
| **Thay đổi output mode** (append ↔ update) | Spark báo lỗi ngay khi start                                                | Mode encode vào checkpoint                 |
| **Batch bị stuck sau restart**             | Có `offsets/0` nhưng không có `commits/0` → batch 0 replay mãi không commit | Incomplete batch từ lần chạy trước         |

Cách nhận biết "batch stuck": job start, log `Processing batch #0` xuất hiện, nhưng **không bao giờ** thấy log tiếp
theo (`User=1 events=Y alerts=Z`) và Redis/HBase không cập nhật sau 2+ phút. Batch 0 cứ bị replay lại mãi.

### KHÔNG cần xóa checkpoint khi:

- Fix logic trong PatternDetector, RedisSink, HBaseSink
- Thêm Redis key mới
- Restart job sau khi kill (checkpoint giúp resume đúng offset)
- Fix bug trong sink layer (không đổi schema hay window)

### Cách xóa:

```powershell
# Xóa toàn bộ checkpoint (cả FraudDetection và Analytics)
docker exec spark-master rm -rf /tmp/spark-checkpoint/

# Hoặc xóa riêng từng job
docker exec spark-master rm -rf /tmp/spark-checkpoint/fraud-detection
docker exec spark-master rm -rf /tmp/spark-checkpoint/analytics
```

Sau khi xóa: submit lại 2 Spark job. FraudDetectionJob với `startingOffsets: earliest` sẽ đọc lại toàn bộ Kafka topic (
có thể sinh nhiều alert từ event cũ — bình thường).

---

## LUỒNG DEMO

Thứ tự show để demo rõ ràng nhất:

```
1. Mở sẵn 4 cửa sổ:
   - Terminal A: FraudDetectionJob đang chạy (thấy heartbeat mỗi 60s)
   - Terminal B: AnalyticsJob đang chạy (thấy "3 queries still running")
   - Terminal C: sẵn sàng gửi event và check Redis/HBase
   - Browser: http://localhost:8080 (Spark UI)

2. Show Spark UI → Running Applications: 2 = 2 job đang streaming

3. Gửi event (Terminal C):
   curl http://localhost:9595/api/payment/test-kafka
   → Response ngay: "Test Kafka event published: test-XXXXX"

4. Chờ 30 giây, check Redis (Terminal C):
   docker exec redis redis-cli HGETALL "spark:user_stats:1"
   → tx_count tăng = pipeline API → Kafka → Spark → Redis hoạt động end-to-end

   docker exec redis redis-cli ZREVRANGE "spark:top_movies:2026-05-15" 0 -1 WITHSCORES
   → thấy movieId 123 = AnalyticsJob đang tính ranking realtime

5. Check HBase (Terminal C):
   docker exec -e "HCMD=scan 'analytics_daily'" hbase bash -c 'echo "$HCMD" | /opt/hbase/bin/hbase shell -n 2>/dev/null'
   → thấy row "2026-05-15_123" với tx_count, revenue = AnalyticsJob aggregate vào HBase

6. Demo Fraud Detection (Terminal C):
   for ($i=1; $i -le 10; $i++) { curl http://localhost:9595/api/payment/test-kafka }
   → Gửi nhanh 10 event

   Chờ 30-60 giây rồi check:

   docker exec redis redis-cli GET "spark:fraud_count:2026-05-15"
   → thấy số dương (vd: 38) = FraudDetectionJob đã phát hiện HIGH_FREQUENCY_WINDOW

   docker exec -e "HCMD=scan 'fraud_logs', {LIMIT => 3}" hbase bash -c 'echo "$HCMD" | /opt/hbase/bin/hbase shell -n 2>/dev/null'
   → thấy row "1_HIGH_FREQUENCY_WINDOW_..." với alertType=HIGH_FREQUENCY_WINDOW,
     description="30 transactions in 1h window (threshold: 10)", userId=1
```

7. Show UI Admin (mở sẵn từ trước):
   - analytics.html → top phim + doanh thu theo giờ realtime từ Redis
   - anomalies.html → fraud alert list từ MySQL (Spring-side detection)

8. Check HBase payment_history có data:
   'count "payment_history"' | docker exec -i hbase /opt/hbase/bin/hbase shell
   → count > 0 = Spark đang ghi lịch sử để tính Z-Score deep

**Điểm nhấn khi demo:**

- Bước 3-4: pipeline end-to-end (API → Kafka → Spark → Redis → HBase) chạy realtime
- Bước 5: HBase lưu analytics aggregate theo ngày/phim
- Bước 6: Fraud detection tự động phát hiện transaction bất thường, ghi log vào HBase + tăng counter Redis
- Bước 7: UI admin hiện kết quả analytics và fraud alert trực quan
- Bước 8: HBase payment_history có data = Z-Score deep analysis hoạt động đầy đủ

---

## CHECKLIST XÁC NHẬN LUỒNG CHẠY ĐÚNG

**Infrastructure:**

- [ ] 6 container Up (Bước 1)
- [ ] Spring Boot log `Started CinegoTicketApplication on port 9595` (Bước 2)

**Jobs đang sống:**

- [ ] FraudDetectionJob log `Fraud detection query still running...` (Bước 3)
- [ ] AnalyticsJob log `3 queries still running...` — nếu thấy 1 hoặc 2: xem Troubleshooting Error 4 (Bước 4)
- [ ] Spark UI `http://localhost:8080` thấy **2 Running Applications** (Bước 8)

**Pipeline hoạt động sau khi gửi event:**

- [ ] Response `Test Kafka event published` (Bước 5)
- [ ] Spring Boot log `Successfully published payment event` (Bước 5)
- [ ] FraudDetectionJob log `Processing batch #X` + `User=1 events=Y alerts=0` xuất hiện trong 30s (Bước 9)
- [ ] Redis `spark:user_stats:1` → `tx_count` tăng lên (Bước 6)
- [ ] Redis `spark:top_movies:{ngày hôm nay}` → có movieId 123 (Bước 6)
- [ ] HBase `analytics_daily` scan → thấy row `{ngày}_{movieId}` (Bước 7.5)

**Fraud detection (sau BƯỚC 5.5 — 10+ event nhanh):**

- [ ] FraudDetectionJob log `User=1 events=10 alerts=N` (N > 0)
- [ ] Redis `spark:fraud_count:{ngày hôm nay}` → có giá trị dương (Bước 6.5)
- [ ] HBase `fraud_logs` count > 0 (Bước 7.4)
- [ ] HBase `payment_history` count > 0 — Z-Score deep có data (Bước 7.2)
- [ ] HBase `fraud_logs` scan → thấy `alertType=HIGH_FREQUENCY_WINDOW`, `description=N transactions in 1h window` (Bước 7.5)

**UI Admin:**

- [ ] `analytics.html` hiện top phim + doanh thu theo giờ (Bước 7.7)
- [ ] `anomalies.html` hiện fraud alert list (Bước 7.7)

---

## LƯU Ý QUAN TRỌNG

**Kafka giữ message 24h:**
FraudDetectionJob dùng `startingOffsets: earliest` nên khi start sẽ đọc lại event cũ trong Kafka. Redis có thể đã có
data từ trước khi gửi event mới — không cần xóa, chỉ cần check giá trị tăng lên sau mỗi lần gửi.

**Log Spark không hiện từng event:**
Xử lý theo batch (30s/batch). Tìm `Processing batch #X` trong terminal FraudDetectionJob và `Revenue batch #X` trong
AnalyticsJob để xác nhận event đã được xử lý.

**`spark:top_movies:{date}` rỗng dù đã gửi event:**
Key dùng ngày hiện tại. Event cũ từ ngày trước ghi vào key ngày đó, không phải hôm nay. Gửi event mới → key ngày hôm nay
mới có data.

**Spark UI hiện 0 Running Applications:**
Đảm bảo đã restart spark-master/worker sau khi rebuild JAR. Nếu vẫn 0: job đang chạy local mode thay vì cluster mode —
rebuild lại JAR với bản mới nhất và restart containers.

**Spark UI hiện nhiều app WAITING + log "Initial job has not accepted any resources":**
Worker hết tài nguyên do job cũ từ lần submit trước vẫn còn giữ cores. Fix: click **(kill)** các app cũ trên Spark UI,
hoặc restart spark containers. Mỗi lần submit job là 1 app mới — phải kill app cũ trước khi submit lại.

**AnalyticsJob hiện "2 queries still running" sau khi gửi event:**
Thường xảy ra khi job đang ở trạng thái WAITING (không có executor). Query bị timeout rồi chết. Fix: kill app cũ → giải
phóng resources → restart AnalyticsJob → phải thấy 3 queries ngay từ đầu.

**HBase UI không thấy bảng trong User Tables:**
Bug của `dajobe/hbase` image (HBase 2.1.2) — UI thường rỗng dù tables tồn tại. Bỏ qua UI, dùng terminal commands ở BƯỚC
7.

**HBase `scan` báo lỗi NameError hoặc unexpected EOF trong PowerShell:**
Nguyên nhân: PowerShell 5.1 trên Windows bị double-escaping khi pass nested quotes qua nhiều tầng (`"..."` → docker →
bash → HBase shell). Cả `\"tablename\"` và `""tablename""` đều có thể fail. Fix dứt điểm: dùng `-e` để truyền HBase
command qua env var, tránh nested quote hoàn toàn:

```powershell
docker exec -e "HCMD=scan 'tablename', {LIMIT => 3}" hbase bash -c 'echo "$HCMD" | /opt/hbase/bin/hbase shell -n 2>/dev/null'
```

Tất cả lệnh scan trong guide này đã dùng pattern này.

**HBase `count`/`scan` báo lỗi UnknownHostException:**
hbase-data có data cũ từ hostname khác. Fix: `docker compose restart zookeeper` → `docker compose restart hbase` → chờ
ready → tạo lại tables (Bước 1).

**FraudDetectionJob log `Processing batch #0` mãi không xong, Redis/HBase không cập nhật:**
Spark checkpoint bị incomplete: `offsets/0` tồn tại nhưng `commits/0` chưa được ghi → batch 0 replay vô hạn. Fix: kill
job → xóa checkpoint → submit lại:

```powershell
docker exec spark-master rm -rf /tmp/spark-checkpoint/fraud-detection
# Submit lại FraudDetectionJob (Bước 3)
```

Xem thêm: section **KHI NÀO XÓA SPARK CHECKPOINT** bên trên.

**`anomaly-events` topic chưa tạo → KafkaSink block batch:**
Nếu FraudDetectionJob log `Processing batch #0` rồi không thấy gì thêm trong >2 phút:

```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --create --topic anomaly-events --partitions 3 --replication-factor 1 --if-not-exists
```

Sau đó kill job → xóa checkpoint → submit lại.
