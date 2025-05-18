package linh.vn.cinegoticket.service.impl;

import linh.vn.cinegoticket.dto.request.BookingRequest;
import linh.vn.cinegoticket.dto.request.PaymentRequest;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.BookingResponse;
import linh.vn.cinegoticket.entity.*;
import linh.vn.cinegoticket.enums.BookingStatus;
import linh.vn.cinegoticket.enums.ESeatStatus;
import linh.vn.cinegoticket.enums.PaymentStatus;
import linh.vn.cinegoticket.enums.UserStatus;
import linh.vn.cinegoticket.repository.*;
import linh.vn.cinegoticket.service.BookingService;
import linh.vn.cinegoticket.service.PaymentService;
import linh.vn.cinegoticket.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
public class BookingServiceImpl implements BookingService {

    final private int MAXSPAM = 3; // per user
    final private int MAX_TICKETS_PER_SHOW = 5; // for per user
    final private int TIMEOUT = 15; // in minutes
    final private long CHECK_PENDING_BOOKING_IS_TIMEOUT = 60000; // in miliseconds
    final private long CHECK_QUEUE_OF_SPAM_USERS = 30000; // in miliseconds

    Queue<User> spamUsers = new LinkedList<>();

    @Autowired
    private UserRepository userREPO;

    @Autowired
    private ShowSeatRepository showSeatREPO;

    @Autowired
    private CinemaShowRepository showREPO;

    @Autowired
    private UserService userSER;

    @Autowired
    private BookingRepository bookingREPO;

    @Autowired
    private PaymentRepository paymentREPO;

    @Autowired
    private SpamUserRepository spamREPO;

    @Autowired
    private PaymentService paymentSER;
    @Autowired
    private VNPayService vnPayService;


    @Override
    public BookingResponse getBookingFromID(String username, String booking_id) {
        User user = userREPO.findByUsername(username).orElseThrow(() -> new RuntimeException("User is not found"));
        Booking booking = bookingREPO.findByIdAndUserId(booking_id, user.getId()).orElseThrow(() -> new RuntimeException("Ticket is not found"));
        return new BookingResponse(booking);
    }

    @Override
    public List<BookingResponse> listOfBooking(String username) {
        User user = userREPO.findByUsername(username).orElseThrow(() -> new RuntimeException("User is not found"));
        List<Booking> listBooking = bookingREPO.findAllByUserId(user.getId());

        List<BookingResponse> info = new ArrayList<>();
        for (Booking booking : listBooking) {
            info.add(new BookingResponse(booking));
        }
        return info;
    }

    @Override
    public BookingResponse createBooking(String username, BookingRequest bookingReq) {
        if (bookingReq.getSeatsId().size() > 4)
            throw new RuntimeException("You can not reverse more than 4 seats at the time.");

        //user authen và k blacklist
        User user = userREPO.findByUsername(username).orElseThrow(() -> new RuntimeException("User is not found"));
        if (user.getStatus().equals(UserStatus.BLACKLISTED.name()))
            throw new RuntimeException("You are not allowed to book ticket");

        //show, slg vé đặt trong show đặt max; còn seat trống trong show k
        CinemaShow show = showREPO.findById(bookingReq.getShowId()).orElseThrow(() -> new RuntimeException("Show is not found"));
        int total_tickets_of_user_from_show = bookingREPO.countByShowId(show.getId());
        if (total_tickets_of_user_from_show == this.MAX_TICKETS_PER_SHOW)
            throw new RuntimeException("You have already " + this.MAX_TICKETS_PER_SHOW + " tickets in this show, so you can pay no more tickets");
        if (this.seatsAreFull(show))
            throw new RuntimeException("Sorry, seats of this show are full. Please choose another show");

        //seat
        List<ShowSeat> seats = new ArrayList<>();
        for (String seat_id : this.removeDuplicate(bookingReq.getSeatsId())) {// loại bỏ các ghế bị trùng lặp trong danh sách seatsId.
            //ngay bước chọn ghế, khóa ghế
            ShowSeat seat = this.getSeatFromStatusWithLock(seat_id, show, ESeatStatus.AVAILABLE);//trạng thái seat phỉa là available
            if (seat == null)
                throw new RuntimeException("Seat ID " + seat_id + " is reserved");

            seat.setStatus(ESeatStatus.PENDING);//chuyển tthai seat thành pending, lưu vào db
            ShowSeat seatSaved = showSeatREPO.save(seat);
            seats.add(seatSaved);//thêm vào ds seat
        }

        Booking booking = new Booking(user, show, seats);//tạo booking, lưu
        Booking bookingSaved = bookingREPO.save(booking);
        return new BookingResponse(bookingSaved);
    }

    @Override
    public ApiResponse cancleBooking(String username, String booking_id) {
        User user = userREPO.findByUsername(username).orElseThrow(() -> new RuntimeException("User is not found"));
        Booking booking = bookingREPO.findById(booking_id).orElseThrow(() -> new RuntimeException("Booking ticket is not found"));

        if (!user.getId().equals(booking.getUser().getId()))
            throw new RuntimeException("This ticket does not belong to user " + user.getUsername());

        //cancled, đã booked k đc hủy; chỉ có pending ms đc hủy
        if (booking.getStatus().equals(BookingStatus.CANCLED) || booking.getStatus().equals(BookingStatus.BOOKED))
            throw new RuntimeException("This ticket can not be cancled");

        this.cancleBookingFromID(booking);//cancel booking, seat thành available
        return new ApiResponse("Done");
    }

    @Override
    //thay đổi trajng thái vé- booking thủ công của admin(vd:  hủy vé sau khi đặt., cập nhật trạng thái vé sau đặt xem thành công hay tbai)
    public ApiResponse setBookingStatus(String username, String booking_id, String status) {
        User user = userREPO.findByUsername(username).orElseThrow(() -> new RuntimeException("User is not found"));
        Booking booking = bookingREPO.findByIdAndUserId(booking_id, user.getId()).orElseThrow(() -> new RuntimeException("Ticket is not found"));

        status = status.toUpperCase();
        if (booking.getStatus().name().equals(status))
            throw new RuntimeException("This ticket already have this status");

        switch (status) {
            case "PENDING":
                for (ShowSeat seat : booking.getSeats()) {
                    seat.setStatus(ESeatStatus.PENDING);
                    showSeatREPO.save(seat);
                }
                //ủa xóa cũ tạo mới gây thay đổi id của booking, hoy kệ đỡ phải update nhiều
                bookingREPO.deleteById(booking_id);
                Booking newBooking = new Booking(booking);
                bookingREPO.save(newBooking);
                break;

            case "CANCLED":
                this.cancleBooking(username, booking_id);
                break;

            case "BOOKED":
                List<Payment> payments = paymentREPO.findAllByBookingId(booking_id);
                if (payments.isEmpty() || payments.get(0).getStatus() != PaymentStatus.PAID) {
                    System.out.println("Payment has not been completed yet. I am going to create a new payment");
                    PaymentRequest req = new PaymentRequest(booking_id, "");
                    paymentSER.create(username, req, "127.0.0.1");
                }
                break;

            default:
                throw new RuntimeException("Not found status " + status);
        }
        return new ApiResponse("Success: bookingid = " + booking_id + " is turned to " + status);
    }

    // Nếu vé (booking pending) quá thời gian chờ (timeout), hệ thống sẽ kiểm tra trạng thái thanh toán và cập nhật vé.
    //1. chưa tạo payment > hủy luôn
    //2. đã tạo payment, đã verify, và bjo cần chuyển tthai của pending sang booked/cancle tùy verify thành công k
    @Scheduled(fixedDelay = CHECK_PENDING_BOOKING_IS_TIMEOUT)//được gọi định kỳ 6s.
    public void autoCancleBooking() {
        List<Booking> bookingList = bookingREPO.findAllByStatus(BookingStatus.PENDING);

        //tính thời gian cho
        LocalDateTime now = LocalDateTime.now();
        for (Booking booking : bookingList) {
            Date createDate = booking.getCreateAt();
            LocalDateTime toLocalDateTime = createDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            Duration duration = Duration.between(toLocalDateTime, now);//thời gian từ lúc vé được tạo cho đến thời điểm hiện tại.
            long minutes = duration.toMinutes() % 60;//lấy số phút dư

            //nếu Booking quá thời gian chờ
            if (minutes >= this.TIMEOUT) {
                List<Payment> payments = paymentREPO.findAllByBookingId(booking.getId());
                // 1.nếu booking pending, không có Payment thì hủy Booking
                if (payments.size() == 0) {
                    this.cancleBookingFromID(booking);
//                    this.spamUsers.offer(booking.getUser());//vch cho user spam
                    System.out.println("--> Timeout: Delete status of booking " + booking.getId());
                }
                //2. nếu có payment (chưa thanh toán xong, nên booking vẫn là pending, chưa chuển sang booked)
                //đã gọi đn verify, chuyển thành paid thì sau 6s ms chuyển thành booked
                else {
                    Payment payment = payments.get(0);
                    if (payment.getStatus() == PaymentStatus.PAID) {
                        // Nếu thanh toán thành công, cập nhật trạng thái Booking và ShowSeat
                        booking.setStatus(BookingStatus.BOOKED);
                        bookingREPO.save(booking);

                        for (ShowSeat seat : booking.getSeats()) {
                            seat.setStatus(ESeatStatus.BOOKED);
                            showSeatREPO.save(seat);
                        }

                        paymentSER.addPaymentMail(payment); // Gửi thông báo thanh toán qua email
                        System.out.println("--> Send ticket of booking " + booking.getId());
                    } else {
                        // Nếu payment=cancle/pending; thanh toán thất bại/chuwua verify,> hủy Booking và trả ghế về AVAILABLE
                        this.cancleBookingFromID(booking);
                        System.out.println("--> Delete status of booking " + booking.getId());
                        if (payment.getStatus() == PaymentStatus.CANCLED) {
                            this.spamUsers.offer(booking.getUser());//nếu payment bị cancle thì ms tính là spam, còn pending thì chỉ hủy thôi
                        }
                    }
                }
            }
        }
    }

    //kiểm tra danh sách các người dùng "spam" và thêm vào (blacklist)
    @Scheduled(fixedDelay = CHECK_QUEUE_OF_SPAM_USERS)
    public void blacklistUsers() {
        if (this.spamUsers.size() == 0)
            return;

        while (this.spamUsers.size() != 0) {
            User user = this.spamUsers.poll();
            Optional<SpamUser> getSpam = spamREPO.findByUserId(user.getId());

            if (getSpam.isPresent()) {
                SpamUser spam = getSpam.get();
                int times = spam.increase();

                if (times >= this.MAXSPAM) {
                    user.setStatus(UserStatus.BLACKLISTED);
                    userREPO.save(user);
                }
                spamREPO.save(spam);
            } else {
                SpamUser spam = new SpamUser(user);
                spamREPO.save(spam);
            }
        }
    }

    private boolean seatsAreFull(CinemaShow show) {
        int bookedSeat = showSeatREPO.countByShowIdAndStatus(show.getId(), ESeatStatus.AVAILABLE);
        return bookedSeat == 0;
    }

    //khóa ghế ngay khi chọn
    private ShowSeat getSeatFromStatusWithLock(String seat_id, CinemaShow show, ESeatStatus status) {
        ShowSeat seat = showSeatREPO.findByIdAndShowId(seat_id, show.getId()).orElseThrow(() -> new RuntimeException("Not found seat id: " + seat_id));
        if (seat.getStatus().name().equals(status.name()))
            return seat;
        return null;
    }

    private void setStatusForBookingAndSeats(Booking booking, BookingStatus bookingStatus, ESeatStatus seatStatus) {
        booking.setStatus(bookingStatus);
        for (ShowSeat seat : booking.getSeats()) {
            seat.setStatus(seatStatus);
            showSeatREPO.save(seat);
        }

        bookingREPO.save(booking);
    }

    private void cancleBookingFromID(Booking booking) {
//		booking.setStatus(BookingStatus.CANCLED);
//
//		for (ShowSeat seat : booking.getSeats()) {
//			seat.setStatus(ESeatStatus.AVAILABLE);
//			showSeatREPO.save(seat);
//		}
//
//		bookingREPO.save(booking);
        this.setStatusForBookingAndSeats(booking, BookingStatus.CANCLED, ESeatStatus.AVAILABLE);
    }

    private String[] removeDuplicate(List<String> array) {
        Set<String> set = new HashSet<>(array);
        return set.toArray(new String[0]);
    }
}



