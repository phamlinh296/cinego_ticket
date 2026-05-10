#!/bin/bash

echo "🚀 Starting CineGo Ticket Infrastructure..."

# 1. Start all containers
echo "📦 Starting Docker containers..."
docker-compose up -d

# Wait for services to be ready
echo "⏳ Waiting for services to start..."
sleep 30

# 2. Create Kafka topics
echo "📡 Creating Kafka topics..."
docker exec -it kafka bash -c "
/opt/kafka/bin/kafka-topics.sh --create --bootstrap-server kafka:9092 --replication-factor 1 --partitions 1 --topic payment-events --if-not-exists
/opt/kafka/bin/kafka-topics.sh --create --bootstrap-server kafka:9092 --replication-factor 1 --partitions 1 --topic fraud-alerts --if-not-exists
/opt/kafka/bin/kafka-topics.sh --create --bootstrap-server kafka:9092 --replication-factor 1 --partitions 1 --topic analytics-results --if-not-exists
"

# 3. Wait for Spark Master to be ready
echo "⏳ Waiting for Spark Master..."
sleep 20

# 4. Start Fraud Detection Job
echo "🔥 Starting Fraud Detection Job..."
docker exec -d spark-master bash -c "
export PATH=\$PATH:/opt/spark/bin
spark-submit --master spark://spark-master:7077 --class linh.vn.spark.job.FraudDetectionJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379
"

# 5. Start Analytics Job
echo "📊 Starting Analytics Job..."
docker exec -d spark-master bash -c "
export PATH=\$PATH:/opt/spark/bin
spark-submit --master spark://spark-master:7077 --class linh.vn.spark.job.AnalyticsJob /opt/spark-jobs/spark-processor-0.0.1-SNAPSHOT-shaded.jar kafka:9092 hbase 2181 redis 6379
"

echo "✅ Infrastructure started successfully!"
echo "📊 Spark UI: http://localhost:8080"
echo "🔥 HBase UI: http://localhost:16010"
echo "📡 Kafka UI: http://localhost:29092"
echo ""
echo "🚀 Now you can start the Spring Boot application in IntelliJ IDEA"
