package linh.vn.cinegoticket.enums;

public enum DetectionSource {
    /** Phát hiện real-time (<50ms) bởi Spring — rule-based đơn giản */
    SPRING,

    /** Phát hiện deep analysis (30s-1min latency) bởi Spark — window + statistical */
    SPARK
}
