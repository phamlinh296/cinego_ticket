package linh.vn.cinegoticket.service.impl;

import linh.vn.cinegoticket.dto.request.CinemaHallRequest;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.entity.CinemaHall;
import linh.vn.cinegoticket.exception.AppException;
import linh.vn.cinegoticket.exception.ErrorCode;
import linh.vn.cinegoticket.repository.CinemaHallRepository;
import linh.vn.cinegoticket.service.CinemaHallService;
import linh.vn.cinegoticket.service.CinemaSeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CinemaHallServiceImpl implements CinemaHallService {
    @Autowired
    private CinemaHallRepository cinemaHallRepository;
    @Autowired
    private InputValidationService inputValidationService;
    @Autowired
    private CinemaSeatService cinemaSeatService;

    @Override
    @Cacheable(value = "allHalls", cacheManager = "listCacheManager")
    public List<CinemaHall> getAllHalls() {
        System.out.println("Fetching halls from DB...");
        return cinemaHallRepository.findAll();
    }

    @Override
    @Cacheable(value = "cinemaHall", key = "#hallID", cacheManager = "objectCacheManager")
    public CinemaHall getHallById(String id) {
        System.out.println("Fetching a hall from DB...");
        return cinemaHallRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));
    }

    @Override
    public ApiResponse newHall(CinemaHall c) {
        if (!inputValidationService.checkInput(c.getName()))
            return new ApiResponse("Illeagal charaters in name");
        if (cinemaHallRepository.existsByName(c.getName()))
            return new ApiResponse("This hall is existed");
        if (c.getTotalCol() < 5 || c.getTotalRow() < 5)
            return new ApiResponse("Row/Column number must be greater than 5");

        //sau check lưu hall
        cinemaHallRepository.save(c);
        CinemaHall hall = cinemaHallRepository.findByName(c.getName()).get();
        cinemaSeatService.CreateListSeats(hall);

        return new ApiResponse("Success");
    }

    @Override
    //    @CachePut(value = "cinemaHall", key = "#hallID", cacheManager = "objectCacheManager") //return apiresponse, kp đtg cinemahall
    public ApiResponse editHall(String hallID, CinemaHallRequest req) {
        if (!inputValidationService.checkInput(req.getName()))
            return new ApiResponse("Illeagal charaters in name", HttpStatus.BAD_REQUEST);
        CinemaHall cinemaHall = cinemaHallRepository.findById(hallID).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));

        if (cinemaHall.getTotalCol() != req.getTotalCol() || cinemaHall.getTotalRow() != req.getTotalRow()) {
            cinemaSeatService.RemoveAllSeatsFromHall(cinemaHall.getId());
            cinemaSeatService.CreateListSeats(cinemaHall);
        }
        cinemaHall.setName(req.getName());
        cinemaHall.setTotalCol(req.getTotalCol());
        cinemaHall.setTotalRow(req.getTotalRow());
        cinemaHallRepository.save(cinemaHall);
        return new ApiResponse("Success");
    }

    @Override
    @CacheEvict(value = "cinemaHall", key = "#hallID", cacheManager = "objectCacheManager", beforeInvocation = true)
    public ApiResponse removeHall(String hallID) {
        CinemaHall hall = cinemaHallRepository.findById(hallID).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));
        cinemaSeatService.RemoveAllSeatsFromHall(hall.getId());
        cinemaHallRepository.delete(hall);
        return null;
    }

    @Override
    public boolean isExistByName(String hallName) {
        return cinemaHallRepository.existsByName(hallName);
    }

    @Override
    public boolean isExistById(String id) {
        return cinemaHallRepository.existsById(id);
    }
}
