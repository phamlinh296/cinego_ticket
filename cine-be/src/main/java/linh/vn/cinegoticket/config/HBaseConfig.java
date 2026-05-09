package linh.vn.cinegoticket.config;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * HBase Connection Bean.
 * <p>
 * Connection là thread-safe và nên được tái sử dụng (không tạo mới mỗi request).
 * Tương tự như DataSource của JDBC.
 * <p>
 * Interviewer question: "Tại sao dùng một Connection duy nhất?"
 * → Connection = pool of connections to RegionServers, rất tốn kém để tạo mới,
 * nên share 1 instance toàn app (singleton bean).
 */

//Spark + Spring Boot = dễ conflict (classpath hell) nen tách riêng module spark-processor để tránh phụ thuộc HBase client trong cine-be.
@Configuration
public class HBaseConfig {

    @Value("${hbase.zookeeper.quorum:localhost}")
    private String zookeeperQuorum;

    @Value("${hbase.zookeeper.port:2181}")
    private String zookeeperPort;

    @Bean(destroyMethod = "close")
    public Connection hbaseConnection() throws IOException {
        org.apache.hadoop.conf.Configuration config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", zookeeperQuorum);
        config.set("hbase.zookeeper.property.clientPort", zookeeperPort);
        // Timeout settings để tránh hang khi HBase chưa sẵn sàng
        config.set("hbase.rpc.timeout", "5000");
        config.set("hbase.client.operation.timeout", "10000");
        config.set("hbase.client.scanner.timeout.period", "10000");
        return ConnectionFactory.createConnection(config);
    }
}