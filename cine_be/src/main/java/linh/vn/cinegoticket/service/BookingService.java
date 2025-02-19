package linh.vn.cinegoticket.service;

import linh.vn.cinegoticket.dto.request.BookingRequest;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.BookingResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface BookingService {
    public BookingResponse createBooking(String username, BookingRequest bookingReq);

    public ApiResponse cancleBooking(String username, String booking_id);

    public BookingResponse getBookingFromID(String username, String booking_id);

    public List<BookingResponse> listOfBooking(String username);

    public ApiResponse setBookingStatus(String username, String booking_id, String status);
}
