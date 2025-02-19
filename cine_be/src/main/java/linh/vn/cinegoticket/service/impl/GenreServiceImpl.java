package linh.vn.cinegoticket.service.impl;

import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.entity.Genre;
import linh.vn.cinegoticket.exception.AppException;
import linh.vn.cinegoticket.exception.ErrorCode;
import linh.vn.cinegoticket.repository.GenreReposity;
import linh.vn.cinegoticket.service.GenreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GenreServiceImpl implements GenreService {

    @Autowired
    private GenreReposity genreReposity;

    @Override
    public Genre getGenre(Long id) {
        return genreReposity.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));
    }

    @Override
    public List<Genre> getGenres() {
        return genreReposity.findAll();
    }

    @Override
    public Genre createGenre(Genre genre) {
        if (genreReposity.existsByGenre(genre.getGenre())) {
            throw new AppException(ErrorCode.EXISTED);
        }
        return genreReposity.save(genre);
    }

    @Override
    public Genre updateGenre(Genre genre) {
        System.out.println("Received ID: " + genre.getId()); // Debug xem ID có đúng không
        System.out.println("Received Genre: " + genre.getGenre());

        Genre existingGenre = genreReposity.findById(genre.getId()).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));
        if (genreReposity.existsByGenre(genre.getGenre())) {
            throw new RuntimeException("This name already exists, please choose another one.");
        }
        existingGenre.setGenre(genre.getGenre());
        return genreReposity.save(existingGenre);
    }

    @Override
    public ApiResponse deleteGenre(Long id) {
        if (!genreReposity.existsById(id))
            throw new AppException(ErrorCode.NOT_EXISTED);
        genreReposity.deleteById(id);
        return new ApiResponse("Successfully deleted the genre ID:" + id);
    }
}
