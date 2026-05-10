@echo off
echo 🚀 Starting CineGo Ticket Infrastructure...

REM 1. Check if containers already exist
echo 🔍 Checking existing containers...
docker ps -a --format "table {{.Names}}" | findstr /C:"cine_db\|redis\|kafka\|zookeeper\|hbase\|spark-master\|spark-worker" >nul
if %ERRORLEVEL% EQU 0 (
    echo ✅ Found existing containers, starting them...
    docker-compose start
) else (
    echo 📦 No containers found, creating new ones...
    docker-compose up -d
)

REM Wait for services to be ready
echo ⏳ Waiting for services to start...
timeout /t 30 /nobreak > nul

REM 2. Create Kafka topics
echo 📡 Creating Kafka topics...
docker exec -it kafka bash -c "/opt/kafka/bin/kafka-topics.sh --create --bootstrap-server kafka:9092 --replication-factor 1 --partitions 1 --topic payment-events --if-not-exists"
docker exec -it kafka bash -c "/opt/kafka/bin/kafka-topics.sh --create --bootstrap-server kafka:9092 --replication-factor 1 --partitions 1 --topic fraud-alerts --if-not-exists"
docker exec -it kafka bash -c "/opt/kafka/bin/kafka-topics.sh --create --bootstrap-server kafka:9092 --replication-factor 1 --partitions 1 --topic analytics-results --if-not-exists"

REM 3. Wait for Spark Master to be ready
echo ⏳ Waiting for Spark Master...
timeout /t 20 /nobreak > nul

REM 4. Start Fraud Detection Job
echo 🔥 Starting Fraud Detection Job...
docker exec -d spark-master bash -c "export PATH=$PATH:/opt/spark/bin && spark-submit --master spark://spark-master:7077 --class linh.vn.spark.job.FraudDetectionJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379"

REM 5. Start Analytics Job
echo 📊 Starting Analytics Job...
docker exec -d spark-master bash -c "export PATH=$PATH:/opt/spark/bin && spark-submit --master spark://spark-master:7077 --class linh.vn.spark.job.AnalyticsJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379"

echo ✅ Infrastructure started successfully!
echo 📊 Spark UI: http://localhost:8080
echo 🔥 HBase UI: http://localhost:16010
echo 📡 Kafka: localhost:29092
echo.
echo 🚀 Now you can start the Spring Boot application in IntelliJ IDEA
pause
