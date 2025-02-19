package linh.vn.cinegoticket.dto.response;

import linh.vn.cinegoticket.entity.CinemaShow;
import lombok.Getter;

@Getter
public class ShowInfoResponse {
    private String id;
    private String hallName;
    private String hallId;
    private String movieName;
    private String movieId;
    private String startTime;
    private String endTime;
    private int total_seats;
    private int totalReservedSeats;
    private int totalAvailableSeats;

    public ShowInfoResponse(CinemaShow show, int total_reserved_seats, int total_available_seats) {
        this.id = show.getId();
        this.hallName = show.getCinemaHall().getName();
        this.hallId = show.getCinemaHall().getId();
        this.movieName = show.getMovie().getTitle();
        this.movieId = show.getMovie().getId().toString();
        this.startTime = show.getStartTime().toString();
        this.endTime = show.getEndTime().toString();
        this.total_seats = show.getCinemaHall().getCapacity();
        this.totalReservedSeats = total_reserved_seats;
        this.totalAvailableSeats = total_available_seats;
    }
}
