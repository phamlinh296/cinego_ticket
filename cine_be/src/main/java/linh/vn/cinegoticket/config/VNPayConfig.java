package linh.vn.cinegoticket.config;


import org.springframework.context.annotation.Configuration;

@Configuration
public class VNPayConfig {
    public static final String VNP_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";//URL thanh toán (Sandbox):
    public static final String VNP_VERSION = "2.1.0";
    //    public static final String VNP_TMN_CODE = "064H1LVP";
    public static final String VNP_TMN_CODE = "HX3YAACN";
    //    public static final String VNP_HASH_SECRET = "AEQQSYJOSEUTZRKRSQSLXXVLIASCSNXM";// kiểm tra toàn vẹn dữ liệu khi hai hệ thống trao đổi thông tin (checksum).
    public static final String VNP_HASH_SECRET = "38SCFACWBN2WNAVOF9QGSXD311E22CJU";
    //    public static final String VNP_RETURN_URL = "http://localhost/order-complete";//VNPay không chấp nhận localhost làm vnp_ReturnUrl
    public static final String VNP_RETURN_URL = "https://cinegoticket.com";//vẫn k dc, wi sờ maaaa, ngrok? not found
    public static final String VNP_API_URL = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";//URL truy vấn kết quả giao dịch - hoàn tiền (Sandbox):

}
