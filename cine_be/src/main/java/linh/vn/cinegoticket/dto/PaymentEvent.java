package linh.vn.cinegoticket.dto;

import lombok.Data;

import java.util.Date;

@Data
public class PaymentEvent {
    private String paymentId;
    private String userId;
    private double amount;
    private String deviceIp;
    private Date time;
    private String status;
    private String location;

    private String returnCode; // optional, VNPay return code if available
}
