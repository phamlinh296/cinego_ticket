package linh.vn.cinegoticket.dto.response;

import linh.vn.cinegoticket.entity.Booking;
import lombok.Getter;

import java.util.List;

@Getter
public class BookingResponse {

    private String id;
    private String showId;
    private String fullname;
    private double price;
    private List<String> seats;
    private String movieName;
    private String hallName;
    private String startTime;
    private String createAt;
    private String status;

    public BookingResponse(Booking booking) {
        this.id = booking.getId();
        this.showId = booking.getShow().getId().toString();
        this.fullname = booking.getUser().getFullName();
        this.price = booking.getPriceFromListSeats();
        this.seats = booking.getNameOfSeats();
        this.movieName = booking.getShow().getMovie().getTitle();
        this.hallName = booking.getShow().getCinemaHall().getName();
        this.startTime = booking.getShow().getStartTime().toString();
        this.createAt = booking.getShow().getCreateAt().toString();
        this.status = booking.getStatus().name();
    }

//	private List<String> getNameOfSeats(List<ShowSeat> seats) {
//		List<String> names = new ArrayList<>();
//		for (ShowSeat seat : seats)
//			names.add(seat.getCinemaSeat().getName());
//		return names;
//	}

//	private double getPriceFromListSeats(List<ShowSeat> seats) {
//		double res = 0;
//		for (ShowSeat seat : seats)
//			res += seat.getCinemaSeat().getPrice();
//		return res;
//	}
} 



