# FIRST TEST GUIDE — Lần Đầu Tải Dự Án Về Chạy

Dùng guide này khi: vừa clone/tải project, chưa có container nào, chưa có data gì.
Nếu đã từng chạy và muốn restart: xem `COMPLETE-TEST-GUIDE.md`.

---

## YÊU CẦU

- Docker Desktop đang chạy
- Java 21 đã cài (cho Spring Boot)
- Maven wrapper `./mvnw` có trong thư mục root

---

## BƯỚC 1 — Khởi động Infrastructure (lần đầu)

Đứng trong thư mục `cine-be`:
```powershell
cd cine-be
docker compose up -d
```

Docker sẽ pull images và khởi động 6 container theo thứ tự `depends_on`. Kiểm tra:
```powershell
docker ps --format "table {{.Names}}\t{{.Status}}"
```

Expected — phải thấy đủ 6 container:
```
NAMES          STATUS
kafka          Up X seconds
redis          Up X seconds
zookeeper      Up X seconds
hbase          Up X seconds
spark-master   Up X seconds
spark-worker   Up X seconds
```

Chờ HBase sẵn sàng (~30s) — chạy lại cho đến khi thấy `1 servers, 0 dead`:
```powershell
docker exec hbase bash -c "echo 'status' | /opt/hbase/bin/hbase shell -n 2>/dev/null"
```

Expected: `1 active master, 0 backup masters, 1 servers, 0 dead, 2.0000 average load`

Tạo 3 HBase tables thủ công (HBase không tự tạo):
```powershell
docker exec hbase bash -c "echo \"create 'payment_history', 'cf'\" | /opt/hbase/bin/hbase shell -n 2>/dev/null"
docker exec hbase bash -c "echo \"create 'fraud_logs', 'cf'\" | /opt/hbase/bin/hbase shell -n 2>/dev/null"
docker exec hbase bash -c "echo \"create 'analytics_daily', 'cf'\" | /opt/hbase/bin/hbase shell -n 2>/dev/null"
```

Expected mỗi lệnh: `Created table X`

Xác nhận 3 tables đã tạo:
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

Pre-create Kafka topic `anomaly-events` (FraudDetectionJob publish fraud alerts vào đây — **bắt buộc** trước khi submit job):
```powershell
docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --create --topic anomaly-events --partitions 3 --replication-factor 1 --if-not-exists
```

Expected: `Created topic anomaly-events.`

---

## BƯỚC 2 — Chạy Backend Spring Boot

Mở terminal riêng, đứng ở thư mục **root** của project (không phải `cine-be`):
```powershell
./mvnw spring-boot:run -pl cine-be
```

Lần đầu Maven sẽ download dependencies — mất 2-5 phút. Các lần sau nhanh hơn.

Chờ log:
```
Started CinegoTicketApplication on port 9595
```

Verify:
```powershell
curl http://localhost:9595/actuator/health
```
Expected: `{"status":"UP"}`

Spring Boot lúc start sẽ tự chạy `db/data.sql` để seed phim và user test vào MySQL (nếu MySQL chưa có data).

---

## BƯỚC 3 — Chạy FraudDetectionJob

Mở terminal riêng:
```powershell
docker exec spark-master /opt/spark/bin/spark-submit --deploy-mode client --master spark://spark-master:7077 --class linh.vn.spark.job.FraudDetectionJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379
```

Chờ log (mất ~30 giây để Spark khởi động):
```
INFO FraudDetectionJob: Starting FraudDetectionJob | kafka=kafka:9092 hbase=hbase:2181 redis=redis:6379
INFO MicroBatchExecution: Streaming query made progress ...
INFO MicroBatchExecution: Streaming query has been idle and waiting for new data more than 10000 ms.
```

Log `idle` = bình thường, job đang chờ event từ Kafka.

---

## BƯỚC 4 — Chạy AnalyticsJob

Mở terminal riêng:
```powershell
docker exec spark-master /opt/spark/bin/spark-submit --deploy-mode client --master spark://spark-master:7077 --class linh.vn.spark.job.AnalyticsJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379
```

Chờ log:
```
INFO AnalyticsJob: Starting AnalyticsJob | kafka=kafka:9092 ...
INFO AnalyticsJob: [AnalyticsJob] 3 queries still running...
INFO MicroBatchExecution: Streaming query has been idle and waiting for new data more than 10000 ms.
```

`3 queries still running` = 3 streaming queries đang chạy song song (revenue, top_movies, user_stats) — đúng, không phải lỗi.

---

## BƯỚC 5 — Gửi Test Event

```powershell
curl http://localhost:9595/api/payment/test-kafka
```

Expected response:
```
Test Kafka event published: test-1747282000000
```

Gửi 5 events để thấy kết quả rõ:
```powershell
for ($i=1; $i -le 5; $i++) { curl http://localhost:9595/api/payment/test-kafka; Start-Sleep 1 }
```

Log trong Spring Boot:
```
Publishing event test-1747282000000
Successfully published payment event test-1747282000000 to partition 0 offset 0
```

Chờ 10-15 giây để Spark xử lý xong micro-batch trước khi check kết quả.

---

## BƯỚC 6 — Kiểm tra Redis

### All keys
```powershell
docker exec redis redis-cli KEYS "spark:*"
```

Expected sau khi gửi 5 events:
```
spark:user_stats:1
spark:top_movies:2026-05-15
spark:revenue:2026-05-15:03
```

### User stats (AnalyticsJob)
```powershell
docker exec redis redis-cli HGETALL "spark:user_stats:1"
```

Expected:
```
tx_count
5
total_amount
750000
avg_amount
150000.0
```

### Top movies (AnalyticsJob)
```powershell
docker exec redis redis-cli ZREVRANGE "spark:top_movies:2026-05-15" 0 -1 WITHSCORES
```

Expected (thay 2026-05-15 bằng ngày hôm nay):
```
123
750000
```

### Revenue theo giờ (AnalyticsJob)
```powershell
docker exec redis redis-cli KEYS "spark:revenue:*"
```

Expected: thấy key dạng `spark:revenue:2026-05-15:03`.

### Fraud count (FraudDetectionJob)
```powershell
docker exec redis redis-cli KEYS "spark:fraud_count:*"
```

Expected khi gửi 5 test event bình thường: không có key nào (chưa phát hiện fraud).

---

## BƯỚC 7 — Kiểm tra HBase

### Tables
```powershell
docker exec hbase bash -c "echo 'list' | /opt/hbase/bin/hbase shell -n 2>/dev/null"
```

Expected: thấy `analytics_daily`, `fraud_logs`, `payment_history`.

### payment_history — FraudDetectionJob ghi trước pattern detection
```powershell
'count "payment_history"' | docker exec -i hbase /opt/hbase/bin/hbase shell
```

Expected sau khi gửi 5 events: số row > 0 và tăng dần theo số event gửi.

### analytics_daily — AnalyticsJob ghi tổng hợp theo ngày/movie
```powershell
'count "analytics_daily"' | docker exec -i hbase /opt/hbase/bin/hbase shell
```

Expected: số row = số cặp `(date, movieId)` độc nhất (với 5 events cùng movieId=123 trong ngày → 2 row: `{date}___ALL__` + `{date}_123`).

### fraud_logs — FraudDetectionJob ghi khi phát hiện fraud
```powershell
'count "fraud_logs"' | docker exec -i hbase /opt/hbase/bin/hbase shell
```

Expected khi gửi test event bình thường: `0 row(s)`. Tăng lên khi gửi 10+ events nhanh liên tiếp.

Nếu count báo `UnknownHostException`: restart zookeeper + hbase rồi tạo lại tables (xem Bước 1).

---

## BƯỚC 8 — Kiểm tra Spark UI

Mở browser: `http://localhost:8080`

Expected:
- **Running Applications**: 2 (FraudDetectionJob + AnalyticsJob)
- Click vào từng app: thấy Streaming Queries đang active

HBase UI: `http://localhost:16010` — thấy 3 tables trong Tables tab.

---

## CHECKLIST XÁC NHẬN THÀNH CÔNG

- [ ] `docker ps` thấy đủ 6 container Up (kafka, redis, zookeeper, hbase, spark-master, spark-worker)
- [ ] HBase shell `list` thấy 3 tables (analytics_daily, fraud_logs, payment_history)
- [ ] Kafka topic `anomaly-events` đã được tạo
- [ ] Spring Boot log `Started CinegoTicketApplication on port 9595`
- [ ] FraudDetectionJob log `idle and waiting for new data`
- [ ] AnalyticsJob log `3 queries still running`
- [ ] `curl test-kafka` trả về `Test Kafka event published`
- [ ] Redis `spark:user_stats:1` có `tx_count` = số event đã gửi
- [ ] Redis `spark:top_movies:{ngày hôm nay}` có movieId 123
- [ ] HBase `payment_history` count > 0 sau khi gửi events
- [ ] Spark UI `http://localhost:8080` thấy 2 Running Applications

---

## XỬ LÝ SỰ CỐ THƯỜNG GẶP

**HBase tables chưa có sau khi `docker compose up -d`:**
Chờ HBase ready (~30 giây) rồi tạo thủ công (xem lệnh Bước 1). Kiểm tra HBase sẵn sàng: `docker exec hbase bash -c "echo 'status' | /opt/hbase/bin/hbase shell -n 2>/dev/null"`. Phải thấy `1 servers, 0 dead`.

**HBase bị stuck — log "Master startup cannot progress":**
ZooKeeper giữ stale data. Fix: `docker compose restart zookeeper` → `docker compose restart hbase` → chờ ~30s → tạo lại tables.

**Spring Boot không kết nối được Kafka:**
Kafka khởi động mất ~15 giây. Nếu Spring Boot start trước Kafka thì sẽ retry tự động. Chờ thêm, không cần restart.

**Spark job báo lỗi JAR không tìm thấy:**
Đường dẫn JAR trong container là `/opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar`. Kiểm tra: `docker exec spark-master ls /opt/spark-jobs/`. JAR được mount qua volume `./spark-jobs` trong `docker-compose.yml`.

**`count` HBase báo `UnknownHostException`:**
hbase-data có data cũ từ hostname khác. Fix: `docker compose restart zookeeper` → `docker compose restart hbase` → chờ ready → tạo lại tables (Bước 1).

**FraudDetectionJob không xử lý, Redis/HBase không cập nhật:**
Có thể thiếu topic `anomaly-events` → KafkaSink bị block. Chạy lại lệnh tạo topic ở cuối Bước 1 với `--if-not-exists`.
