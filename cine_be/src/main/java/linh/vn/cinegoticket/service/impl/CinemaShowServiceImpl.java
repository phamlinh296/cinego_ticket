package linh.vn.cinegoticket.service.impl;

import linh.vn.cinegoticket.dto.request.ShowRequest;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.ShowInfoResponse;
import linh.vn.cinegoticket.dto.response.ShowSeatResponse;
import linh.vn.cinegoticket.entity.*;
import linh.vn.cinegoticket.enums.ESeatStatus;
import linh.vn.cinegoticket.exception.AppException;
import linh.vn.cinegoticket.exception.ErrorCode;
import linh.vn.cinegoticket.repository.*;
import linh.vn.cinegoticket.service.CinemaShowService;
import linh.vn.cinegoticket.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CinemaShowServiceImpl implements CinemaShowService {
    @Autowired
    private CinemaShowRepository cinemaShowRepository;
    @Autowired
    private ShowSeatRepository showSeatRepository;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private CinemaHallRepository cinemaHallRepository;
    @Autowired
    private CinemaSeatRepository cinemaSeatRepository;
    @Autowired
    private CinemaSeatServiceImpl cinemaSeatService;

    //USER
    @Override//Ttin show
    public ShowInfoResponse getShowInfo(String show_id) {
        CinemaShow show = cinemaShowRepository.findById(show_id).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));
        int availableSeats = showSeatRepository.countByShowIdAndStatus(show_id, ESeatStatus.AVAILABLE);
        int reservedSeats = showSeatRepository.countByShowIdAndStatus(show_id, ESeatStatus.BOOKED);

        return new ShowInfoResponse(show, availableSeats, reservedSeats);
    }

    @Override//ds show của 1 phim
    public List<ShowInfoResponse> getAllShowByMovieID(Long movieID) {
        List<CinemaShow> shows = cinemaShowRepository.findByMovieId(movieID);
        List<ShowInfoResponse> showInfoResponses = new ArrayList<>();
        for (CinemaShow show : shows) {
            ShowInfoResponse response = this.getShowInfo(show.getId());
            showInfoResponses.add(response);
        }
        return showInfoResponses;
    }

    @Override//ttin all chỗ ngồi 1 show
    public List<ShowSeatResponse> getAllShowSeats(String showID) {
        List<ShowSeat> showSeats = showSeatRepository.findByShowId(showID);
        if (showSeats.isEmpty()) {
            log.warn("Không tìm thấy ghế nào cho showID: {}", showID);
        }
        List<ShowSeatResponse> responses = new ArrayList<>();
        for (ShowSeat showSeat : showSeats) {
            responses.add(new ShowSeatResponse(showSeat));
        }
        return responses;
    }

    //ADMIN
    @Override //all show trong 1 hall
    public List<ShowInfoResponse> getAllShowByHallID(String hallID) {
        List<CinemaShow> shows = cinemaShowRepository.findByCinemaHallId(hallID);
        List<ShowInfoResponse> showInfoResponses = new ArrayList<>();
        for (CinemaShow show : shows) {
            ShowInfoResponse response = this.getShowInfo(show.getId());
            showInfoResponses.add(response);
        }
        return showInfoResponses;
    }

    @Override //all show trong hệ thống
    public List<ShowInfoResponse> getAllShows() {
        List<CinemaShow> shows = cinemaShowRepository.findAll();
        List<ShowInfoResponse> showInfoResponses = new ArrayList<>();
        for (CinemaShow show : shows) {
            ShowInfoResponse response = this.getShowInfo(show.getId());
            showInfoResponses.add(response);
        }
        return showInfoResponses;
    }

    @Override
    public ApiResponse addShow(ShowRequest req) {
        CinemaHall hall = cinemaHallRepository.findById(req.getCinemaID()).orElseThrow(() -> new RuntimeException("Hall is not found"));
        Movie movie = movieRepository.findById(req.getMovieID()).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));

        //Chuyển thời gian từ request từ String thành kiểu LocalDateTime.
        LocalDateTime starttime = DateUtils.convertStringDateToDate(req.getStartTime(), "dd/MM/yyyy HH:mm");
        if (starttime == null)
            throw new RuntimeException("Invaild date format, it must be dd/MM/yyyy HH:mm");
        LocalDateTime endtime = starttime.plusMinutes(movie.getDurationInMins()).plusMinutes(10);

        CinemaShow cinemaShow = new CinemaShow(hall, movie, starttime, endtime);//k cần thêm endtime cx đc vì đã có construct tính.
        cinemaShowRepository.save(cinemaShow);

        //Tạo danh sách ghế cho suất chiếu:
        this.addNewShowSeats(req.getCinemaID(), cinemaShow, false);
        return new ApiResponse("add show successfully");
    }

    @Override
    public List<ApiResponse> addListShows(List<ShowRequest> requests) {
        List<ApiResponse> apiResponses = new ArrayList<>();
        for (ShowRequest request : requests) {
            apiResponses.add(this.addShow(request));
        }
        return apiResponses;
    }

    @Override
    public ApiResponse updateShow(String show_id, ShowRequest showReq) {
        CinemaShow show = cinemaShowRepository.findById(show_id).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));

        //1. nếu hall đc update:
        if (show.getCinemaHall().getId() != showReq.getCinemaID()) {
            CinemaHall hall = cinemaHallRepository.findById(showReq.getCinemaID()).orElseThrow(() -> new RuntimeException("Hall is not found"));

            // Xóa các ghế cũ liên quan đến buổi chiếu hiện tại và tạo lại các ghế mới
            List<CinemaSeat> cinemaSeats = cinemaSeatRepository.findByCinemaHallId(hall.getId());
            if (cinemaSeats.isEmpty()) {
                cinemaSeatService.CreateListSeats(hall);
                cinemaSeats = cinemaSeatRepository.findByCinemaHallId(hall.getId());
            }

            showSeatRepository.deleteAllByShowId(show.getId());
            for (CinemaSeat cinemaSeat : cinemaSeats) {
                ShowSeat showSeat = new ShowSeat(show, cinemaSeat, ESeatStatus.AVAILABLE);
                showSeatRepository.save(showSeat);
            }
            show.setCinemaHall(hall);
        }

        //2. nếu movie dc update
        if (show.getMovie().getId() != showReq.getMovieID()) {
            Movie movie = movieRepository.findById(showReq.getMovieID()).orElseThrow(() -> new RuntimeException("Movie is not found"));
            show.setMovie(movie);
        }

        //3. nếu starttime dc update
        LocalDateTime starttime = DateUtils.convertStringDateToDate(showReq.getStartTime(), "dd/MM/yyyy HH:mm");
        if (starttime != null && !starttime.equals(show.getStartTime())) {
            LocalDateTime endtime = starttime.plusMinutes(show.getMovie().getDurationInMins()).plusMinutes(10);

            // Kiểm tra xung đột thời gian với các buổi chiếu khác
            List<CinemaShow> conflictedShows = cinemaShowRepository.findConflictingShows(starttime, endtime, show.getCinemaHall().getId(), show_id);
            if (!conflictedShows.isEmpty()) {
                throw new RuntimeException("Conflict start/end time with show ID: " + conflictedShows.get(0).getId());
            }

            show.setStartTime(starttime);
            show.setEndTime(endtime);
        }
        cinemaShowRepository.save(show);
        return new ApiResponse("Done");
    }

    @Override //xóa show dựa trên show id
    @Transactional
    public ApiResponse deleteShow(String show_id) {
        // Xóa tất cả ghế liên quan trước
        showSeatRepository.deleteAllByShowId(show_id);

        // Sau đó mới xóa suất chiếu
        CinemaShow cinemaShow = cinemaShowRepository.findById(show_id).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));
        cinemaShowRepository.deleteById(show_id);
        return new ApiResponse("Done");
    }

    @Override
    public ApiResponse deleteShowByHallIDMovieID(ShowRequest showReq) {
        LocalDateTime startTime = DateUtils.convertStringDateToDate(showReq.getStartTime(), "dd/MM/yyyy HH:mm");

        CinemaShow cinemaShow = cinemaShowRepository.findByHallIdAndMovieId(showReq.getCinemaID(), showReq.getMovieID(), startTime);
        if (cinemaShow == null) {
            throw new RuntimeException("Show not found for the given Hall ID, Movie ID, and Start Time");
        }
        showSeatRepository.deleteAllByShowId(cinemaShow.getId());
        cinemaShowRepository.deleteById(cinemaShow.getId());
        return new ApiResponse("Done");
    }

    //Tạo danh sách ghế liên kết với suất chiếu mới.
    private void addNewShowSeats(String cinema_id, CinemaShow show, boolean delete_old) {
        List<CinemaSeat> cinemaSeats = cinemaSeatRepository.findByCinemaHallId(cinema_id);
        if (delete_old) {
            showSeatRepository.deleteAllByShowId(show.getId());//Xóa ghế cũ liên kết với suất chiếu
        }

        //tạo showseat từ ds ghế với trạng thái mặc đnh= available.
        for (CinemaSeat cinemaSeat : cinemaSeats) {
            ShowSeat showSeat = new ShowSeat(show, cinemaSeat, ESeatStatus.AVAILABLE);
            showSeatRepository.save(showSeat);
        }
    }
}
