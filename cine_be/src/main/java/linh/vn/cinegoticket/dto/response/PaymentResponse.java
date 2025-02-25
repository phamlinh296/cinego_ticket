package linh.vn.cinegoticket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import linh.vn.cinegoticket.entity.Payment;

@JsonInclude(JsonInclude.Include.NON_NULL) // Bỏ qua null khi serialize
public class PaymentResponse {

    private String id;
    private String email;
    private double price;
    private String createOn;
    private String status;


    private TicketDetail detail;
    private String paymentUrl;

    public PaymentResponse(Payment payment) {
        this.id = payment.getId();
        this.email = payment.getBooking().getUser().getEmail();
        this.price = payment.getAmount();
        this.createOn = payment.getCreateAt().toString();
        this.status = payment.getStatus().name();
        this.detail = new TicketDetail(payment.getBooking());
        // Nếu payment.getBooking() null, tránh lỗi
        System.out.println("check booking trong payres=" + payment.getBooking());
        if (payment.getBooking() != null) {
            this.detail = new TicketDetail(payment.getBooking());
        } else {
            this.detail = new TicketDetail(); // Tránh null
        }
    }

    // Thêm constructor mặc định (Jackson cần khi deserialize)
    public PaymentResponse() {
        this.detail = new TicketDetail(); // Đảm bảo không bao giờ null
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getId() {
        return this.id;
    }

    public String getEmail() {
        return this.email;
    }

    public double getPrice() {
        return this.price;
    }

    public String getCreateOn() {
        return this.createOn;
    }

    public String getStatus() {
        return this.status;
    }

    public TicketDetail getDetail() {
        return this.detail;
    }

    public String getPaymentUrl() {
        return this.paymentUrl;
    }

    @Override
    public String toString() {
        return "PaymentResponse{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", price=" + price +
                ", createOn='" + createOn + '\'' +
                ", status='" + status + '\'' +
                ", detail=" + detail +
                ", paymentUrl='" + paymentUrl + '\'' +
                '}';
    }
}

