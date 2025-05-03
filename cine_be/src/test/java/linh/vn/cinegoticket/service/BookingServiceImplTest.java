package linh.vn.cinegoticket.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import linh.vn.cinegoticket.dto.request.BookingRequest;
import linh.vn.cinegoticket.dto.response.BookingResponse;
import linh.vn.cinegoticket.entity.*;
import linh.vn.cinegoticket.enums.ESeatStatus;
import linh.vn.cinegoticket.enums.UserStatus;
import linh.vn.cinegoticket.repository.*;
import linh.vn.cinegoticket.service.impl.BookingServiceImpl;

@ExtendWith(MockitoExtension.class)
public class BookingServiceImplTest {

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Mock
    private UserRepository userRepo;
    @Mock
    private ShowSeatRepository showSeatRepo;
    @Mock
    private CinemaShowRepository showRepo;
    @Mock
    private BookingRepository bookingRepo;

    private User user;
    private CinemaShow show;
    private ShowSeat seat1;
    private ShowSeat seat2;

//    @BeforeEach //k dùng beforeach nữa, vì test giữa k dùng chung data trong beforeeach
    public void commonSetup() {//nên mh se gọi thủ công cái này trong những test dung n thôi
        user = new User();
        user.setId("user1");
        user.setFullName("John Doe");
        user.setStatus(UserStatus.ACTIVE);

        Movie movie = new Movie();
        movie.setTitle("Avengers");

        CinemaHall hall = new CinemaHall();
        hall.setName("Hall 1");

        show = new CinemaShow();
        show.setId("show123");
        show.setMovie(movie);
        show.setCinemaHall(hall);
        show.setStartTime(LocalDateTime.of(2025, 5, 2, 14, 30));
        show.setCreateAt(new Date());

        CinemaSeat cinemaSeat1 = new CinemaSeat();
        cinemaSeat1.setId(1L);
        cinemaSeat1.setName("A1");

        CinemaSeat cinemaSeat2 = new CinemaSeat();
        cinemaSeat2.setId(2L);
        cinemaSeat2.setName("A2");

        seat1 = new ShowSeat(show, cinemaSeat1, ESeatStatus.AVAILABLE);
        seat1.setId("A1");

        seat2 = new ShowSeat(show, cinemaSeat2, ESeatStatus.AVAILABLE);
        seat2.setId("A2");

        when(userRepo.findByUsername("john")).thenReturn(Optional.of(user));
        when(showRepo.findById("show123")).thenReturn(Optional.of(show));
    }

    @Test
    void createBooking_validRequest_success() {
        commonSetup();
        BookingRequest bookingReq = new BookingRequest();
        bookingReq.setShowId("show123");
        bookingReq.setSeatsId(List.of("A1", "A2"));

        when(bookingRepo.countByShowId("show123")).thenReturn(0);
        when(showSeatRepo.countByShowIdAndStatus("show123", ESeatStatus.AVAILABLE)).thenReturn(10);
        when(showSeatRepo.findByIdAndShowId("A1", "show123")).thenReturn(Optional.of(seat1));
        when(showSeatRepo.findByIdAndShowId("A2", "show123")).thenReturn(Optional.of(seat2));
        when(showSeatRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepo.save(any())).thenAnswer(inv -> {
            Booking booking = inv.getArgument(0);
            booking.setId("bk123");
            return booking;
        });

        BookingResponse result = bookingService.createBooking("john", bookingReq);

        assertNotNull(result);
        assertEquals("show123", result.getShowId());
        assertEquals("John Doe", result.getFullname());
        assertTrue(result.getSeats().contains("A1"));
        assertTrue(result.getSeats().contains("A2"));
    }

    @Test
    void createBooking_seatAlreadyReserved_throwsException() {
        // Arrange
        User user = new User();
        user.setId("user1");
        user.setFullName("John Doe");
        user.setStatus(UserStatus.ACTIVE);

        CinemaShow show = new CinemaShow();
        show.setId("show123");
        show.setStartTime(LocalDateTime.now());
        show.setCreateAt(new Date());

        CinemaSeat seat = new CinemaSeat();
        seat.setId(1L);
        seat.setName("A1");

        ShowSeat showSeat = new ShowSeat(show, seat, ESeatStatus.BOOKED);
        showSeat.setId("A1");

        BookingRequest req = new BookingRequest();
        req.setShowId("show123");
        req.setSeatsId(List.of("A1"));

        when(userRepo.findByUsername("john")).thenReturn(Optional.of(user));
        when(showRepo.findById("show123")).thenReturn(Optional.of(show));
        when(showSeatRepo.countByShowIdAndStatus("show123", ESeatStatus.AVAILABLE)).thenReturn(10);
        when(showSeatRepo.findByIdAndShowId("A1", "show123")).thenReturn(Optional.of(showSeat));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                bookingService.createBooking("john", req)
        );

        assertEquals("Seat ID A1 is reserved", ex.getMessage());
    }


    @Test
    void createBooking_seatsFull_throwsException() {
        commonSetup();
        BookingRequest bookingReq = new BookingRequest();
        bookingReq.setShowId("show123");
        bookingReq.setSeatsId(List.of("A1", "A2"));

        when(bookingRepo.countByShowId("show123")).thenReturn(0);
        when(showSeatRepo.countByShowIdAndStatus("show123", ESeatStatus.AVAILABLE)).thenReturn(0);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                bookingService.createBooking("john", bookingReq)
        );
        assertEquals("Sorry, seats of this show are full. Please choose another show", ex.getMessage());
    }

} 