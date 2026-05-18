package linh.vn.cinegoticket.enums;

public enum AnomalyType {

    // ── Spring-side: phát hiện real-time (<50ms), dùng Redis Z-Score 20 records gần nhất ──

    /** ≥3 giao dịch trong cùng 30 giây từ cùng 1 user */
    MULTIPLE_FAST_TRANSACTION,

    /** Giao dịch FAILED nhưng payment gateway trả returnCode = "0" (thanh toán thực tế thành công) */
    FAILED_BUT_CHARGED,

    /** IP/device thay đổi so với lần giao dịch trước của cùng user */
    DEVICE_CHANGE,

    /** Số tiền vượt ngưỡng cố định (≥1.000.000 VND) */
    HIGH_AMOUNT,

    /** @deprecated Không dùng nữa trong detection — giữ lại để đọc được records cũ trong DB */
    @Deprecated
    AMOUNT_ZSCORE,

    // ── Spark-side: phát hiện deep analysis (latency 30s-1min), window 1h, HBase Z-Score 500 records ──

    /** Smurfing (rửa tiền phân mảnh): ≥3 tx nhỏ (≤200K) + ≥1 tx lớn (≥1M), tx lớn ≥ 10× trung bình tx nhỏ */
    SMURFING,

    /** Z-Score deep: |Z| > 3.0 so với 500 records lịch sử trong HBase (30 ngày) */
    ZSCORE_DEEP,

    /** Tần suất cao: ≥10 giao dịch trong cùng 1 giờ (sliding window 5 phút) */
    HIGH_FREQUENCY_WINDOW
}
