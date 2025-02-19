package linh.vn.cinegoticket.service;

import linh.vn.cinegoticket.dto.request.CinemaHallRequest;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.entity.CinemaHall;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CinemaHallService {
    List<CinemaHall> getAllHalls();

    CinemaHall getHallById(String id);

    ApiResponse newHall(CinemaHall c);

    ApiResponse editHall(String hallID, CinemaHallRequest c);

    ApiResponse removeHall(String HallID);

    boolean isExistByName(String hallName);

    boolean isExistById(String ID);
}
