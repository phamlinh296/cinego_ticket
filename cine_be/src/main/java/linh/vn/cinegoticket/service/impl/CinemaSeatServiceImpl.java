package linh.vn.cinegoticket.service.impl;

import linh.vn.cinegoticket.dto.request.SeatEditRequest;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.SeatsResponse;
import linh.vn.cinegoticket.entity.CinemaHall;
import linh.vn.cinegoticket.entity.CinemaSeat;
import linh.vn.cinegoticket.enums.ESeat;
import linh.vn.cinegoticket.enums.ESeatStatus;
import linh.vn.cinegoticket.repository.CinemaHallRepository;
import linh.vn.cinegoticket.repository.CinemaSeatRepository;
import linh.vn.cinegoticket.service.CinemaSeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service

public class CinemaSeatServiceImpl implements CinemaSeatService {
    @Autowired
    private CinemaSeatRepository cinemaSeatRepository;
    @Autowired
    private CinemaHallRepository cinemaHallRepository;

    @Override
    public List<SeatsResponse> getAllSeatsFromHall(String hallID) {
        List<CinemaSeat> seats = cinemaSeatRepository.findAll();
        List<SeatsResponse> seatsResponses = new ArrayList<>();
        for (CinemaSeat seat : seats) {
            seatsResponses.add(new SeatsResponse(seat));
        }
        return seatsResponses;
    }

    @Override
    public void CreateListSeats(CinemaHall hall) {
        for (int r = 0; r < hall.getTotalRow(); r++) {
            for (int c = 0; c < hall.getTotalCol(); c++) {
                CinemaSeat seat = new CinemaSeat(hall, r, c, ESeat.REGULAR);
                cinemaSeatRepository.save(seat);
            }
        }
    }

    @Override
    public void RemoveAllSeatsFromHall(String hallID) {
        cinemaSeatRepository.deleteAllByCinemaHall_Id(hallID);
    }

    @Override
    public ApiResponse Edit(String hallID, SeatEditRequest seatReq) {
        CinemaSeat cinemaSeat = cinemaSeatRepository.findByCinemaHallIdAndRowIndexAndColIndex(hallID, seatReq.getRow(), seatReq.getCol())
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        //set ESeat:
        if (seatReq.getType() == null && cinemaSeat.getSeatType() == null) {
            cinemaSeat.setSeatType(ESeat.REGULAR);
        } else {
            try {
                cinemaSeat.setSeatType(ESeat.valueOf(seatReq.getType().toUpperCase()));//nếu k khớp vs enum thì ném lỗi
            } catch (IllegalArgumentException e) {
                return new ApiResponse("Invalid seat type. It must be REGULAR or PREMIUM");
            }
        }
        //set EseatStatus:
        if (seatReq.getStatus() == null && cinemaSeat.getStatus() == null) {
            return new ApiResponse("Status is not found. It must be AVAILABLE or UNAVAILABLE");
        } else {
            cinemaSeat.setStatus(ESeatStatus.valueOf(seatReq.getStatus().toUpperCase()));
        }

        cinemaSeatRepository.save(cinemaSeat);
        return new ApiResponse("Success");
    }

    @Override
    public boolean isExist(String hallID, int row, int column) {
        return cinemaSeatRepository.findByCinemaHallIdAndRowIndexAndColIndex(hallID, row, column).isPresent();
    }
}
