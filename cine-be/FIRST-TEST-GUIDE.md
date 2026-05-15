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

Docker sẽ pull images và khởi động theo đúng thứ tự (`depends_on`):
`zookeeper` → `hbase` → `hbase-init` (tạo tables), `kafka`, `redis`, `spark-master` → `spark-worker`.

Chờ khoảng **40-60 giây** để `hbase-init` tạo xong 3 tables. Kiểm tra:
```powershell
docker ps --format "table {{.Names}}\t{{.Status}}"
```

Expected:
```
NAMES          STATUS
kafka          Up X seconds
redis          Up X seconds
zookeeper      Up X seconds
hbase          Up X seconds
spark-master   Up X seconds
spark-worker   Up X seconds
hbase-init     Exited (0)     ← bình thường, đã tạo xong tables
```

Xác nhận 3 tables HBase đã được tạo:
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

Nếu chưa thấy 3 tables (hbase-init chưa xong): chờ thêm 30 giây rồi chạy lại lệnh trên.

Fix hostname HBase để lệnh `count` hoạt động:
```powershell
docker exec hbase bash -c "echo ""127.0.0.1 $(docker exec hbase hostname)"" >> /etc/hosts"
```

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

### payment_history — FraudDetectionJob ghi sau mỗi event
```powershell
docker exec hbase bash -c "echo 'count ""payment_history""' | /opt/hbase/bin/hbase shell -n 2>/dev/null"
```

Expected sau khi gửi 5 events:
```
5 row(s)
```

### analytics_daily — AnalyticsJob ghi tổng hợp theo ngày/movie
```powershell
docker exec hbase bash -c "echo 'count ""analytics_daily""' | /opt/hbase/bin/hbase shell -n 2>/dev/null"
```

Expected: số row = số cặp `(date, movieId)` độc nhất (với 5 events cùng movieId=123 trong ngày → 1 row).

### fraud_logs — FraudDetectionJob ghi khi phát hiện fraud
```powershell
docker exec hbase bash -c "echo 'count ""fraud_logs""' | /opt/hbase/bin/hbase shell -n 2>/dev/null"
```

Expected khi test bình thường: `0 row(s)`.

Nếu lệnh count báo lỗi `UnknownHostException`: chạy lại lệnh fix hostname ở cuối Bước 1 rồi thử lại.

---

## BƯỚC 8 — Kiểm tra Spark UI

Mở browser: `http://localhost:8080`

Expected:
- **Running Applications**: 2 (FraudDetectionJob + AnalyticsJob)
- Click vào từng app: thấy Streaming Queries đang active

HBase UI: `http://localhost:16010` — thấy 3 tables trong Tables tab.

---

## CHECKLIST XÁC NHẬN THÀNH CÔNG

- [ ] `docker ps` thấy đủ 6 container Up
- [ ] `list` trong HBase shell thấy 3 tables
- [ ] Spring Boot log `Started CinegoTicketApplication on port 9595`
- [ ] FraudDetectionJob log `idle and waiting for new data`
- [ ] AnalyticsJob log `3 queries still running`
- [ ] `curl test-kafka` trả về `Test Kafka event published`
- [ ] Redis `spark:user_stats:1` có `tx_count` = số event đã gửi
- [ ] Redis `spark:top_movies:{ngày hôm nay}` có movieId 123
- [ ] HBase `payment_history` count = số event đã gửi
- [ ] Spark UI `http://localhost:8080` thấy 2 Running Applications

---

## XỬ LÝ SỰ CỐ THƯỜNG GẶP

**HBase tables chưa có sau khi `docker compose up -d`:**
`hbase-init` cần 30-60 giây để chạy xong. Kiểm tra log: `docker logs hbase-init`. Nếu bị lỗi thì chạy lại: `docker compose up hbase-init`.

**Spring Boot không kết nối được Kafka:**
Kafka khởi động mất ~15 giây. Nếu Spring Boot start trước Kafka thì sẽ retry tự động. Chờ thêm, không cần restart.

**Spark job báo lỗi JAR không tìm thấy:**
Đường dẫn JAR trong container là `/opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar`. Kiểm tra: `docker exec spark-master ls /opt/spark-jobs/`. JAR được mount qua volume `./spark-jobs` trong `docker-compose.yml`.

**`count` HBase báo `UnknownHostException`:**
Chạy fix hostname: `docker exec hbase bash -c "echo ""127.0.0.1 $(docker exec hbase hostname)"" >> /etc/hosts"` rồi thử lại.
