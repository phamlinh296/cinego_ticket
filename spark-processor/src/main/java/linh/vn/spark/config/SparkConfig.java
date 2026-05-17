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
 * <p>
 * fix spark UI: — bỏ .master(master) hardcode, thay bằng check System.getProperty("spark.master").
 * Khi chạy qua spark-submit với --master spark://spark-master:7077, system property đó có giá trị →
 * builder không override → job chạy đúng trên cluster → Spark UI hiện 2 Running Applications.
 */
public class SparkConfig {

    public static SparkSession createSession(String appName, String master) {
        SparkSession.Builder builder = SparkSession.builder()
                .appName(appName)
                .config("spark.sql.shuffle.partitions", "4")
                .config("spark.sql.streaming.kafka.useDeprecatedOffsetFetching", "false");

        // Ưu tiên master từ spark-submit (--master flag → system property "spark.master").
        // Chỉ fallback sang local[*] khi chạy trực tiếp không qua spark-submit (dev/test).
        String submittedMaster = System.getProperty("spark.master");
        if (submittedMaster == null || submittedMaster.isEmpty()) {
            builder.master(master);
        }

        return builder.getOrCreate();
    }
}