package linh.vn.spark.config;

import org.apache.spark.sql.SparkSession;

/**
 * Factory để tạo SparkSession.
 * <p>
 * SparkSession là entry point cho Structured Streaming.
 * Tương tự ApplicationContext của Spring — 1 instance duy nhất per JVM.
 * <p>
 * Interviewer question: "local[*] nghĩa là gì?"
 * → Chạy Spark locally, dùng tất cả CPU cores của máy.
 * local[2] = 2 threads. Khi deploy lên cluster, đổi thành spark://spark-master:7077
 */
public class SparkConfig {

    public static SparkSession createSession(String appName, String master) {
        return SparkSession.builder()
                .appName(appName)
                .master(master)
                // Kafka source cần checkpoint để đảm bảo exactly-once semantics
                .config("spark.sql.streaming.checkpointLocation", "/tmp/spark-checkpoint/" + appName)
                // Tắt Spark UI khi chạy trong Docker (tránh port conflict)
                // .config("spark.ui.enabled", "false")
                // Tối ưu cho streaming: shuffle nhỏ hơn (default 200 partition quá nhiều)
                .config("spark.sql.shuffle.partitions", "4")
                // Cho phép Spark đọc từ Kafka với multiple topic
                .config("spark.sql.streaming.kafka.useDeprecatedOffsetFetching", "false")
                .getOrCreate();
    }
}