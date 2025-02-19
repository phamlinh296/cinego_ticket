package linh.vn.cinegoticket.service;

import linh.vn.cinegoticket.dto.request.HashRequest;
import linh.vn.cinegoticket.dto.request.PaymentRequest;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.PaymentResponse;
import linh.vn.cinegoticket.entity.Payment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface PaymentService {

    public PaymentResponse create(String username, PaymentRequest request, String ip_addr);

    public PaymentResponse getFromId(String username, String payment_id);

    public List<PaymentResponse> getAllPaymentsOfUser(String username);

    public boolean checkPaymentInfo(PaymentRequest request);

    public ApiResponse verifyPayment(String username, String payment_id);

    public String createHash(HashRequest rawdata);

    public void addPaymentMail(Payment payment);

}
