package linh.vn.cinegoticket.service;

import linh.vn.cinegoticket.dto.request.ShowRequest;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.ShowInfoResponse;
import linh.vn.cinegoticket.dto.response.ShowSeatResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CinemaShowService {
    //USER
    public ShowInfoResponse getShowInfo(String show_id);

    public List<ShowInfoResponse> getAllShowByMovieID(Long movieID);

    public List<ShowSeatResponse> getAllShowSeats(String showID);

    //ADMIN
    public List<ShowInfoResponse> getAllShowByHallID(String hallID);

    public List<ShowInfoResponse> getAllShows();

    public ApiResponse addShow(ShowRequest req);

    public List<ApiResponse> addListShows(List<ShowRequest> shows);

    public ApiResponse updateShow(String show_id, ShowRequest showReq);

    public ApiResponse deleteShow(String show_id);

    public ApiResponse deleteShowByHallIDMovieID(ShowRequest showReq);
}
