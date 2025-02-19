package linh.vn.cinegoticket.service.impl;

import linh.vn.cinegoticket.dto.request.HashRequest;
import linh.vn.cinegoticket.dto.request.PaymentRequest;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.PaymentResponse;
import linh.vn.cinegoticket.entity.Booking;
import linh.vn.cinegoticket.entity.Payment;
import linh.vn.cinegoticket.entity.User;
import linh.vn.cinegoticket.enums.BookingStatus;
import linh.vn.cinegoticket.enums.PaymentStatus;
import linh.vn.cinegoticket.exception.AppException;
import linh.vn.cinegoticket.exception.ErrorCode;
import linh.vn.cinegoticket.repository.BookingRepository;
import linh.vn.cinegoticket.repository.PaymentRepository;
import linh.vn.cinegoticket.repository.ShowSeatRepository;
import linh.vn.cinegoticket.repository.UserRepository;
import linh.vn.cinegoticket.service.EmailService;
import linh.vn.cinegoticket.service.PaymentService;
import linh.vn.cinegoticket.utils.HashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    final private int SEND_MAIL_SCHEDULE = 1000;
    Queue<PaymentResponse> sendEmail = new LinkedList<>();
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private VNPayService vnPayService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Override
    public PaymentResponse create(String username, PaymentRequest request, String ip_addr) {
        Booking booking = bookingRepository.findById(request.getBookingID()).orElseThrow(() -> new RuntimeException("Ticket ID " + request.getBookingID() + " is not found"));
        if (!booking.getStatus().equals(BookingStatus.PENDING))
            throw new RuntimeException("This ticket have been already paid or canceled before.");
        List<Payment> payments = paymentRepository.findAllByBookingId(request.getBookingID());
        if (payments.size() != 0)
            throw new RuntimeException("This ticket have been already pending for payment.");

        if (!username.equals(booking.getUser().getUsername()))
            throw new RuntimeException("Ticket ID " + request.getBookingID() + " is not found");

        //Tính tổng giá tiền dựa trên danh sách ghế đã đặt
        double price = booking.getPriceFromListSeats();
        //tạo payment mới
        Payment payment = new Payment(booking, price);
        payment = paymentRepository.save(payment);

        //Gửi request thanh toán đến cổng VNPay; Nếu VNPay.createPay() lỗi, trạng thái payment bị đổi thành CANCLED.
        String res = null;
        try {
            res = vnPayService.createPay(payment, request.getPaymentType(), ip_addr);
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.CANCLED);
            log.error("Error while creating VNPay payment for Booking ID {}: {}", booking.getId(), e.getMessage(), e);
            payment = paymentRepository.save(payment);
        }

        PaymentResponse response = new PaymentResponse(payment);
        response.setPaymentUrl(res != null ? res : "");

        return response;
    }

    @Override
    public PaymentResponse getFromId(String username, String payment_id) {//chi user authen ms đc xem payment
        Payment payment = paymentRepository.findById(payment_id).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));
        String userOfPayment = payment.getBooking().getUser().getUsername();
        if (userOfPayment.equals(username))
            return new PaymentResponse(payment);
        throw new RuntimeException("Payment ID not found");
    }

    @Override
    public List<PaymentResponse> getAllPaymentsOfUser(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));
        List<Payment> payments = paymentRepository.findAllByUserId(user.getId());
        List<PaymentResponse> resps = new ArrayList<PaymentResponse>();
        for (Payment p : payments)
            resps.add(new PaymentResponse(p));
        return resps;
    }

    @Override
    public boolean checkPaymentInfo(PaymentRequest request) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ApiResponse verifyPayment(String username, String payment_id) {
        Payment payment = paymentRepository.findById(payment_id).orElseThrow(() -> new RuntimeException("Payment ID not found"));
        String userOfPayment = payment.getBooking().getUser().getUsername();
        if (userOfPayment.equals(username)) {
            if (payment.getStatus() != PaymentStatus.PENDING)
                throw new RuntimeException("This ticket have been already paid or canceled before.");

            try {
                Integer paid = vnPayService.verifyPay(payment);
                //0: Giao dịch thành công, cp nhật trạng thái thanh toán + booking, ghế
                if (paid == 0) {
                    payment.setStatus(PaymentStatus.PAID);
                    paymentRepository.save(payment);

                    return new ApiResponse("Ticket is paid. You will receive this email", "PAID");
                }
                //2: Giao dịch không thành công (chưa thanh toán).
                if (paid == 2) {
                    payment.setStatus(PaymentStatus.CANCLED);
                    paymentRepository.save(payment);
                    return new ApiResponse("Ticket is unpaid", "UNPAID");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        throw new RuntimeException("Payment lỗi");
    }

    @Override
    public String createHash(HashRequest rawdata) {
        try {
            HashUtil hashUtil = new HashUtil();
            //tạo chuỗi
            String data = rawdata.getBookingID() + "&" + rawdata.getCardID()
                    + "&" + rawdata.getCardName() + "&" + rawdata.getCVCnumber();
            //rồi hash
            String hash = hashUtil.calculateHash(data);
            return hash;

        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    //thêm một đối tượng PaymentResponse vào hàng đợi sendEmail.
    @Override
    public void addPaymentMail(Payment payment) {
        PaymentResponse response = new PaymentResponse(payment);
        this.sendEmail.offer(response);
    }

    @Scheduled(fixedDelay = SEND_MAIL_SCHEDULE)
    private void sendPaymentViaMail() {
        while (this.sendEmail.size() != 0) {
            PaymentResponse data = this.sendEmail.poll();
            String info = "Payment ID " + data.getId() + "\n" +
                    "Total amount: " + data.getPrice() + "\n" +
                    "Create at: " + data.getCreateOn() + "\n" +
                    "Movie name: " + data.getDetai().getMovieName() + "\n" +
                    "Hall name: " + data.getDetai().getHallName() + "\n" +
                    "Start time: " + data.getDetai().getStartTime() + "\n" +
                    "Seats: " + String.join(", ", data.getDetai().getSeats());
            String subject = "Movie Project: Payment infomation";
            emailService.sendMail(data.getEmail(), subject, info);
        }
    }
}
