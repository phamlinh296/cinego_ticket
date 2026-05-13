package linh.vn.cinegoticket.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import linh.vn.cinegoticket.dto.request.HashRequest;
import linh.vn.cinegoticket.dto.request.PaymentRequest;
import linh.vn.cinegoticket.dto.response.PaymentResponse;
import linh.vn.cinegoticket.entity.Payment;
import linh.vn.cinegoticket.kafka.PaymentEventPublisher;
import linh.vn.cinegoticket.kafka.event.PaymentEvent;
import linh.vn.cinegoticket.service.EmailService;
import linh.vn.cinegoticket.service.PaymentService;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentEventPublisher paymentEventPublisher;

    @PostMapping("/create")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createPayment(Principal principal, @Valid @RequestBody PaymentRequest request,
                                           HttpServletRequest servletRequest) {
        PaymentResponse paymentResponse = paymentService.createPayment(principal.getName(), request, "127.0.0.1", servletRequest); // test local dùng ip nay
        return ResponseEntity.ok().body(paymentResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")//kể cả tắt authen vẫn phải gửi token thì ms nhận đc principal
    public ResponseEntity<?> getPaymentById(Principal principal, @Valid @PathVariable(name = "id") String id) {
        return ResponseEntity.ok().body(paymentService.getFromId(principal.getName(), id));
    }

    @PostMapping("/{id}/verify")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> verifyPaymentById(Principal principal, @Valid @PathVariable(name = "id") String id) {
        return ResponseEntity.ok().body(paymentService.verifyPayment(principal.getName(), id));
    }

    @GetMapping("/getall")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getPaymentAllByUsername(Principal principal) {
        return ResponseEntity.ok().body(paymentService.getAllPaymentsOfUser(principal.getName()));
    }

    @PostMapping("/createhash")
    public ResponseEntity<?> getHash(@Valid @RequestBody HashRequest data) {
        return ResponseEntity.ok().body(paymentService.createHash(data));
    }

    //TEST
    //1. test send gửi mail tự động bằng reids khi booking gọi đến payment có thành công k (test cả redis)
    @PostMapping("/send")
    public ResponseEntity<String> testSendPaymentMail(@RequestBody Payment payment) {
        System.out.println("PaymentController test: Received Payment: " + payment);
        paymentService.addPaymentMail(payment);
        return ResponseEntity.ok("Payment message sent to Redis!");
    }

    //2. test gửi mỗi mail
    private final EmailService emailService;

    @Autowired
    public PaymentController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/sendmail")
    public ResponseEntity<String> sendTestEmail(@RequestParam String to) {
        emailService.sendMail(to, "Test Email", "Hello! This is a test email from Spring Boot.");
        return ResponseEntity.ok("PaymentController test: Email sent to " + to);
    }

    // Test Kafka publish
    @GetMapping("/test-kafka")
    public ResponseEntity<String> testKafkaPublish() {
        PaymentEvent testEvent = new PaymentEvent();
        testEvent.setPaymentId("test-" + System.currentTimeMillis());
        testEvent.setUserId("1");
        testEvent.setMovieId("123");
        testEvent.setAmount(150000.0);
        testEvent.setStatus("PAID");
        testEvent.setDeviceIp("127.0.0.1");
        testEvent.setTime(Instant.now());
        testEvent.setLocation("VN");
        
        paymentEventPublisher.publish(testEvent);
        return ResponseEntity.ok("Test Kafka event published: " + testEvent.getPaymentId());
    }

    // Test Kafka publish with custom body
    @PostMapping("/test-kafka")
    public ResponseEntity<String> testKafkaPublishWithBody(@RequestBody PaymentEvent paymentEvent) {
        if (paymentEvent.getPaymentId() == null || paymentEvent.getPaymentId().isEmpty()) {
            paymentEvent.setPaymentId("test-" + System.currentTimeMillis());
        }
        if (paymentEvent.getTime() == null) {
            paymentEvent.setTime(Instant.now());
        }
        
        paymentEventPublisher.publish(paymentEvent);
        return ResponseEntity.ok("Test Kafka event published: " + paymentEvent.getPaymentId());
    }


    /////////
    @GetMapping("/vnpay/return")
    public String vnpayReturn(HttpServletRequest request) {
        return "Thanh toan xong";
    }

}
