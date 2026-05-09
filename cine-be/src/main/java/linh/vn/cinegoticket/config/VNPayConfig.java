package linh.vn.cinegoticket.config;


import org.springframework.context.annotation.Configuration;

@Configuration
public class VNPayConfig {
    public static final String VNP_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static final String VNP_VERSION = "2.1.0";

    // LÂU LÂU VNP_TMN_CODE VÀ VNP_HASH_SECRET THAY ĐỔI NÊN phải lấy gtri ms vs mail ms, ở https://sandbox.vnpayment.vn/devreg/
    public static final String VNP_TMN_CODE = "S3DBAQZS";
//    public static final String VNP_TMN_CODE = "YUN58R69";

    public static final String VNP_HASH_SECRET = "52CDU26JDLA50TB16M00LCD2C2I3OCR4";
//    public static final String VNP_HASH_SECRET = "LRXBRHNGWCRX2T72AQ4MWMQ5PRG6D30R";


    //    public static final String VNP_RETURN_URL = "http://localhost/order-complete";
    public static final String VNP_RETURN_URL = "http://127.0.0.1:5500/order-complete.html";
//    public static final String VNP_RETURN_URL = "http://localhost:9595/api/payment/vnpay/return";
//    public static final String VNP_RETURN_URL = "https://stubbled-embryologically-cristiano.ngrok-free.dev/api/payment/vnpay/return";

    public static final String VNP_API_URL = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

}
