package linh.vn.cinegoticket.config;


import org.springframework.context.annotation.Configuration;

@Configuration
public class VNPayConfig {
    public static final String VNP_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static final String VNP_VERSION = "2.1.0";
    //    public static final String VNP_TMN_CODE = "064H1LVP";
    public static final String VNP_TMN_CODE = "HX3YAACN";
    //    public static final String VNP_HASH_SECRET = "AEQQSYJOSEUTZRKRSQSLXXVLIASCSNXM";
    public static final String VNP_HASH_SECRET = "38SCFACWBN2WNAVOF9QGSXD311E22CJU";
    //    public static final String VNP_RETURN_URL = "http://localhost/order-complete";
    public static final String VNP_RETURN_URL = "https://cinegoticket.com";
    public static final String VNP_API_URL = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

}
