package linh.vn.cinegoticket.service;

import linh.vn.cinegoticket.dto.request.SeatEditRequest;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.SeatsResponse;
import linh.vn.cinegoticket.entity.CinemaHall;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CinemaSeatService {

    List<SeatsResponse> getAllSeatsFromHall(String hallID);

    public void CreateListSeats(CinemaHall hall);

    public void RemoveAllSeatsFromHall(String hallID);

    public ApiResponse Edit(String hallID, SeatEditRequest seatReq);

    public boolean isExist(String hallID, int row, int column);
}
