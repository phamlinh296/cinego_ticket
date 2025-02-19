package linh.vn.cinegoticket.service.impl;

import linh.vn.cinegoticket.config.VNPayConfig;
import linh.vn.cinegoticket.entity.Payment;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

@Service
public class VNPayService {

    public static String createPay(Payment payment, String bankCode, String ipAddr) throws Exception {
        // Chuyển số tiền thành đơn vị VNPay (nhân 100)
        long amount = Math.round(payment.getAmount() * 100);//số tiền thanh toán, nhân với 100 để chuyển đổi sang đơn vị của VNPay (VND được tính theo đồng, không phải ngàn đồng).
        String txnRef = payment.getId().replace("-", "").substring(0, 16) +
                String.valueOf(System.currentTimeMillis()).substring(9, 13);


        // Khởi tạo danh sách tham số, sử dụng TreeMap để tự động sắp xếp theo thứ tự alphabet
        Map<String, String> vnp_Params = new TreeMap<>();
        vnp_Params.put("vnp_Version", VNPayConfig.VNP_VERSION);
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");

        if (bankCode != null && !bankCode.isEmpty()) {
            vnp_Params.put("vnp_BankCode", bankCode);
        }

        vnp_Params.put("vnp_TxnRef", txnRef);
        vnp_Params.put("vnp_OrderInfo", "Payment for Booking ID " + txnRef);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.VNP_RETURN_URL);
        vnp_Params.put("vnp_IpAddr", ipAddr);

        // Định dạng ngày giờ theo VNPay yêu cầu
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Calendar cld = Calendar.getInstance();
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        cld.add(Calendar.MINUTE, 15); // Hạn thanh toán sau 15 phút
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // Tạo chuỗi dữ liệu để mã hóa chữ ký bảo mật
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (Map.Entry<String, String> param : vnp_Params.entrySet()) {
            hashData.append(param.getKey()).append('=').append(param.getValue()).append('&');
            query.append(URLEncoder.encode(param.getKey(), StandardCharsets.US_ASCII))
                    .append('=')
                    .append(URLEncoder.encode(param.getValue(), StandardCharsets.US_ASCII))
                    .append('&');
        }

        // Loại bỏ ký tự '&' cuối cùng trong hashData
        hashData.setLength(hashData.length() - 1);

        // Tạo chữ ký bảo mật
        String vnp_SecureHash = hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashData.toString());
        query.append("vnp_SecureHash=").append(vnp_SecureHash);

        // Tạo URL thanh toán
        String finalUrl = VNPayConfig.VNP_PAY_URL + "?" + query.toString();
        System.out.println("Generated Payment URL: " + finalUrl);
        System.out.println("vnp_Amount: " + vnp_Params.get("vnp_Amount"));
        System.out.println("vnp_TxnRef: " + vnp_Params.get("vnp_TxnRef"));
        System.out.println("vnp_CreateDate: " + vnp_Params.get("vnp_CreateDate"));
        System.out.println("vnp_ExpireDate: " + vnp_Params.get("vnp_ExpireDate"));
        System.out.println("=== Tham số trước khi tạo chữ ký ===");
        for (Map.Entry<String, String> param : vnp_Params.entrySet()) {
            System.out.println(param.getKey() + " = " + param.getValue());
        }
        System.out.println("Generated SecureHash: " + vnp_SecureHash);

        return finalUrl;
    }

    // Hàm mã hóa HMAC-SHA512
    private static String hmacSHA512(String key, String data) throws Exception {
        Mac sha512Hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        sha512Hmac.init(keySpec);
        byte[] hash = sha512Hmac.doFinal(data.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }


    private String getRandomID(int min, int max) {
        return String.valueOf((int) (Math.random() * (max - min) + min));
    }

    public Integer verifyPay(Payment payment) throws Exception {
        // Gửi request kiểm tra giao dịch:  xây dựng các tham số, tạo chữ ký bảo mật và gửi yêu cầu đến VNPay.
        String response = sendVerifyRequest(payment);

        // kiểm tra phản hồi từ VNPay và xác minh rằng giao dịch có hợp lệ hay không.
        return processVerifyResponse(response, payment);
    }

    private String sendVerifyRequest(Payment payment) throws Exception {
        // Tạo các tham số cần gửi
        String vnp_RequestId = payment.getId() + getRandomID(10000, 99999);
        String vnp_Command = "querydr";
        String vnp_TxnRef = payment.getId();

        String vnp_OrderInfo = "Kiem tra ket qua GD don hang " + vnp_TxnRef;

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(new Date());
        String vnp_TransDate = formatter.format(payment.getCreateAt());
        String vnp_IpAddr = InetAddress.getLocalHost().getHostAddress();

        // Tạo dữ liệu hash
        String hashData = String.join("|", vnp_RequestId, VNPayConfig.VNP_VERSION, vnp_Command,
                VNPayConfig.VNP_TMN_CODE, vnp_TxnRef, vnp_TransDate, vnp_CreateDate, vnp_IpAddr, vnp_OrderInfo);
        String vnp_SecureHash = hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashData);//Tạo chữ ký bảo mật

        // Tạo JSONObject chứa các tham số cần gửi tới VNPay, bao gồm cả chữ ký bảo mật vnp_SecureHash.
        JSONObject vnp_Params = new JSONObject();
        vnp_Params.put("vnp_RequestId", vnp_RequestId);
        vnp_Params.put("vnp_Version", VNPayConfig.VNP_VERSION);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_TransactionDate", vnp_TransDate);
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
        vnp_Params.put("vnp_SecureHash", vnp_SecureHash);

        // Gửi request
        URL url = new URL(VNPayConfig.VNP_API_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.writeBytes(vnp_Params.toString());
            wr.flush();
        }

        // Đọc phản hồi từ VNPay
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {//dọc từng dòng
                response.append(line);
            }
        }
        System.out.println("ress=" + response.toString());
        return response.toString();
    }

    private Integer processVerifyResponse(String response, Payment payment) {
        JSONObject json = new JSONObject(response);
        String res_ResponseCode = json.getString("vnp_ResponseCode");
        String res_TxnRef = json.getString("vnp_TxnRef");
        double res_Amount = json.getDouble("vnp_Amount") / 100;
        String res_TransactionType = json.getString("vnp_TransactionType");
        String res_TransactionStatus = json.getString("vnp_TransactionStatus");

        // Kiểm tra phản hồi từ VNPay
        if (!"00".equals(res_ResponseCode) || !res_TxnRef.equals(payment.getId()) || res_Amount != payment.getAmount()
                || !"01".equals(res_TransactionType) || !"00".equals(res_TransactionStatus)) {
            return 2; // Giao dịch không hợp lệ
        }

        return 0; // Giao dịch thành công
    }


}
