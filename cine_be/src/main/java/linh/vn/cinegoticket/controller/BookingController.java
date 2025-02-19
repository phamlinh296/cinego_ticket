package linh.vn.cinegoticket.controller;

import jakarta.validation.Valid;
import linh.vn.cinegoticket.dto.request.BookingRequest;
import linh.vn.cinegoticket.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/booking")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    //1. USER HIỆN TẠI
    //Lấy thông tin vé đã đặt dựa trên booking_id của người dùng hiện tại.
    @GetMapping("/{booking_id}")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getBookingByID(Principal principal,
                                            @Valid @PathVariable(value = "booking_id") String booking_id) {
        return ResponseEntity.ok().body(bookingService.getBookingFromID(principal.getName(), booking_id));
    }

    //Lấy danh sách tất cả các vé đã đặt của user htai
    @GetMapping("/getall")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getAllBookingsFromUser(Principal principal) {
        return ResponseEntity.ok().body(bookingService.listOfBooking(principal.getName()));
    }

    //Tạo đặt chỗ mới cho người dùng hiện tại.
    @PostMapping("/add")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createBooking(Principal principal, @Valid @RequestBody BookingRequest bookingReq) {
        return ResponseEntity.ok().body(bookingService.createBooking(principal.getName(), bookingReq));
    }

    //Hủy vé đã đặt dựa trên booking_id của người dùng hiện tại.
    @DeleteMapping("/{booking_id}/cancel")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteBookingByID(Principal principal,
                                               @Valid @PathVariable(value = "booking_id") String booking_id) {
        return ResponseEntity.ok().body(bookingService.cancleBooking(principal.getName(), booking_id));
    }

    //2. ADMIN
    //Lấy danh sách tất cả các vé đã đặt của một người dùng cụ thể (dành cho admin).
    @GetMapping("/user/{username}/getall")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllBookingsFromUser(@Valid @PathVariable(value = "username") String username) {
        return ResponseEntity.ok().body(bookingService.listOfBooking(username));
    }

    //Lấy thông tin vé đã đặt dựa trên booking_id của một người dùng cụ thể (dành cho admin).
    @GetMapping("/user/{username}/{booking_id}")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getBookingsByIDFromUser(@Valid @PathVariable(value = "username") String username,
                                                     @Valid @PathVariable(value = "booking_id") String booking_id) {
        return ResponseEntity.ok().body(bookingService.getBookingFromID(username, booking_id));
    }

    //Cập nhật trạng thái của vé đã đặt dựa trên booking_id và username (dành cho admin).
    @PutMapping("/user/{username}/{booking_id}/setstatus")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> setBookingStatusFromUsername(@Valid @PathVariable(value = "username") String username,
                                                          @Valid @PathVariable(value = "booking_id") String booking_id,
                                                          @RequestParam("value") @Valid String status) {
        return ResponseEntity.ok().body(bookingService.setBookingStatus(username, booking_id, status));
    }
}
