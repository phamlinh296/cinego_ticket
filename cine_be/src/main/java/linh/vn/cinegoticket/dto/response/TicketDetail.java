package linh.vn.cinegoticket.dto.response;

import linh.vn.cinegoticket.entity.Booking;

import java.util.List;

public class TicketDetail {

    private String movieName;
    private String hallName;
    private String startTime;
    private List<String> seats;

    public TicketDetail(Booking booking) {
        this.seats = booking.getNameOfSeats();
        this.movieName = booking.getShow().getMovie().getTitle();
        this.hallName = booking.getShow().getCinemaHall().getName();
        this.startTime = booking.getShow().getStartTime().toString();
    }

    public String getMovieName() {
        return this.movieName;
    }

    public List<String> getSeats() {
        return this.seats;
    }

    public String getHallName() {
        return this.hallName;
    }

    public String getStartTime() {
        return this.startTime;
    }
}