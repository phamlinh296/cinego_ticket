# 🚀 CineGo Ticket - Kafka Payment Flow Testing Guide

## 📋 Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [Quick Start Test](#quick-start-test)
4. [Complete Production Test](#complete-production-test)
5. [Troubleshooting Guide](#troubleshooting-guide)
6. [Common Issues & Solutions](#common-issues--solutions)

---

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌──────────────┐    ┌─────────────────┐
│   Spring Boot   │    │    Kafka     │    │   Spark Jobs    │
│   (cine-be)     │───▶│payment-events│───▶│  Fraud +        │
│   Port: 9595    │    │ Topic: 3     │    │  Analytics      │
└─────────────────┘    └──────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────┐    ┌─────────────────┐
│      Redis      │    │    HBase     │    │   Spark UI      │
│   Cache/Data    │    │   Storage    │    │  Monitoring     │
│   Port: 6379    │    │  Port: 16010 │    │  Port: 8080     │
└─────────────────┘    └──────────────┘    └─────────────────┘
```

### Data Flow

1. **Payment** → Spring Boot publishes to Kafka
2. **Kafka** → Spark jobs consume events
3. **Spark** → Process and store to HBase/Redis
4. **UI** → Display analytics results

---

## 🎯 Prerequisites

### Required Software

- **Docker & Docker Compose**
- **IntelliJ IDEA** (for Spring Boot)
- **Git Bash/PowerShell** (Windows) or Terminal (Linux/Mac)

### Project Structure

```
cine-be/
├── docker-compose.yml         # Infrastructure definition
├── start-infrastructure.bat   # Windows startup script
├── spark-jobs/                # Spark JAR files
└── src/main/java/             # Spring Boot source
```

---

## ⚡ Quick Start Test (5 minutes)

### Step 1: Start Infrastructure

```bash
# Windows
start-infrastructure.bat

# Linux/Mac
./start-infrastructure.sh
```

**🔍 Why?** Starts all required services (Kafka, Redis, HBase, Spark)

### Step 2: Verify Services

```bash
# Check all containers running
docker ps | grep -E "(kafka|redis|hbase|spark)"

# Expected output: 5 containers running
```

### Step 3: Start Spring Boot

- Open IntelliJ IDEA
- Open project `cine-be`
- Run `CinegoTicketApplication.java`
- Wait for: `Started CinegoTicketApplication on port 9595`

### Step 4: Quick Test

```bash
# Test Kafka publish
curl http://localhost:9595/api/payment/test-kafka

# Expected: "Test Kafka event published: test-xxxxxxxx"
```

### Step 5: Check Results

- **Spark UI**: http://localhost:8080
- **HBase UI**: http://localhost:16010

---

## 🔬 Complete Production Test (15 minutes)

### Phase 1: Environment Setup

#### 1.1 Clean Previous State

```bash
# ⚠️ IMPORTANT: Prevents consumer offset conflicts
docker exec spark-master rm -rf /tmp/spark-checkpoint/
docker restart kafka
```

**🔍 Why clean checkpoint?**

- Spark stores consumer offsets in checkpoint files
- Old offsets cause "data loss" errors when topic structure changes
- Cleaning ensures fresh start from offset 0

#### 1.2 Start Infrastructure

```bash
# Start all services
docker-compose up -d kafka redis zookeeper hbase hbase-init spark-master spark-worker

# Wait 30 seconds for services to fully start
sleep 30
```

#### 1.3 Verify Kafka Topic

```bash
# Check topic exists and has correct partitions
docker exec kafka bash -c "cd /opt/kafka && ./bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic payment-events"

# Expected output: 3 partitions
# Topic: payment-events	PartitionCount: 3	ReplicationFactor: 1
```

**🔍 Why 3 partitions?**

- Spark jobs expect 3 partitions for parallel processing
- Matches default Spark shuffle partitions (4)
- Prevents partition mismatch errors

#### 1.4 Fix Topic Partitions (if needed)

```bash
# If only 1 partition, add more
docker exec kafka bash -c "cd /opt/kafka && ./bin/kafka-topics.sh --bootstrap-server localhost:9092 --alter --topic payment-events --partitions 3"
```

### Phase 2: Start Spark Jobs

#### 2.1 Start Fraud Detection Job

```bash
docker exec spark-master /opt/spark/bin/spark-submit \
    --master spark://spark-master:7077 \
    --class linh.vn.spark.job.FraudDetectionJob \
    /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar \
    kafka:9092 hbase 2181 redis 6379
```

**Expected logs:**

```
INFO FraudDetectionJob: Starting FraudDetectionJob
INFO KafkaMicroBatchStream: Initial offsets: {"payment-events":{"2":0,"1":0,"0":0}}
INFO MicroBatchExecution: Committed offsets for batch 0
```

#### 2.2 Start Analytics Job

```bash
docker exec spark-master /opt/spark/bin/spark-submit \
    --master spark://spark-master:7077 \
    --class linh.vn.spark.job.AnalyticsJob \
    /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar \
    kafka:9092 hbase 2181 redis 6379
```

**Expected logs:**

```
INFO AnalyticsJob: Starting AnalyticsJob
INFO MicroBatchExecution: Starting revenue-window-query
INFO MicroBatchExecution: Starting top-movies-query
```

**🔍 Why separate commands?**

- Each job runs in its own JVM
- Prevents resource conflicts
- Allows independent monitoring/restart

#### 2.3 Verify Spark Jobs

- Open: http://localhost:8080
- Check "Completed Applications" should show (2)
- Check "Streaming" tab for active queries

### Phase 3: Start Spring Boot & Test

#### 3.1 Start Spring Boot Application

- Open IntelliJ IDEA
- Navigate to `src/main/java/linh/vn/cinegoticket/CinegoTicketApplication.java`
- Right-click → Run 'CinegoTicketApplication'
- Wait for startup completion

**Expected logs:**

```
INFO CinegoTicketApplication: Started CinegoTicketApplication in xx.xxx seconds
INFO KafkaTestRunner: 🧪 Testing Kafka connection...
INFO PaymentEventPublisher: ✅ Successfully published payment event
```

#### 3.2 Test Payment Flow

**Method 1: API Test**

```bash
# Test Kafka publish endpoint
curl http://localhost:9595/api/payment/test-kafka

# Expected response: "Test Kafka event published: test-xxxxxxxx"
```

**Method 2: Full UI Test**

1. Open browser: http://localhost:9595
2. Login with test credentials
3. Select movie and showtime
4. Complete booking process
5. Make payment (test/sandbox mode)

#### 3.3 Monitor Real-time Processing

**Check Spark Processing:**

```bash
# Monitor Spark job logs
docker logs -f spark-master | grep -E "(payment-events|batch|processed)"
```

**Expected real-time logs:**

```
INFO MicroBatchExecution: Streaming query made progress
INFO MicroBatchExecution: batchId: 1, numInputRows: 1
INFO FraudDetectionJob: Processing payment: test-xxxxxxxx
INFO AnalyticsJob: Revenue updated: 150000.0
```

### Phase 4: Verify Results

#### 4.1 Check Spark UI

- URL: http://localhost:8080
- **Completed Applications**: Should show (2)
- **Streaming Tab**: 3 active queries
- **SQL Tab**: Query execution details

#### 4.2 Check HBase Data

- URL: http://localhost:16010
- **Tables to verify:**
    - `payment_history`: All payment records
    - `fraud_logs`: Fraud detection results
    - `analytics_daily`: Daily statistics

#### 4.3 Check Redis Cache

```bash
# Check cached analytics data
docker exec redis redis-cli KEYS "*analytics*"
docker exec redis redis-cli GET "daily_revenue:$(date +%Y-%m-%d)"
```

---

## 🛠️ Troubleshooting Guide

### Common Startup Issues

#### Issue: Kafka connection timeout

```bash
# Check Kafka container
docker ps | grep kafka

# Check Kafka logs
docker logs kafka | tail -20

# Restart Kafka
docker restart kafka
```

#### Issue: Spark jobs fail to start

```bash
# Check Spark Master
docker logs spark-master | tail -20

# Check Spark Worker
docker logs spark-worker | tail -20

# Restart Spark services
docker restart spark-master spark-worker
```

#### Issue: HBase connection failed

```bash
# Check HBase status
docker exec hbase hbase status

# Check HBase UI
curl http://localhost:16010/table.jsp
```

### Consumer Offset Issues

#### Issue: "incorrect offsets" or "data loss" errors

```bash
# ⚠️ COMPLETE RESET REQUIRED
docker exec spark-master rm -rf /tmp/spark-checkpoint/
docker restart kafka
sleep 10

# Restart Spark jobs (see Phase 2)
```

**🔍 Why this happens:**

- Spark stores last read offsets in checkpoint files
- When topic structure changes (partitions, retention), offsets become invalid
- Reset ensures clean start from current topic state

#### Issue: Topic partition mismatch

```bash
# Check current partitions
docker exec kafka bash -c "cd /opt/kafka && ./bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic payment-events"

# Fix partitions to 3
docker exec kafka bash -c "cd /opt/kafka && ./bin/kafka-topics.sh --bootstrap-server localhost:9092 --alter --topic payment-events --partitions 3"
```

### Performance Issues

#### Issue: Slow processing

```bash
# Check Spark job resource usage
curl http://localhost:8080/api/v1/applications

# Monitor container resources
docker stats
```

#### Issue: High memory usage

```bash
# Increase Spark memory in docker-compose.yml
# Add to spark-master environment:
SPARK_DRIVER_MEMORY: 2g
SPARK_EXECUTOR_MEMORY: 2g
```

---

## 📊 Monitoring & Verification

### Real-time Monitoring Commands

#### Monitor Kafka Events

```bash
# Consume events for debugging
docker exec kafka bash -c "cd /opt/kafka && ./bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic payment-events --from-beginning"
```

#### Monitor Spark Jobs

```bash
# Follow Spark master logs
docker logs -f spark-master

# Check specific job status
curl http://localhost:8080/api/v1/applications | jq '.[].name'
```

#### Monitor HBase Operations

```bash
# HBase shell for manual queries
docker exec -it hbase hbase shell
> list
> scan 'payment_history'
```

### Health Check Script

Create `check-health.sh`:

```bash
#!/bin/bash
echo "🔍 Checking CineGo Infrastructure Health..."

# Check containers
echo "📦 Docker Containers:"
docker ps | grep -E "(kafka|redis|hbase|spark)" | wc -l

# Check Kafka topic
echo "📡 Kafka Topic:"
docker exec kafka bash -c "cd /opt/kafka && ./bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic payment-events" | grep "PartitionCount"

# Check Spark jobs
echo "⚡ Spark Applications:"
curl -s http://localhost:8080/api/v1/applications | jq '. | length'

# Check Spring Boot
echo "🌐 Spring Boot:"
curl -s http://localhost:9595/actuator/health | jq '.status'

echo "✅ Health check completed!"
```

---

## 🚨 Production Deployment Checklist

### Pre-deployment Checks

- [ ] All Docker containers running
- [ ] Kafka topic has 3 partitions
- [ ] HBase tables created
- [ ] Spark jobs start successfully
- [ ] Spring Boot connects to all services

### Post-deployment Verification

- [ ] Payment events published to Kafka
- [ ] Spark jobs consuming events
- [ ] Data stored in HBase
- [ ] Analytics updated in Redis
- [ ] No error logs in any service

### Monitoring Setup

- [ ] Spark UI monitoring active
- [ ] HBase UI accessible
- [ ] Log aggregation configured
- [ ] Alert rules for failures
- [ ] Performance metrics collection

---

## 📝 Quick Reference Commands

### Essential Commands

```bash
# Start all services
start-infrastructure.bat

# Check Spark jobs
curl http://localhost:8080/api/v1/applications

# Reset everything (when issues occur)
docker exec spark-master rm -rf /tmp/spark-checkpoint/
docker restart kafka
docker restart spark-master spark-worker

# Test Kafka publish
curl http://localhost:9595/api/payment/test-kafka

# Monitor logs
docker logs -f spark-master
docker logs -f kafka
```

### URLs

- **Spring Boot**: http://localhost:9595
- **Spark UI**: http://localhost:8080
- **HBase UI**: http://localhost:16010
- **Kafka Test**: http://localhost:9595/api/payment/test-kafka

---

## ⚠️ LỖI ĐÃ GẶP VÀ CÁCH TRÁNH

### Lỗi #1: Kafka Consumer Offset Conflicts

**Error:** `Found incorrect offsets in some partitions` hoặc `failOnDataLoss`

**Nguyên nhân:**

- Spark lưu consumer offsets trong checkpoint files
- Khi Kafka restart hoặc topic structure thay đổi, offsets cũ trở nên invalid
- Spark cố gắng đọc từ offsets không còn tồn tại

**Cách fix:**

```bash
# Xóa checkpoint data hoàn toàn
docker exec spark-master rm -rf /tmp/spark-checkpoint/

# Restart Kafka để reset state
docker restart kafka

# Chạy lại Spark jobs (xem Phase 2)
```

### Lỗi #2: Topic Partition Mismatch

**Error:** `Expected 3 partitions but found 1`

**Nguyên nhân:**

- Spark jobs được code để expect 3 partitions
- Topic mặc định chỉ có 1 partition
- Spark không thể distribute data đúng cách

**Cách fix:**

```bash
# Kiểm tra partitions hiện tại
docker exec kafka bash -c "cd /opt/kafka && ./bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic payment-events"

# Fix partitions thành 3
docker exec kafka bash -c "cd /opt/kafka && ./bin/kafka-topics.sh --bootstrap-server localhost:9092 --alter --topic payment-events --partitions 3"
```

### Lỗi #3: Spark Jobs Exit Code 1

**Error:** Jobs start nhưng immediately exit với exit code 1

**Nguyên nhân:**

- Thường là do Kafka connection hoặc topic issues
- Consumer group conflicts
- HBase connection problems

**Cách fix:**

```bash
# Reset toàn bộ state
docker exec spark-master rm -rf /tmp/spark-checkpoint/
docker restart kafka hbase
sleep 15

# Chạy lại jobs theo đúng thứ tự
```

---

## 🛡️ CHECKLIST TRÁNH LỖI KHI RESTART HỆ THỐNG

### ✅ Bắt buộc phải làm (Mỗi lần restart):

1. **Xóa Spark checkpoint data**
   ```bash
   docker exec spark-master rm -rf /tmp/spark-checkpoint/
   ```

2. **Restart Kafka**
   ```bash
   docker restart kafka
   ```

3. **Kiểm tra topic partitions**
   ```bash
   docker exec kafka bash -c "cd /opt/kafka && ./bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic payment-events"
   # Phải thấy: PartitionCount: 3
   ```

4. **Fix partitions nếu cần**
   ```bash
   docker exec kafka bash -c "cd /opt/kafka && ./bin/kafka-topics.sh --bootstrap-server localhost:9092 --alter --topic payment-events --partitions 3"
   ```

5. **Chạy lại Spark jobs theo thứ tự**
   ```bash
   # 1. Fraud Detection Job trước
   docker exec spark-master /opt/spark/bin/spark-submit --master spark://spark-master:7077 --class linh.vn.spark.job.FraudDetectionJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379

   # 2. Analytics Job sau
   docker exec spark-master /opt/spark/bin/spark-submit --master spark://spark-master:7077 --class linh.vn.spark.job.AnalyticsJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379
   ```

### 🔍 Kiểm tra sau khi start:

- [ ] Spark UI shows "Completed Applications (2)"
- [ ] No "incorrect offsets" errors in logs
- [ ] Topic has exactly 3 partitions
- [ ] Both jobs running without exit code 1

### ⚠️ KHÔNG BAO GIỜ:

- Skip việc xóa checkpoint data
- Start Spark jobs khi Kafka chưa ready
- Bỏ qua việc kiểm tra partitions
- Start cả 2 jobs cùng lúc (chạy từng cái)

---

## 🎯 Success Criteria

### Test Success Indicators

✅ **All 5 containers running** without errors
✅ **Kafka topic with 3 partitions** ready
✅ **2 Spark applications** showing in Completed Apps
✅ **Spring Boot starts** on port 9595
✅ **Payment events published** to Kafka successfully
✅ **Spark jobs consuming** events in real-time
✅ **Data stored** in HBase tables
✅ **No error logs** in any service

### Performance Benchmarks

- **Event processing**: < 1 second latency
- **Spark batch processing**: < 5 seconds
- **HBase write**: < 100ms per record
- **Redis cache**: < 10ms read/write

---

## 🆘 Emergency Recovery

### Complete System Reset

```bash
# Stop everything
docker-compose down

# Clean all data (⚠️ DESTRUCTIVE)
docker system prune -f
docker volume prune -f

# Restart fresh
docker-compose up -d
./setup-kafka-topics.sh
./start-spark-jobs.sh
```

### Partial Recovery

```bash
# Reset only Kafka/Spark state
docker exec spark-master rm -rf /tmp/spark-checkpoint/
docker restart kafka
sleep 10

# Restart only Spark jobs
./start-spark-jobs.sh
```

---

**🎉 Your Kafka Payment Flow is now ready for production testing!**

For issues, check the troubleshooting section or verify all prerequisites are met.
