package linh.vn.cinegoticket.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import linh.vn.cinegoticket.dto.request.HashRequest;
import linh.vn.cinegoticket.dto.request.PaymentRequest;
import linh.vn.cinegoticket.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentSER;

    @PostMapping("/create")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createPayment(Principal principal, @Valid @RequestBody PaymentRequest request,
                                           HttpServletRequest servletRequest) {
//        return ResponseEntity.ok().body(paymentSER.create(principal.getName(), request, "13.160.92.202")); // servletRequest.getRemoteAddr()
        return ResponseEntity.ok().body(paymentSER.create(principal.getName(), request, "127.0.0.1"));// test local dùng ip nay
    }

    @GetMapping("/{id}")
//    @PreAuthorize("hasRole('USER')")//kể cả tắt authen vẫn phải gửi token thì ms nhận đc principal
    public ResponseEntity<?> getPaymentById(Principal principal, @Valid @PathVariable(name = "id") String id) {
        return ResponseEntity.ok().body(paymentSER.getFromId(principal.getName(), id));
    }

    @PostMapping("/{id}/verify")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> verifyPaymentById(Principal principal, @Valid @PathVariable(name = "id") String id) {
        return ResponseEntity.ok().body(paymentSER.verifyPayment(principal.getName(), id));
    }

    @GetMapping("/getall")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getPaymentAllByUsername(Principal principal) {
        return ResponseEntity.ok().body(paymentSER.getAllPaymentsOfUser(principal.getName()));
    }

    @PostMapping("/createhash")
    public ResponseEntity<?> getHash(@Valid @RequestBody HashRequest data) {
        return ResponseEntity.ok().body(paymentSER.createHash(data));
    }
}
