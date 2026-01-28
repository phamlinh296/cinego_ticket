package linh.vn.cinegoticket.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import linh.vn.cinegoticket.config.VNPayConfig;
import linh.vn.cinegoticket.entity.Payment;
import linh.vn.cinegoticket.repository.PaymentRepository;
import linh.vn.cinegoticket.utils.PaymentUtil;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.*;

@Service
public class VNPayService {
    @Autowired
    private PaymentRepository paymentRepository;

    public String createPay(Payment payment, String bankCode, String vnp_IpAddr,  HttpServletRequest request) throws Exception {
        // Chuyển số tiền thành đơn vị VNPay (nhân 100)
        long amount = Math.round(payment.getAmount() * 100);//số tiền thanh toán, nhân với 100 để chuyển đổi sang đơn vị của VNPay (VND được tính theo đồng, không phải ngàn đồng).
        String vnp_TxnRef = payment.getId().replace("-", "").substring(0, 16) +
                String.valueOf(System.currentTimeMillis()).substring(9, 13);

        payment.setTxnRef(vnp_TxnRef);
        paymentRepository.save(payment);

        String vnp_Version = VNPayConfig.VNP_VERSION;
        String vnp_Command = "pay";
        String vnp_OrderInfo = request.getParameter("vnp_OrderInfo");
        String orderType = request.getParameter("ordertype");
        String vnp_TmnCode = VNPayConfig.VNP_TMN_CODE;


        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        String bank_code = request.getParameter("bankcode");
        if (bank_code != null && !bank_code.isEmpty()) {
            vnp_Params.put("vnp_BankCode", bank_code);
        }
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        if (vnp_OrderInfo == null || vnp_OrderInfo.isEmpty()) {
            vnp_OrderInfo = "Thanh toan don hang: " + vnp_TxnRef;
        }
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        if (orderType == null || orderType.isEmpty()) {
            orderType = "other";
        }
        vnp_Params.put("vnp_OrderType", orderType);

        String locate = request.getParameter("language");
        if (locate != null && !locate.isEmpty()) {
            vnp_Params.put("vnp_Locale", locate);
        } else {
            vnp_Params.put("vnp_Locale", "vn");
        }
        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.VNP_RETURN_URL);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        //Billing
        vnp_Params.put("vnp_Bill_Mobile", request.getParameter("txt_billing_mobile"));
        vnp_Params.put("vnp_Bill_Email", request.getParameter("txt_billing_email"));
        String fullName = (request.getParameter("txt_billing_fullname"));
        if (fullName != null && !fullName.isEmpty()) {
            fullName = fullName.trim();
            int idx = fullName.indexOf(' ');
            String firstName = fullName.substring(0, idx);
            String lastName = fullName.substring(fullName.lastIndexOf(' ') + 1);
            vnp_Params.put("vnp_Bill_FirstName", firstName);
            vnp_Params.put("vnp_Bill_LastName", lastName);
        }

        vnp_Params.put("vnp_Bill_Address", request.getParameter("txt_inv_addr1"));
        vnp_Params.put("vnp_Bill_City", request.getParameter("txt_bill_city"));
        vnp_Params.put("vnp_Bill_Country", request.getParameter("txt_bill_country"));
        if (request.getParameter("txt_bill_state") != null && !request.getParameter("txt_bill_state").isEmpty()) {
            vnp_Params.put("vnp_Bill_State", request.getParameter("txt_bill_state"));
        }

        // Invoice
        vnp_Params.put("vnp_Inv_Phone", request.getParameter("txt_inv_mobile"));
        vnp_Params.put("vnp_Inv_Email", request.getParameter("txt_inv_email"));
        vnp_Params.put("vnp_Inv_Customer", request.getParameter("txt_inv_customer"));
        vnp_Params.put("vnp_Inv_Address", request.getParameter("txt_inv_addr1"));
        vnp_Params.put("vnp_Inv_Company", request.getParameter("txt_inv_company"));
        vnp_Params.put("vnp_Inv_Taxcode", request.getParameter("txt_inv_taxcode"));
        vnp_Params.put("vnp_Inv_Type", request.getParameter("cbo_inv_type"));

        //Build data to hash and query string
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {                //Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                //Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = PaymentUtil.hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VNPayConfig.VNP_PAY_URL + "?" + queryUrl;

        return paymentUrl;
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
        String vnp_TxnRef = payment.getTxnRef();

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
        if (!"00".equals(res_ResponseCode) || !res_TxnRef.equals(payment.getTxnRef()) || res_Amount != payment.getAmount()
                || !"01".equals(res_TransactionType) || !"00".equals(res_TransactionStatus)) {
            return 2; // Giao dịch không hợp lệ
        }

        return 0; // Giao dịch thành công
    }


}
